package it.cava99.towny3d.cache

import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Spatial index for regions.
 *
 * Simplified version using chunk-based indexing.
 * Full KD-Tree integration can be added later.
 *
 * Note: This is NOT a cache - it's an in-memory spatial index for fast region lookups.
 * It does not persist data and is rebuilt when regions change.
 *
 * @property rebuildLock Mutex for thread-safe rebuilds
 */
class SpatialRegionIndex {
    private val rebuildLock = Mutex()

    // Simple chunk-based index: Map<chunkKey, List<Region>>
    private val chunkIndex: MutableMap<String, List<Region>> = mutableMapOf()

    // Keep a map for quick lookups by region ID
    private val regionMap: MutableMap<RegionId, Region> = mutableMapOf()

    /**
     * Rebuilds the spatial index with the provided regions.
     */
    suspend fun rebuild(regions: Collection<Region>) = rebuildLock.withLock {
        // Clear existing data
        chunkIndex.clear()
        regionMap.clear()

        // Index by chunk position
        val byChunk = regions.groupBy { "${it.chunkPos.x},${it.chunkPos.z}" }
        chunkIndex.putAll(byChunk)

        // Keep region map
        regions.forEach { regionMap[it.id] = it }
    }

    /**
     * Gets the region at a specific location.
     * @param location The block location to check
     * @return The region ID, or null if wilderness
     */
    fun getRegionAt(location: BlockLocation): RegionId? {
        val chunkKey = "${location.chunkPos.x},${location.chunkPos.z}"
        val regions = chunkIndex[chunkKey] ?: return null

        // Find region containing this Y coordinate
        return regions.find { location.y in it.y1..it.y2 }?.id
    }

    /**
     * Gets the region at a specific chunk and Y coordinate.
     * @param chunkPos The chunk position
     * @param y The Y coordinate
     * @return The region ID, or null if wilderness
     */
    fun getRegionAt(chunkPos: ChunkPos, y: Int): RegionId? {
        val chunkKey = "${chunkPos.x},${chunkPos.z}"
        val regions = chunkIndex[chunkKey] ?: return null

        // Find region containing this Y coordinate
        return regions.find { y in it.y1..it.y2 }?.id
    }

    /**
     * Gets ANY region at a specific chunk position, ignoring Y coordinate.
     * This is used for map rendering where Y level doesn't matter.
     * @param chunkPos The chunk position
     * @return The first region ID at this chunk, or null if wilderness
     */
    fun getRegionAtIgnoringY(chunkPos: ChunkPos): RegionId? {
        val chunkKey = "${chunkPos.x},${chunkPos.z}"
        val regions = chunkIndex[chunkKey]
        if (regions != null) {
            return regions.firstOrNull()?.id
        }
        return null
    }

    /**
     * Debug method to get all chunk keys in the index.
     */
    fun debugGetAllKeys(): Set<String> = chunkIndex.keys

    /**
     * Debug method to get the number of entries (same as size()).
     */
    fun debugSize(): Int = chunkIndex.size

    /**
     * Clears the spatial index.
     */
    suspend fun clear() = rebuildLock.withLock {
        chunkIndex.clear()
        regionMap.clear()
    }

    /**
     * Removes a specific region from the spatial index.
     * @param region The region to remove
     */
    suspend fun remove(region: Region) = rebuildLock.withLock {
        val chunkKey = "${region.chunkPos.x},${region.chunkPos.z}"
        val regions = chunkIndex[chunkKey] ?: return@withLock

        val updated = regions.filter { it.id != region.id }
        if (updated.isEmpty()) {
            chunkIndex.remove(chunkKey)
        } else {
            chunkIndex[chunkKey] = updated
        }

        regionMap.remove(region.id)
    }

    /**
     * Gets the number of chunks in the index.
     */
    fun size(): Int = chunkIndex.size

    companion object {
        /**
         * Extension function to convert BlockLocation to chunk key.
         */
        fun BlockLocation.toChunkKey(): String = "${chunkPos.x},${chunkPos.z}"
    }
}
