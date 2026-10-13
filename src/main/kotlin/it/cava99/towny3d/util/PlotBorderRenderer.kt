package it.cava99.towny3d.util

import com.hypixel.hytale.protocol.Color
import com.hypixel.hytale.protocol.Direction
import com.hypixel.hytale.protocol.Position
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem
import com.hypixel.hytale.server.core.io.PacketHandler
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.ParticleUtil
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.ChunkConstants
import it.cava99.towny3d.util.TimeConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Renders plot border visualizations for players using particles.
 * Spawns particle effects along plot boundaries.
 */
class PlotBorderRenderer(private val plugin: Towny3D) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.IO

    // Track active visualizations per player
    private val activeVisualizations = ConcurrentHashMap<UUID, PlotBorderVisualization>()

    companion object {
        /** Horizontal step between particles */
        const val PARTICLE_HORIZONTAL_STEP = 4

        /** Vertical step between particles */
        const val PARTICLE_VERTICAL_STEP = 5

        /** Particle scale for visibility */
        const val PARTICLE_SCALE = 2.0f
        const val PARTICLE_DURATION = 1.0f
    }

    /**
     * Show borders for a plot at the player's current location.
     */
    fun showPlotBorders(playerUuid: UUID, playerName: String) {
        // Cancel existing visualization for this player
        activeVisualizations[playerUuid]?.cancel()
        activeVisualizations.remove(playerUuid)

        // Create new visualization
        val visualization = PlotBorderVisualization(playerUuid, playerName, plugin, this)
        activeVisualizations[playerUuid] = visualization
        visualization.start()
    }

    /**
     * Hide borders for a player.
     */
    fun hidePlotBorders(playerUuid: UUID) {
        activeVisualizations[playerUuid]?.cancel()
        activeVisualizations.remove(playerUuid)
    }

    /**
     * Called when a visualization ends.
     */
    fun onVisualizationEnded(playerUuid: UUID) {
        activeVisualizations.remove(playerUuid)
    }

    /**
     * Get the PacketHandler for a player by UUID.
     */
    fun getPacketHandler(playerUuid: UUID): PacketHandler? {
        val universe = com.hypixel.hytale.server.core.universe.Universe.get()
        val playerRef = universe.getPlayer(playerUuid) ?: return null
        return playerRef.getPacketHandler()
    }

    /**
     * Cleanup all visualizations.
     */
    fun shutdown() {
        activeVisualizations.values.forEach { it.cancel() }
        activeVisualizations.clear()
        coroutineContext[Job]?.cancel()
    }

    /**
     * Manages a single plot border visualization for a player using particles.
     */
    class PlotBorderVisualization(
        private val playerUuid: UUID,
        private val playerName: String,
        private val plugin: Towny3D,
        private val renderer: PlotBorderRenderer
    ) {
        private val job = Job()
        private val scope = CoroutineScope(job + Dispatchers.IO)

        fun start() {
            scope.launch {
                while (job.isActive) {
                    try {
                        sendBorderUpdate()
                        delay(TimeConstants.PLOT_BORDER_UPDATE_INTERVAL_MS)
                    } catch (e: Exception) {
                        TownyLog.error("Error updating border for $playerName", e)
                    }
                }
            }
        }

        fun cancel() {
            job.cancel()
            renderer.onVisualizationEnded(playerUuid)
        }

        private suspend fun sendBorderUpdate() {
            val packetHandler = renderer.getPacketHandler(playerUuid) ?: return

            // Get the world first
            val universe = com.hypixel.hytale.server.core.universe.Universe.get()
            val world = universe.getDefaultWorld() ?: return

            // We need to execute on the world thread to access components
            world.execute {
                try {
                    val playerRef = universe.getPlayer(playerUuid) ?: return@execute

                    val entities = world.getEntityStore().getStore()
                    val entityStore = entities.getExternalData() as? com.hypixel.hytale.server.core.universe.world.storage.EntityStore ?: return@execute

                    val ref = entityStore.getRefFromUUID(playerUuid) ?: return@execute

                    val transformComponent = entities.getComponent(
                        ref,
                        com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType()
                    ) as? com.hypixel.hytale.server.core.modules.entity.component.TransformComponent ?: return@execute

                    val position = transformComponent.transform.position
                    val blockX = position.x.toInt()
                    val blockZ = position.z.toInt()
                    val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                    // Get regions outside of world.execute, then use them inside
                    val regions = kotlinx.coroutines.runBlocking {
                        plugin.regionService.getRegionsAt(chunkPos)
                    }

                    if (regions.isEmpty()) {
                        return@execute
                    }

                    // Find a region that contains the player vertically
                    val playerY = position.y.toInt()
                    val currentRegion = regions.firstOrNull { region ->
                        region.y1 <= playerY && region.y2 >= playerY
                    }

                    // If no region contains player vertically, show all regions at this chunk
                    // (so player can see what regions exist here)
                    if (currentRegion == null) {
                        regions.forEach { region ->
                            spawnBorderParticles(packetHandler, region)
                        }
                        return@execute
                    }

                    // Spawn particles along the border edges
                    spawnBorderParticles(packetHandler, currentRegion)

                } catch (e: Exception) {
                    TownyLog.error("Exception in world.execute", e)
                }
            }
        }

        /**
         * Spawns particles along the edges of the region border.
         * Shows both horizontal and vertical extent of the region.
         */
        private fun spawnBorderParticles(packetHandler: PacketHandler, region: it.cava99.towny3d.model.Region) {
            val minX = region.chunkPos.x * ChunkConstants.CHUNK_SIZE
            val maxX = minX + ChunkConstants.CHUNK_SIZE - 1
            val minZ = region.chunkPos.z * ChunkConstants.CHUNK_SIZE
            val maxZ = minZ + ChunkConstants.CHUNK_SIZE - 1
            val minY = region.y1
            val maxY = region.y2

            val step = PARTICLE_HORIZONTAL_STEP
            val verticalStep = PARTICLE_VERTICAL_STEP
            var particleCount = 0

            // Spawn particles at multiple vertical levels
            for (y in minY..maxY step verticalStep) {
                // Horizontal edges at this Y level
                for (x in minX..maxX step step) {
                    spawnParticle(packetHandler, x.toDouble(), y.toDouble(), minZ.toDouble())
                    spawnParticle(packetHandler, x.toDouble(), y.toDouble(), maxZ.toDouble())
                    particleCount += 2
                }

                for (z in minZ..maxZ step step) {
                    spawnParticle(packetHandler, minX.toDouble(), y.toDouble(), z.toDouble())
                    spawnParticle(packetHandler, maxX.toDouble(), y.toDouble(), z.toDouble())
                    particleCount += 2
                }
            }

            // Always include the top Y level if not already included
            if ((maxY - minY) % verticalStep != 0) {
                for (x in minX..maxX step step) {
                    spawnParticle(packetHandler, x.toDouble(), maxY.toDouble(), minZ.toDouble())
                    spawnParticle(packetHandler, x.toDouble(), maxY.toDouble(), maxZ.toDouble())
                    particleCount += 2
                }

                for (z in minZ..maxZ step step) {
                    spawnParticle(packetHandler, minX.toDouble(), maxY.toDouble(), z.toDouble())
                    spawnParticle(packetHandler, maxX.toDouble(), maxY.toDouble(), z.toDouble())
                    particleCount += 2
                }
            }

            // Add corner particles at all Y levels for better visibility
            for (y in minY..maxY step verticalStep) {
                spawnParticle(packetHandler, minX.toDouble(), y.toDouble(), minZ.toDouble())
                spawnParticle(packetHandler, maxX.toDouble(), y.toDouble(), minZ.toDouble())
                spawnParticle(packetHandler, minX.toDouble(), y.toDouble(), maxZ.toDouble())
                spawnParticle(packetHandler, maxX.toDouble(), y.toDouble(), maxZ.toDouble())
                particleCount += 4
            }
        }

        /**
         * Spawns a single particle at the given position.
         * Uses "Item" as a valid particle system ID.
         */
        private fun spawnParticle(packetHandler: PacketHandler, x: Double, y: Double, z: Double) {
            try {
                val position = Position(x, y, z)
                val color = Color(0.toByte(), 255.toByte(), 0.toByte()) // Green color for town claims

                // Use "Item" as the particle system ID (default from ItemEntityConfig)
                val packet = SpawnParticleSystem(
                    "Item", // Valid particle system ID
                    position,
                    null, // rotation
                    PARTICLE_SCALE,
                    color,
                    PARTICLE_DURATION
                )

                packetHandler.writeNoCache(packet)
            } catch (e: Exception) {
                TownyLog.error("Failed to spawn particle at ($x, $y, $z)", e)
            }
        }
    }
}
