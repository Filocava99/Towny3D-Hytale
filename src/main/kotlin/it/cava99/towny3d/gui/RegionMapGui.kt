package it.cava99.towny3d.gui

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.gui.codec.RegionMapEventData
import it.cava99.towny3d.model.*
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.util.concurrent.CompletableFuture

/**
 * Interactive chunk grid GUI for viewing and claiming regions.
 * Follows SimpleClaims ChunkInfoGui pattern exactly.
 *
 * @param playerRef Reference to the player
 * @param town The town whose claims to show
 * @param viewerResidentId The resident ID of the viewer
 * @param initialChunkX The chunk X coordinate to center the map on
 * @param initialChunkZ The chunk Z coordinate to center the map on
 */
class RegionMapGui(
    playerRef: PlayerRef,
    town: Town,
    private val viewerResidentId: ResidentId,
    private val initialChunkX: Int,
    private val initialChunkZ: Int
) : InteractiveCustomUIPage<RegionMapEventData>(
    playerRef,
    CustomPageLifetime.CanDismiss,
    RegionMapEventData.CODEC
) {
    private val plugin = Towny3D.instance
    private var town: Town = town  // Make var so we can refresh from cache

    // Player position (set from constructor, may update in build)
    private var chunkX: Int = initialChunkX
    private var chunkZ: Int = initialChunkZ
    private var dimension: String = "world"

    // Map asset generation
    private var mapAsset: CompletableFuture<RegionMapAsset?>? = null

    // Store ref/store for rebuilding from async callbacks
    private var lastRef: Ref<EntityStore>? = null
    private var lastStore: Store<EntityStore>? = null

    override fun handleDataEvent(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        data: RegionMapEventData
    ) {
        super.handleDataEvent(ref, store, data)

        // Store for use in async callbacks
        lastRef = ref
        lastStore = store

        val actionData = data.action
        if (actionData != null) {
            val actions = actionData.split(":")
            if (actions.size >= 3) {
                val button = actions[0]
                val x = actions[1].toIntOrNull()
                val z = actions[2].toIntOrNull()

                if (x != null && z != null) {

                    if (button == "LeftClicking") {
                        handleClaimChunk(ref, store, x, z)
                        return
                    }

                    if (button == "RightClicking") {
                        handleUnclaimChunk(ref, store, x, z)
                        return
                    }
                }
            } else {
            }

            // Fallback rebuild for unrecognized actions
            rebuildAndUpdate(ref, store)
            return
        }

        sendUpdate()
    }

    private fun handleClaimChunk(ref: Ref<EntityStore>, store: Store<EntityStore>, x: Int, z: Int) {
        val chunkPos = ChunkPos(x, z)

        // Check if already claimed
        if (plugin.spatialIndex.getRegionAtIgnoringY(chunkPos) != null) {
            TownyLog.info("RegionMapGui: Chunk ($x, $z) already claimed, skipping")
            rebuildAndUpdate(ref, store)
            return
        }

        // Check if town has enough layers
        if (town.layers >= town.maxLayers) {
            TownyLog.info("RegionMapGui: Town ${town.name} has reached max layers (${town.maxLayers}), cannot claim more")
            rebuildAndUpdate(ref, store)
            return
        }

        try {
            runBlocking(Dispatchers.Default) {
                plugin.regionService.createRegion(
                    townId = town.id,
                    chunkPos = chunkPos,
                    y1 = -64,
                    y2 = 320,
                    type = RegionType.NORMAL
                )
            }
            // Refresh town data from cache to get updated layer count
            refreshTownFromCache()
        } catch (e: Exception) {
            TownyLog.error("RegionMapGui: Failed to claim chunk ($x, $z) for town ${town.name}", e)
        }
        rebuildAndUpdate(ref, store)
    }

    private fun handleUnclaimChunk(ref: Ref<EntityStore>, store: Store<EntityStore>, x: Int, z: Int) {
        val chunkPos = ChunkPos(x, z)
        val regionId = plugin.spatialIndex.getRegionAtIgnoringY(chunkPos)
        if (regionId == null) {
            TownyLog.warn("RegionMapGui: No region found at chunk ($x, $z) for unclaim")
            rebuildAndUpdate(ref, store)
            return
        }

        // Check if it belongs to our town
        val region = plugin.regionCache.getSync(regionId)
        if (region == null) {
            TownyLog.warn("RegionMapGui: Region $regionId not found in cache for unclaim")
            rebuildAndUpdate(ref, store)
            return
        }
        if (region.townId != town.id) {
            TownyLog.info("RegionMapGui: Region at ($x, $z) belongs to ${region.townId}, not ${town.id}, skipping unclaim")
            rebuildAndUpdate(ref, store)
            return
        }

        try {
            runBlocking(Dispatchers.Default) {
                plugin.regionService.deleteRegion(regionId)
            }
            // Refresh town data from cache to get updated layer count
            refreshTownFromCache()
        } catch (e: Exception) {
            TownyLog.error("RegionMapGui: Failed to unclaim chunk ($x, $z) for town ${town.name}", e)
        }
        rebuildAndUpdate(ref, store)
    }

    /**
     * Refreshes town data from cache after claim/unclaim operations.
     * This ensures the displayed layer count is up-to-date.
     */
    private fun refreshTownFromCache() {
        try {
            runBlocking(Dispatchers.Default) {
                val refreshedTown = plugin.townCache.get(town.id)
                if (refreshedTown != null) {
                    town = refreshedTown
                    TownyLog.debug("RegionMapGui: Refreshed town data for ${town.name}, layers: ${town.layers}/${town.maxLayers}")
                } else {
                    TownyLog.warn("RegionMapGui: Failed to refresh town ${town.id} - town not found in cache")
                }
            }
        } catch (e: Exception) {
            TownyLog.error("RegionMapGui: Failed to refresh town data", e)
        }
    }

    /**
     * Rebuilds the full map UI and sends update to the client.
     */
    private fun rebuildAndUpdate(ref: Ref<EntityStore>, store: Store<EntityStore>) {
        val commandBuilder = UICommandBuilder()
        val eventBuilder = UIEventBuilder()
        build(ref, commandBuilder, eventBuilder, store)
        sendUpdate(commandBuilder, eventBuilder, true)
    }

    override fun build(
        ref: Ref<EntityStore>,
        ui: UICommandBuilder,
        ev: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        // Store ref/store for async rebuilds (map asset callback)
        lastRef = ref
        lastStore = store

        ui.append("Pages/HytaleTowny_RegionMap.ui")

        // Get world for logging
        val world = store.getExternalData().world
        dimension = world.name

        // Generate map asset (SimpleClaims pattern) - only once
        if (mapAsset == null) {
            // Send empty/placeholder first
            val emptyAsset = RegionMapAsset.empty()
            if (emptyAsset != null) {
                RegionMapAsset.sendToPlayer(playerRef.packetHandler, emptyAsset)
            }

            // Generate actual map for 17x17 chunk area centered on player
            mapAsset = RegionMapAsset.generate(playerRef, chunkX - 8, chunkZ - 8, chunkX + 8, chunkZ + 8)

            mapAsset?.thenAccept { asset ->
                if (asset == null) return@thenAccept
                RegionMapAsset.sendToPlayer(playerRef.packetHandler, asset)
                val savedRef = lastRef
                val savedStore = lastStore
                if (savedRef != null && savedStore != null) {
                    rebuildAndUpdate(savedRef, savedStore)
                } else {
                    sendUpdate()
                }
            }
        }

        // Set claim counts
        val claimCount = town.layers
        val maxClaims = town.maxLayers
        ui.set("#ClaimedChunksInfo #ClaimedChunksCount.Text", "$claimCount")
        ui.set("#ClaimedChunksInfo #MaxChunksCount.Text", "$maxClaims")

        val hytaleGold = "#93844c"
        val ownTownColor = Color(59, 130, 246, 128)  // Blue with alpha
        val otherTownColor = Color(239, 68, 68, 128)  // Red with alpha

        // Build 17x17 grid (SimpleClaims uses 8*2+1 = 17)
        for (z in 0..(8 * 2)) {
            // Create row container (SimpleClaims pattern)
            ui.appendInline("#ChunkCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }")

            for (x in 0..(8 * 2)) {
                // Append chunk entry to the row
                ui.append("#ChunkCards[$z]", "Pages/HytaleTowny_ChunkEntry.ui")

                val worldChunkX = chunkX + x - 8
                val worldChunkZ = chunkZ + z - 8
                val chunkPos = ChunkPos(worldChunkX, worldChunkZ)

                // Check if this is player's position
                val isPlayerPos = (z - 8 == 0 && x - 8 == 0)
                if (isPlayerPos) {
                    ui.set("#ChunkCards[$z][$x].Text", "+")
                }

                // Look up region at this chunk
                val regionId = plugin.spatialIndex.getRegionAtIgnoringY(chunkPos)

                if (regionId != null) {
                    val region = plugin.regionCache.getSync(regionId)
                    if (region != null) {
                        val ownerTown = plugin.townCache.getSync(region.townId)
                        if (ownerTown != null) {
                            // Determine color based on ownership
                            val color = if (region.townId == town.id) {
                                ownTownColor
                            } else {
                                otherTownColor
                            }

                            ui.set("#ChunkCards[$z][$x].Background.Color", ColorParseUtil.colorToHexAlpha(color))
                            ui.set("#ChunkCards[$z][$x].OutlineColor", ColorParseUtil.colorToHexAlpha(color))
                            ui.set("#ChunkCards[$z][$x].OutlineSize", 1)

                            // Build tooltip
                            var tooltip = MessageHelper.multiLine()
                                .append(Message.raw("Owner: ").bold(true).color(hytaleGold))
                                .append(Message.raw(ownerTown.name))
                                .nl()
                                .append(Message.raw("Y: ${region.y1} to ${region.y2}"))

                            if (region.townId == town.id) {
                                tooltip = tooltip.nl().nl()
                                    .append(Message.raw("*Right Click to Unclaim*")
                                        .bold(true)
                                        .color(Color.RED.darker().darker()))
                            }

                            ui.set("#ChunkCards[$z][$x].TooltipTextSpans", tooltip.build())

                            // Bind right-click for unclaiming own land
                            if (region.townId == town.id) {
                                ev.addEventBinding(
                                    CustomUIEventBindingType.RightClicking,
                                    "#ChunkCards[$z][$x]",
                                    EventData.of("Action", "RightClicking:$worldChunkX:$worldChunkZ")
                                )
                            }
                        }
                    }
                } else {
                    // Wilderness
                    var tooltip = MessageHelper.multiLine()
                        .append(Message.raw("Wilderness").bold(true).color(Color.GREEN.darker()))

                    tooltip = tooltip.nl().nl()
                        .append(Message.raw("*Left Click to claim*").bold(true).color(Color.GRAY))

                    ui.set("#ChunkCards[$z][$x].TooltipTextSpans", tooltip.build())

                    // Bind left-click for claiming
                    ev.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#ChunkCards[$z][$x]",
                        EventData.of("Action", "LeftClicking:$worldChunkX:$worldChunkZ")
                    )
                }
            }
        }
    }

}

/**
 * Helper for building multi-line Messages (SimpleClaims pattern).
 */
object MessageHelper {
    fun multiLine(): ML = ML()

    class ML {
        private val messages = mutableListOf<Message>()

        fun append(message: Message): ML {
            messages.add(message)
            return this
        }

        fun nl(): ML {
            messages.add(Message.raw("\n"))
            return this
        }

        fun build(): Message = Message.join(*messages.toTypedArray())
    }
}
