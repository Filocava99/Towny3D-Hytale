package it.cava99.towny3d.system

import com.hypixel.hytale.server.core.io.adapter.PacketAdapters
import com.hypixel.hytale.server.core.io.PacketHandler
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.TargetUtil
import com.hypixel.hytale.component.Store
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.util.TimeConstants
import it.cava99.towny3d.util.TownyLog
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles claim stick input via packet listeners.
 *
 * Hytale input mapping (as per official documentation):
 * - InteractionType.Primary (left click) -> Position 1
 * - InteractionType.Use (F key) -> Position 2
 *
 * All inputs are handled via SyncInteractionChains packet (ID 290) listener.
 */
class ClaimStickSystem {

    companion object {
        // Track last packet processing time per player to deduplicate multiple packets from same input
        private val lastPacketTime = ConcurrentHashMap<UUID, Long>()

        /** Maximum block reach distance for raycast */
        const val MAX_BLOCK_REACH = 5.0

        /**
         * Registers the packet listener for claim stick functionality.
         * This should be called from Towny3D.setup().
         */
        fun register(plugin: Towny3D) {
            // Register packet listener for all claim stick inputs
            registerPacketListener(plugin)

            TownyLog.info("Registered ClaimStickSystem handlers")
        }

        /**
         * Registers the packet listener for claim stick functionality.
         *
         * This listens to SyncInteractionChains (packet ID 290) for:
         * - InteractionType.Primary (left click) -> Position 1
         * - InteractionType.Use (F key) -> Position 2
         */
        private fun registerPacketListener(plugin: Towny3D) {
            PacketAdapters.registerInbound(object : com.hypixel.hytale.server.core.io.adapter.PacketWatcher {
                override fun accept(packetHandler: PacketHandler, packet: com.hypixel.hytale.protocol.Packet) {
                    // Filter for SyncInteractionChains packet (ID 290)
                    if (packet.getId() != 290) {
                        return
                    }

                    try {
                        val interactionChains = packet as SyncInteractionChains
                        val updates = interactionChains.updates

                        // Process only the first matching interaction to avoid duplicates
                        for (interactionChain in updates) {
                            when (interactionChain.interactionType) {
                                InteractionType.Primary -> {
                                    // Left click - handle position 1
                                    handleInteractionKeyPress(packetHandler, plugin, InteractionType.Primary)
                                    break  // Only process first match
                                }
                                InteractionType.Use -> {
                                    // F key - handle position 2
                                    handleInteractionKeyPress(packetHandler, plugin, InteractionType.Use)
                                    break  // Only process first match
                                }
                                else -> {
                                    // Ignore other interaction types, continue checking
                                }
                            }
                        }
                    } catch (e: Exception) {
                        TownyLog.error("Failed to process SyncInteractionChains packet", e)
                    }
                }
            })
            TownyLog.info("Registered ClaimStickSystem SyncInteractionChains packet listener")
        }

        /**
         * Handles interaction key press for claim stick selection.
         *
         * This method is called when an interaction key is pressed:
         * - InteractionType.Primary (left click) -> Position 1
         * - InteractionType.Use (F key) -> Position 2
         *
         * It checks if the player is holding a claim stick and looking at a block,
         * then sets the appropriate position to that block's location.
         */
        private fun handleInteractionKeyPress(packetHandler: PacketHandler, plugin: Towny3D, interactionType: InteractionType) {
            // Get player authentication info
            val playerAuthentication = packetHandler.getAuth() ?: run {
                TownyLog.warn("getAuth() returned null for packet handler")
                return
            }

            val playerUuid = playerAuthentication.getUuid()

            // Packet-level deduplication: check if we recently processed a packet for this player
            val currentTime = System.currentTimeMillis()
            val lastTime = lastPacketTime.getOrDefault(playerUuid, 0L)
            if (currentTime - lastTime < TimeConstants.PACKET_DEDUPE_MS) {
                // Skip this packet as it's likely a duplicate from the same input
                return
            }
            lastPacketTime[playerUuid] = currentTime

            // Get the universe and world
            val universe = com.hypixel.hytale.server.core.universe.Universe.get()
            val world = universe.getDefaultWorld() ?: run {
                TownyLog.warn("Default world not found")
                return
            }

            // Execute on world thread to access player components
            world.execute {
                try {
                    val entityStore = world.getEntityStore()
                    val store: Store<EntityStore> = entityStore.getStore()

                    // Get player entity reference by UUID
                    // Note: Player entity might be null if player hasn't loaded yet - this is expected
                    val ref: com.hypixel.hytale.component.Ref<EntityStore> = entityStore.getRefFromUUID(playerUuid) ?: run {
                        // Silently return - player not loaded yet is expected behavior
                        return@execute
                    }

                    // Get Player component to check inventory
                    val player: Player = store.getComponent(ref, Player.getComponentType()) ?: run {
                        // Player component not available yet
                        return@execute
                    }

                    // Get PlayerRef component to get player name
                    val playerRefComponent: PlayerRef = store.getComponent(ref, PlayerRef.getComponentType()) ?: run {
                        // PlayerRef component not available yet
                        return@execute
                    }

                    val playerName = playerRefComponent.username

                    // Check if player is holding claim stick
                    val heldItem = try {
                        player.getInventory().getItemInHand()
                    } catch (e: Exception) {
                        null
                    }

                    val heldItemId = heldItem?.itemId

                    if (heldItemId != "Ingredient_Stick") {
                        // Player is not holding claim stick, ignore
                        return@execute
                    }

                    // Raycast to find the block the player is looking at
                    val lookTransform = TargetUtil.getLook(ref, store)
                    val eyePos = lookTransform.position
                    val direction = lookTransform.direction

                    // Raycast for block target (max 5 blocks reach)
                    val hitBlock = TargetUtil.getTargetBlock(
                        ref,
                        { blockId: Int -> blockId != 0 }, // Hit any non-air block
                        MAX_BLOCK_REACH,
                        store
                    )

                    if (hitBlock == null) {
                        // Not looking at a block
                        return@execute
                    }

                    // Create BlockLocation from hit block position
                    val location = BlockLocation(
                        plugin.getConfig().defaultWorldName,
                        hitBlock.x,
                        hitBlock.y,
                        hitBlock.z
                    )

                    // Execute synchronously inside world.execute - no coroutines needed
                    try {
                        TownyLog.info("Processing $interactionType for $playerName at (${hitBlock.x}, ${hitBlock.y}, ${hitBlock.z})")

                        when (interactionType) {
                            InteractionType.Primary -> {
                                // Left click - set position 1 (synchronous call)
                                plugin.claimStickManager.onLeftClickSync(
                                    playerUuid,
                                    playerName,
                                    location,
                                    playerRefComponent
                                )
                                TownyLog.info("$playerName used Primary click at (${hitBlock.x}, ${hitBlock.y}, ${hitBlock.z})")
                            }
                            InteractionType.Use -> {
                                // F key - set position 2 (synchronous call)
                                plugin.claimStickManager.onRightClickSync(
                                    playerUuid,
                                    playerName,
                                    location,
                                    playerRefComponent
                                )
                                TownyLog.info("$playerName used F key at (${hitBlock.x}, ${hitBlock.y}, ${hitBlock.z})")
                            }
                            else -> {
                                TownyLog.warn("Unknown interaction type: $interactionType")
                            }
                        }
                    } catch (e: Exception) {
                        TownyLog.error("Exception in interaction handler", e)
                    }
                } catch (e: Exception) {
                    TownyLog.error("Exception in packet handler", e)
                }
            }
        }
    }
}
