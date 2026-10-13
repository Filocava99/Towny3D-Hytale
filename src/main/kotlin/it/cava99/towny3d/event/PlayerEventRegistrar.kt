package it.cava99.towny3d.event

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.universe.PlayerRef
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch

/**
 * Registers player-related event handlers with Hytale's EventBus.
 *
 * Handles:
 * - Player connect (load/create resident)
 * - Player disconnect (cleanup)
 * - Player chat (town/nation chat modes)
 */
class PlayerEventRegistrar(
    private val plugin: Towny3D
) {
    private val eventRegistry = plugin.getEventRegistry()

    /**
     * Registers all player event handlers.
     */
    fun registerAll() {
        registerConnectEvents()
        registerChatEvents()
        TownyLog.info("Registered player events")
    }

    /**
     * Registers player connect/disconnect events.
     */
    private fun registerConnectEvents() {
        // Player connect event - Load or create resident with player's name
        eventRegistry.register(PlayerConnectEvent::class.java) { event ->
            plugin.pluginScope.launch {
                try {
                    val playerRef = event.getPlayerRef()
                    val playerUuid = playerRef.getUuid()
                    val playerName = playerRef.username

                    // Load or create resident with the player's actual name
                    plugin.residentService.loadOrCreate(playerUuid, playerName)
                } catch (e: Exception) {
                    TownyLog.error("Error handling player connect event: ${e.message}", e)
                }
            }
        }

        // Player disconnect event
        eventRegistry.register(PlayerDisconnectEvent::class.java) { event ->
            plugin.pluginScope.launch {
                try {
                    // Clean up title tracking for this player
                    val playerUuid = event.getPlayerRef().getUuid()
                    plugin.titleTickingSystem.removePlayer(playerUuid)
                    // Clean up plot border visualizations
                    plugin.plotBorderRenderer.hidePlotBorders(playerUuid)
                } catch (e: Exception) {
                    TownyLog.warn("Error handling player disconnect event: ${e.message}")
                }
            }
        }
    }

    /**
     * Registers chat-related events.
     */
    private fun registerChatEvents() {
        // Player chat event
        eventRegistry.register(PlayerChatEvent::class.java) { event ->
            try {
                // Get player and message from event
                val sender = event.sender
                val playerUuid = sender.uuid
                val playerName = sender.username
                val message = event.content

                // Check if message is empty
                if (message.isBlank()) {
                    return@register
                }

                // Check resident's chat mode synchronously from cache
                val resident = plugin.residentCache.getSync(
                    ResidentId.fromPlayerUuid(playerUuid)
                )

                // Only process if player has a special chat mode
                if (resident != null && resident.chatMode != it.cava99.towny3d.model.ChatMode.GLOBAL) {
                    // Cancel the original event immediately so it doesn't propagate to global chat
                    event.setCancelled(true)

                    // Process the chat message asynchronously
                    plugin.pluginScope.launch {
                        try {
                            val result = plugin.chatModeService.processChatMessage(
                                playerUuid = playerUuid,
                                playerName = playerName,
                                message = message
                            )

                            // Get target UUIDs
                            val targetUuids = result.targets ?: return@launch

                            // Get all player refs
                            val allPlayerRefs = getAllPlayerRefs()

                            // Send the formatted message to each target player
                            for (playerRef in allPlayerRefs) {
                                if (playerRef.uuid in targetUuids) {
                                    try {
                                        playerRef.sendMessage(result.message)
                                    } catch (e: Exception) {
                                        TownyLog.warn("Failed to send message to ${playerRef.username}: ${e.message}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            TownyLog.error("Error processing town chat: ${e.message}", e)
                        }
                    }

                    return@register
                }
                // Otherwise, let the event propagate normally for global chat

            } catch (e: Exception) {
                TownyLog.error("Error handling chat event: ${e.message}", e)
            }
        }

        TownyLog.info("Registered chat events")
    }

    /**
     * Gets all online player refs from the universe.
     */
    private fun getAllPlayerRefs(): List<PlayerRef> {
        return try {
            val universe = com.hypixel.hytale.server.core.universe.Universe.get()
            universe.getPlayers().toList()
        } catch (e: Exception) {
            TownyLog.warn("Failed to get all player refs: ${e.message}")
            emptyList()
        }
    }
}
