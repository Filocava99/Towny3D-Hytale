package it.cava99.towny3d.service.region

import it.cava99.towny3d.cache.RegionCache
import it.cava99.towny3d.cache.SpatialRegionIndex
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId

/**
 * Handles spatial index queries for regions.
 * Provides fast lookups by location without mutex locking.
 */
class RegionIndexService(
    private val regionCache: RegionCache,
    private val spatialIndex: SpatialRegionIndex
) {
    suspend fun getRegionAt(location: BlockLocation): Region? {
        val regionId = spatialIndex.getRegionAt(location) ?: return null
        return regionCache.get(regionId)
    }

    suspend fun getRegionAt(chunkPos: ChunkPos, y: Int): Region? {
        val regionId = spatialIndex.getRegionAt(chunkPos, y) ?: return null
        return regionCache.get(regionId)
    }

    fun getRegionAtSync(chunkPos: ChunkPos, y: Int): Region? {
        val regionId = spatialIndex.getRegionAt(chunkPos, y) ?: return null
        return regionCache.getSync(regionId)
    }

    suspend fun getRegionsInChunk(chunkPos: ChunkPos): List<Region> =
        regionCache.getAllCached().filter { it.chunkPos == chunkPos }

    suspend fun getRegionsAt(chunkPos: ChunkPos): List<Region> = getRegionsInChunk(chunkPos)

    suspend fun isClaimed(location: BlockLocation): Boolean = getRegionAt(location) != null

    suspend fun isClaimed(chunkPos: ChunkPos): Boolean = getRegionsInChunk(chunkPos).isNotEmpty()

    suspend fun isOverlapping(chunkPos: ChunkPos, y1: Int, y2: Int): Boolean {
        val existingRegions = getRegionsInChunk(chunkPos)
        return existingRegions.any { region ->
            !(y2 < region.y1 || y1 > region.y2)
        }
    }
}
