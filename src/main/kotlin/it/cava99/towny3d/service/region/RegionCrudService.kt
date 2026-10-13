package it.cava99.towny3d.service.region

import it.cava99.towny3d.cache.RegionCache
import it.cava99.towny3d.cache.SpatialRegionIndex
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionType
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.RuinRecoveryService
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles basic Region CRUD operations (Create, Read, Update, Delete).
 */
class RegionCrudService(
    private val regionCache: RegionCache,
    private val spatialIndex: SpatialRegionIndex,
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter
) {
    private val mutex = Mutex()

    suspend fun createRegion(
        townId: TownId,
        chunkPos: ChunkPos,
        y1: Int,
        y2: Int,
        type: RegionType = RegionType.NORMAL,
        ownerId: it.cava99.towny3d.model.ResidentId? = null,
        permissions: it.cava99.towny3d.model.PermissionSet = when (type) {
            RegionType.HOME -> it.cava99.towny3d.model.PermissionSet.defaultHome()
            RegionType.EMBASSY -> it.cava99.towny3d.model.PermissionSet.defaultEmbassy()
            RegionType.NORMAL -> it.cava99.towny3d.model.PermissionSet.defaultRegion()
        }
    ): Region = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "claim new land")

        require(y1 >= -512 && y1 <= 512) { "y1 must be between -512 and 512, got $y1" }
        require(y2 >= -512 && y2 <= 512) { "y2 must be between -512 and 512, got $y2" }
        require(y1 <= y2) { "y1 must be <= y2, got y1=$y1, y2=$y2" }

        val height = y2 - y1 + 1
        if (town.layers + height > town.maxLayers) {
            throw IllegalStateException("Town has insufficient layers. Current: ${town.layers}, needed: $height, max: ${town.maxLayers}")
        }

        val region = Region(
            id = RegionId.random(),
            chunkPos = chunkPos,
            y1 = y1,
            y2 = y2,
            townId = townId,
            ownerId = ownerId,
            type = type,
            permissions = permissions
        )

        saveRegion(region)

        // Update town layers count
        val updatedTown = town.withModification(layers = town.layers + height)
        townCache.put(updatedTown)
        batchWriter.saveTown(updatedTown)

        region
    }

    suspend fun createHomeRegion(
        townId: TownId,
        chunkPos: ChunkPos,
        y1: Int = -64,
        y2: Int = 320
    ): Region = mutex.withLock {
        val region = Region.createHome(townId, chunkPos, y1, y2)
        saveRegion(region)
        region
    }

    suspend fun deleteRegion(regionId: RegionId): Boolean = mutex.withLock {
        val region = regionCache.get(regionId) ?: return false

        val chunkX = region.chunkPos.x
        val chunkZ = region.chunkPos.z
        val height = region.y2 - region.y1 + 1

        regionCache.invalidate(regionId)
        batchWriter.deleteRegion(regionId)

        // Update town layers count
        val town = townCache.get(region.townId)
        if (town != null) {
            val updatedTown = town.withModification(layers = maxOf(0, town.layers - height))
            townCache.put(updatedTown)
            batchWriter.saveTown(updatedTown)
        }

        // Use getAllCached() to read from in-memory cache, not disk
        val allRegions = regionCache.getAllCached()
        spatialIndex.rebuild(allRegions)

        it.cava99.towny3d.map.TownyWorldMapProvider.markChunkForRefresh(chunkX, chunkZ)
        true
    }

    suspend fun getRegion(regionId: RegionId): Region? = regionCache.get(regionId)

    suspend fun updateRegion(region: Region) = mutex.withLock {
        val existing = regionCache.get(region.id)
            ?: throw IllegalStateException("Region ${region.id} does not exist")

        require(existing.townId == region.townId) {
            "Cannot change region town from ${existing.townId} to ${region.townId}"
        }

        saveRegion(region)
    }

    suspend fun deleteRegionsByTown(townId: TownId): Int = mutex.withLock {
        val regions = getRegionsByTown(townId)
        regions.forEach { region ->
            regionCache.invalidate(region.id)
            batchWriter.deleteRegion(region.id)
            spatialIndex.remove(region)
        }
        regions.size
    }

    suspend fun saveRegion(region: Region) {
        regionCache.put(region)
        batchWriter.saveRegion(region)

        // Use getAllCached() to read from in-memory cache, not disk.
        // getAll() reads from repository which may not have the new region yet
        // (batch writer hasn't flushed).
        val allRegions = regionCache.getAllCached()
        spatialIndex.rebuild(allRegions)

        it.cava99.towny3d.map.TownyWorldMapProvider.markChunkForRefresh(region.chunkPos.x, region.chunkPos.z)
    }

    fun getRegionsByTown(townId: TownId): List<Region> =
        regionCache.getAllCached().filter { it.townId == townId }

    fun getAllRegions(): List<Region> = regionCache.getAllCached()
}
