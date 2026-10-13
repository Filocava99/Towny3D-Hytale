package it.cava99.towny3d.util

import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages player selections for region claims.
 * Players use a "claim stick" to select two points to define a claim area.
 *
 * This file is intentionally large because:
 * - Selection logic is inherently cohesive - all operations work on the same selection state
 * - Requires both async (coroutine-safe) and sync (world thread) versions of the same methods
 * - The SelectedVolume inner class contains related geometric calculations that can't be separated
 * - Duplicating logic between async/sync versions would increase maintenance burden
 *
 * Future improvements:
 * - Consider extracting SelectedVolume to a separate file if it grows additional functionality
 * - Could potentially use a code generator to create sync versions from async versions
 *
 * @property selections Map of player UUID to their selection
 */
class SelectionManager {
    private data class Selection(
        var primary: BlockLocation? = null,
        var secondary: BlockLocation? = null
    )

    private val selections = ConcurrentHashMap<UUID, Selection>()
    private val mutex = Mutex()

    /**
     * Gets a player's current selection.
     */
    suspend fun getSelection(playerUuid: UUID): Pair<BlockLocation?, BlockLocation?> = mutex.withLock {
        val selection = selections[playerUuid]
        return selection?.primary to selection?.secondary
    }

    /**
     * Sets the primary (left-click) selection point.
     */
    suspend fun setPrimary(playerUuid: UUID, location: BlockLocation): Unit = mutex.withLock {
        val selection = selections.getOrPut(playerUuid) { Selection() }
        selection.primary = location
    }

    /**
     * Sets the secondary (right-click) selection point.
     */
    suspend fun setSecondary(playerUuid: UUID, location: BlockLocation): Unit = mutex.withLock {
        val selection = selections.getOrPut(playerUuid) { Selection() }
        selection.secondary = location
    }

    /**
     * Clears a player's selection.
     */
    suspend fun clearSelection(playerUuid: UUID): Unit = mutex.withLock {
        selections.remove(playerUuid)
    }

    /**
     * Checks if a player has a complete selection (both points set).
     */
    suspend fun hasCompleteSelection(playerUuid: UUID): Boolean = mutex.withLock {
        val selection = selections[playerUuid] ?: return false
        return selection.primary != null && selection.secondary != null
    }

    /**
     * Gets the chunks covered by a player's selection.
     * Returns empty list if selection is incomplete.
     */
    suspend fun getSelectedChunks(playerUuid: UUID): List<ChunkPos> = mutex.withLock {
        val selection = selections[playerUuid] ?: return emptyList()
        val primary = selection.primary ?: return emptyList()
        val secondary = selection.secondary ?: return emptyList()

        // Convert block coordinates to chunk coordinates
        val minChunkX = minOf(primary.x shr 5, secondary.x shr 5)
        val maxChunkX = maxOf(primary.x shr 5, secondary.x shr 5)
        val minChunkZ = minOf(primary.z shr 5, secondary.z shr 5)
        val maxChunkZ = maxOf(primary.z shr 5, secondary.z shr 5)

        val chunks = mutableListOf<ChunkPos>()
        for (x in minChunkX..maxChunkX) {
            for (z in minChunkZ..maxChunkZ) {
                chunks.add(ChunkPos(x, z))
            }
        }
        return chunks
    }

    /**
     * Gets the volume defined by the selection.
     * Returns null if selection is incomplete.
     */
    suspend fun getSelectedVolume(playerUuid: UUID): SelectedVolume? = mutex.withLock {
        val selection = selections[playerUuid] ?: return null
        val primary = selection.primary ?: return null
        val secondary = selection.secondary ?: return null

        return SelectedVolume(
            world = primary.world,
            minX = minOf(primary.x, secondary.x),
            maxX = maxOf(primary.x, secondary.x),
            minY = minOf(primary.y, secondary.y),
            maxY = maxOf(primary.y, secondary.y),
            minZ = minOf(primary.z, secondary.z),
            maxZ = maxOf(primary.z, secondary.z)
        )
    }

