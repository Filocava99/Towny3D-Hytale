package it.cava99.towny3d.item

import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.component.Store
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TimeConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the claim stick functionality.
 * Players use a special item to select two points for claiming regions.
 *
 * This file is intentionally large because:
 * - It represents a single cohesive item system with interaction, visualization, and claim logic
 * - Requires both async (coroutine-safe) and sync (world thread) versions of click handlers
 * - The VisualizationJob inner class manages particle effects lifecycle and is tightly coupled
 * - The ClaimResult sealed class defines all possible outcomes and must stay with the manager
 *
 * Future improvements:
 * - Consider extracting VisualizationJob to a separate file if visualization logic becomes complex
 * - Could extract ClaimResult to a separate file if used by other classes
 */
class ClaimStickManager(
    private val plugin: Towny3D
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeVisualizations = ConcurrentHashMap<UUID, VisualizationJob>()

    /**
     * Gets a PlayerRef from a UUID to send messages to the player.
     */
    private fun getPlayerRef(playerUuid: UUID): PlayerRef? {
        return try {
            val universe = Universe.get()
            val world = universe.getDefaultWorld() ?: return null

            var playerRef: PlayerRef? = null
            world.execute {
                val entityStore = world.getEntityStore()
                val ref = entityStore.getRefFromUUID(playerUuid) ?: return@execute
                val store: Store<EntityStore> = entityStore.getStore()
                playerRef = store.getComponent(ref, PlayerRef.getComponentType())
            }
            playerRef
        } catch (e: Exception) {
            logger.warning("Failed to get PlayerRef for UUID $playerUuid: ${e.message}")
            null
        }
    }

    /**
     * Handles left-click (primary position selection).
     */
    suspend fun onLeftClick(playerUuid: UUID, playerName: String, location: BlockLocation, playerRef: PlayerRef? = null) {
        plugin.selectionManager.setPrimary(playerUuid, location)

        val selection = plugin.selectionManager.getSelectionSummary(playerUuid)

        // Use provided playerRef or try to get it
        val ref = playerRef ?: getPlayerRef(playerUuid)

        if (ref != null) {
            // Send position 1 message with green coordinates
            val positionMsg = com.hypixel.hytale.server.core.Message.raw("[Pos1] ")
                .insert(com.hypixel.hytale.server.core.Message.raw("Position 1 set to "))
                .insert(com.hypixel.hytale.server.core.Message.raw("(${location.x}, ${location.y}, ${location.z})")
                    .color("#00FF00"))

            if (selection != null) {
                // Add selection summary on a new line
                ref.sendMessage(
                    positionMsg.insert(com.hypixel.hytale.server.core.Message.raw("\n" + selection))
                )
            } else {
                ref.sendMessage(positionMsg)
            }
        }

        logger.info("[$playerName] Position 1: ${location.x}, ${location.y}, ${location.z}")

        // Update visualization
        updateVisualization(playerUuid, playerName)
    }

    /**
     * Synchronous version of onLeftClick - for use inside world.execute blocks.
     * This bypasses coroutines and mutex locks since we're already in a thread-safe context.
     */
    fun onLeftClickSync(playerUuid: UUID, playerName: String, location: BlockLocation, playerRef: PlayerRef) {
        // Use synchronous SelectionManager methods (no mutex, no coroutine)
        plugin.selectionManager.setPrimarySync(playerUuid, location)
        val selection = plugin.selectionManager.getSelectionSummarySync(playerUuid)

        // Send position 1 message with green coordinates
        val positionMsg = com.hypixel.hytale.server.core.Message.raw("[Pos1] ")
            .insert(com.hypixel.hytale.server.core.Message.raw("Position 1 set to "))
            .insert(com.hypixel.hytale.server.core.Message.raw("(${location.x}, ${location.y}, ${location.z})")
                .color("#00FF00"))

        if (selection != null) {
            // Add selection summary on a new line
            playerRef.sendMessage(
                positionMsg.insert(com.hypixel.hytale.server.core.Message.raw("\n" + selection))
            )
        } else {
            playerRef.sendMessage(positionMsg)
        }

        logger.info("[$playerName] Position 1: ${location.x}, ${location.y}, ${location.z}")

        // Update visualization (still uses coroutine for the visual effect, but doesn't affect selection)
        updateVisualization(playerUuid, playerName)
    }

    /**
     * Handles right-click (secondary position selection).
     */
    suspend fun onRightClick(playerUuid: UUID, playerName: String, location: BlockLocation, playerRef: PlayerRef? = null) {
        try {
            logger.info("[DEBUG] onRightClick START for $playerName at ${location.x}, ${location.y}, ${location.z}, playerRef=${playerRef != null}")
            plugin.selectionManager.setSecondary(playerUuid, location)
            logger.info("[DEBUG] setSecondary completed")

            val selection = plugin.selectionManager.getSelectionSummary(playerUuid)
            logger.info("[DEBUG] getSelectionSummary completed: $selection")

            // Use provided playerRef or try to get it
            val ref = playerRef ?: getPlayerRef(playerUuid)
            logger.info("[DEBUG] PlayerRef resolved: ${ref != null}")

            if (ref != null) {
                // Send position 2 message with green coordinates
                val positionMsg = com.hypixel.hytale.server.core.Message.raw("[Pos2] ")
                    .insert(com.hypixel.hytale.server.core.Message.raw("Position 2 set to "))
                    .insert(com.hypixel.hytale.server.core.Message.raw("(${location.x}, ${location.y}, ${location.z})")
                        .color("#00FF00"))

                if (selection != null) {
                    // Add selection summary on a new line
                    ref.sendMessage(
                        positionMsg.insert(com.hypixel.hytale.server.core.Message.raw("\n" + selection))
                    )
                } else {
                    ref.sendMessage(positionMsg)
                }
                logger.info("[DEBUG] Message sent to player")
            }

            logger.info("[$playerName] Position 2: ${location.x}, ${location.y}, ${location.z}")

            // Update visualization
            updateVisualization(playerUuid, playerName)
            logger.info("[DEBUG] onRightClick completed successfully")
        } catch (e: Exception) {
            logger.severe("[DEBUG] onRightClick FAILED: ${e.message}")
        }
    }

    /**
     * Synchronous version of onRightClick - for use inside world.execute blocks.
     * This bypasses coroutines and mutex locks since we're already in a thread-safe context.
     */
    fun onRightClickSync(playerUuid: UUID, playerName: String, location: BlockLocation, playerRef: PlayerRef) {
        try {
            // Use synchronous SelectionManager methods (no mutex, no coroutine)
            plugin.selectionManager.setSecondarySync(playerUuid, location)
            val selection = plugin.selectionManager.getSelectionSummarySync(playerUuid)

            // Send position 2 message with green coordinates
            val positionMsg = com.hypixel.hytale.server.core.Message.raw("[Pos2] ")
                .insert(com.hypixel.hytale.server.core.Message.raw("Position 2 set to "))
                .insert(com.hypixel.hytale.server.core.Message.raw("(${location.x}, ${location.y}, ${location.z})")
                    .color("#00FF00"))

            if (selection != null) {
                // Add selection summary on a new line
                playerRef.sendMessage(
                    positionMsg.insert(com.hypixel.hytale.server.core.Message.raw("\n" + selection))
                )
            } else {
                playerRef.sendMessage(positionMsg)
            }

            logger.info("[$playerName] Position 2: ${location.x}, ${location.y}, ${location.z}")

            // Update visualization (still uses coroutine for the visual effect, but doesn't affect selection)
            updateVisualization(playerUuid, playerName)
        } catch (e: Exception) {
            logger.severe("[ERROR] onRightClickSync FAILED: ${e.message}")
        }
    }

    /**
     * Updates the particle visualization for a player's selection.
     */
    private fun updateVisualization(playerUuid: UUID, playerName: String) {
        // Cancel existing visualization
        activeVisualizations[playerUuid]?.cancel()
        activeVisualizations.remove(playerUuid)

        scope.launch {
            // Use synchronous method since we're already in a coroutine context
            // and the selection was just updated
            val volume = plugin.selectionManager.getSelectedVolumeSync(playerUuid) ?: return@launch

            // Create new visualization
            val job = VisualizationJob(playerUuid, playerName, volume)
            activeVisualizations[playerUuid] = job
            job.start()
        }
    }

    /**
     * Clears visualization for a player.
     */
    fun clearVisualization(playerUuid: UUID) {
        activeVisualizations[playerUuid]?.cancel()
        activeVisualizations.remove(playerUuid)
    }

    /**
     * Handles player disconnect.
     */
    fun onPlayerDisconnect(playerUuid: UUID) {
        scope.launch {
            plugin.selectionManager.clearSelection(playerUuid)
        }
        clearVisualization(playerUuid)
    }

    /**
     * Handles claim confirmation.
     */
    suspend fun confirmClaim(playerUuid: UUID, playerName: String): ClaimResult {
        val plugin = Towny3D.instance

        if (!plugin.selectionManager.hasCompleteSelection(playerUuid)) {
            return ClaimResult.IncompleteSelection
        }

        val volume = plugin.selectionManager.getSelectedVolume(playerUuid)
            ?: return ClaimResult.IncompleteSelection

        val chunks = volume.getIntersectingChunks()
        val resident = plugin.residentService.getResidentByUuid(playerUuid)
            ?: return ClaimResult.NotResident

        val townId = resident.townId
            ?: return ClaimResult.NotInTown

        val town = plugin.townService.getTown(townId)
            ?: return ClaimResult.TownNotFound

        // Check permissions
        if (town.mayorId != resident.id && resident.id !in town.assistantIds) {
            return ClaimResult.NoPermission
        }

        // Calculate cost
        val config = plugin.getConfig()
        val height = volume.getHeight()
        val cost = height * chunks.size * config.layerCost

        // Check balance
        if (town.balance < cost) {
            return ClaimResult.InsufficientFunds(cost, town.balance)
        }

        // Check for conflicts
        for (chunk in chunks) {
            val existing = plugin.regionService.getRegionsAt(chunk)
            if (existing.isNotEmpty()) {
                return ClaimResult.AlreadyClaimed(chunk)
            }
        }

        // Create regions
        val regionsCreated = mutableListOf<it.cava99.towny3d.model.Region>()
        for (chunk in chunks) {
            try {
                val region = plugin.regionService.createRegion(
                    chunkPos = chunk,
                    y1 = volume.minY,
                    y2 = volume.maxY,
                    townId = townId,
                    type = it.cava99.towny3d.model.RegionType.NORMAL
                )
                regionsCreated.add(region)
            } catch (e: Exception) {
                // Rollback
                regionsCreated.forEach { plugin.regionService.deleteRegion(it.id) }
                return ClaimResult.Error(e.message ?: "Unknown error")
            }
        }

        // Deduct cost
        if (cost > 0) {
            plugin.townService.withdraw(townId, cost)
        }

        // Clear selection
        plugin.selectionManager.clearSelection(playerUuid)
        clearVisualization(playerUuid)

        return ClaimResult.Success(regionsCreated.size, cost)
    }

    /**
     * Result of a claim operation.
     */
    sealed class ClaimResult {
        data class Success(val regionCount: Int, val cost: Double) : ClaimResult()
        object IncompleteSelection : ClaimResult()
        object NotResident : ClaimResult()
        object NotInTown : ClaimResult()
        object TownNotFound : ClaimResult()
        object NoPermission : ClaimResult()
        data class InsufficientFunds(val required: Double, val available: Double) : ClaimResult()
        data class AlreadyClaimed(val chunk: it.cava99.towny3d.model.ChunkPos) : ClaimResult()
        data class Error(val message: String) : ClaimResult()
    }

    /**
     * Manages particle visualization for a selection.
     */
    private inner class VisualizationJob(
        private val playerUuid: UUID,
        private val playerName: String,
        private val volume: it.cava99.towny3d.util.SelectionManager.SelectedVolume
    ) : CoroutineScope {
        private val job = Job()
        override val coroutineContext = job + Dispatchers.IO

        fun start() {
            launch {
                while (job.isActive) {
                    showSelectionOutline()
                    delay(TimeConstants.CLAIM_STICK_UPDATE_INTERVAL_MS)
                }
            }
        }

        fun cancel() {
            job.cancel()
        }

        private fun showSelectionOutline() {
            // TODO(2026-02-22): Send particle packets to show the selection outline
            // This requires Hytale's particle API
            logger.fine("Showing selection for $playerName: $volume")

            // For now, we'll log the corners that should be highlighted
            val corners = listOf(
                Pair(volume.minX, volume.minZ),
                Pair(volume.maxX, volume.minZ),
                Pair(volume.minX, volume.maxZ),
                Pair(volume.maxX, volume.maxZ)
            )

            corners.forEach { (x, z) ->
                logger.fine("Corner at ($x, ${volume.minY}, $z) to ($x, ${volume.maxY}, $z)")
            }
        }
    }

    companion object {
        private val logger = java.util.logging.Logger.getLogger(ClaimStickManager::class.java.name)
    }
}