    /**
     * Gets the Y-range defined by the selection.
     * Useful for vertical claims (layers).
     */
    suspend fun getSelectedYRange(playerUuid: UUID): IntRange? = mutex.withLock {
        val selection = selections[playerUuid] ?: return null
        val primary = selection.primary ?: return null
        val secondary = selection.secondary ?: return null

        return minOf(primary.y, secondary.y)..maxOf(primary.y, secondary.y)
    }

    /**
     * Calculates the cost to claim the selected area.
     */
    suspend fun calculateClaimCost(playerUuid: UUID, costPerLayer: Double): Double = mutex.withLock {
        val yRange = getSelectedYRange(playerUuid) ?: return 0.0
        val height = yRange.endInclusive - yRange.start + 1
        val chunkCount = getSelectedChunks(playerUuid).size
        return height * chunkCount * costPerLayer
    }

    /**
     * Gets a summary of the current selection for display.
     */
    suspend fun getSelectionSummary(playerUuid: UUID): String? = mutex.withLock {
        val selection = selections[playerUuid] ?: return null
        val primary = selection.primary
        val secondary = selection.secondary

        if (primary == null && secondary == null) {
            return null
        }

        return buildString {
            if (primary != null) {
                append("Position 1: ")
                append(MessageFormatter.formatCoords(primary.x, primary.y, primary.z))
            } else {
                append("Position 1: Not set")
            }

            append("\n")

            if (secondary != null) {
                append("Position 2: ")
                append(MessageFormatter.formatCoords(secondary.x, secondary.y, secondary.z))
            } else {
                append("Position 2: Not set")
            }

            if (primary != null && secondary != null) {
                val volume = getSelectedVolume(playerUuid)!!
                val chunks = getSelectedChunks(playerUuid).size

                append("\n\n")
                append("Volume: ")
                append(MessageFormatter.formatCoords(
                    volume.maxX - volume.minX + 1,
                    volume.maxY - volume.minY + 1,
                    volume.maxZ - volume.minZ + 1
                ))

                append("\n")
                append("Chunks: $chunks")
            }
        }
    }

    /**
     * Gets all players with active selections.
     */
    suspend fun getActivePlayers(): List<UUID> {
        return selections.keys.toList()
    }

    /**
     * Checks if a player has an active selection.
     */
    suspend fun hasSelection(playerUuid: UUID): Boolean {
        return selections.containsKey(playerUuid)
    }

    /**
     * Removes all selections (for shutdown/reset).
     */
    suspend fun clearAll() = mutex.withLock {
        selections.clear()
    }

    // ========== Synchronous (unsafe) methods ==========
    // These methods bypass the mutex and should ONLY be called when already in a thread-safe context
    // (e.g., inside world.execute block). Do NOT call from coroutines or async code.

    /**
     * Synchronous version of setPrimary - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun setPrimarySync(playerUuid: UUID, location: BlockLocation) {
        val selection = selections.getOrPut(playerUuid) { Selection() }
        selection.primary = location
    }

    /**
     * Synchronous version of setSecondary - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun setSecondarySync(playerUuid: UUID, location: BlockLocation) {
        val selection = selections.getOrPut(playerUuid) { Selection() }
        selection.secondary = location
    }

    /**
     * Synchronous version of getSelectionSummary - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun getSelectionSummarySync(playerUuid: UUID): String? {
        val selection = selections[playerUuid] ?: return null
        val primary = selection.primary
        val secondary = selection.secondary

        if (primary == null && secondary == null) {
            return null
        }

        return buildString {
            if (primary != null) {
                append("Position 1: ")
                append(MessageFormatter.formatCoords(primary.x, primary.y, primary.z))
            } else {
                append("Position 1: Not set")
            }

            append("\n")

            if (secondary != null) {
                append("Position 2: ")
                append(MessageFormatter.formatCoords(secondary.x, secondary.y, secondary.z))
            } else {
                append("Position 2: Not set")
            }

            if (primary != null && secondary != null) {
                val volume = getSelectedVolumeSync(playerUuid)!!
                val chunks = getSelectedChunksSync(playerUuid).size

                append("\n\n")
                append("Volume: ")
                append(MessageFormatter.formatCoords(
                    volume.maxX - volume.minX + 1,
                    volume.maxY - volume.minY + 1,
                    volume.maxZ - volume.minZ + 1
                ))

                append("\n")
                append("Chunks: $chunks")
            }
        }
    }

    /**
     * Synchronous version of getSelectedVolume - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun getSelectedVolumeSync(playerUuid: UUID): SelectedVolume? {
        val selection = selections[playerUuid] ?: return null
        val primary = selection.primary ?: return null
        val secondary = selection.secondary ?: return null

        return SelectedVolume(
            world = primary.world,
            minX = minOf(primary.x, secondary.x),
            maxX = maxOf(primary.x, secondary.x),
            minY = minOf(primary.y, secondary.y),
            maxY = maxOf(primary.y, secondary.y),
            minZ = minOf(primary.z, secondary.z),
            maxZ = maxOf(primary.z, secondary.z)
        )
    }

    /**
     * Synchronous version of getSelectedChunks - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun getSelectedChunksSync(playerUuid: UUID): List<ChunkPos> {
        val selection = selections[playerUuid] ?: return emptyList()
        val primary = selection.primary ?: return emptyList()
        val secondary = selection.secondary ?: return emptyList()

        // Convert block coordinates to chunk coordinates
        val minChunkX = minOf(primary.x shr 5, secondary.x shr 5)
        val maxChunkX = maxOf(primary.x shr 5, secondary.x shr 5)
        val minChunkZ = minOf(primary.z shr 5, secondary.z shr 5)
        val maxChunkZ = maxOf(primary.z shr 5, secondary.z shr 5)

        val chunks = mutableListOf<ChunkPos>()
        for (x in minChunkX..maxChunkX) {
            for (z in minChunkZ..maxChunkZ) {
                chunks.add(ChunkPos(x, z))
            }
        }
        return chunks
    }

    /**
     * Synchronous version of getSelectedYRange - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun getSelectedYRangeSync(playerUuid: UUID): IntRange? {
        val selection = selections[playerUuid] ?: return null
        val primary = selection.primary ?: return null
        val secondary = selection.secondary ?: return null

        return minOf(primary.y, secondary.y)..maxOf(primary.y, secondary.y)
    }

    /**
     * Synchronous version of clearSelection - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun clearSelectionSync(playerUuid: UUID) {
        selections.remove(playerUuid)
    }

    /**
     * Synchronous version of hasCompleteSelection - NOT thread-safe by itself.
     * Only call when already in a thread-safe context (e.g., inside world.execute).
     */
    fun hasCompleteSelectionSync(playerUuid: UUID): Boolean {
        val selection = selections[playerUuid] ?: return false
        return selection.primary != null && selection.secondary != null
    }

    /**
     * Data class representing a 3D volume selection.
     */
    data class SelectedVolume(
        val world: String,
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int,
        val minZ: Int,
        val maxZ: Int
    ) {
        /**
         * Gets the height of this volume.
         */
        fun getHeight(): Int = maxY - minY + 1

        /**
         * Gets all chunks intersected by this volume.
         */
        fun getIntersectingChunks(): List<ChunkPos> {
            val minChunkX = minX shr 5 // Divide by 32
            val maxChunkX = maxX shr 5
            val minChunkZ = minZ shr 5
            val maxChunkZ = maxZ shr 5

            val chunks = mutableListOf<ChunkPos>()
            for (cx in minChunkX..maxChunkX) {
                for (cz in minChunkZ..maxChunkZ) {
                    chunks.add(ChunkPos(cx, cz))
                }
            }
            return chunks
        }
    }
}
