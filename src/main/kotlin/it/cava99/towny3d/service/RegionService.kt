package it.cava99.towny3d.service

import it.cava99.towny3d.cache.RegionCache
import it.cava99.towny3d.cache.SpatialRegionIndex
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.RegionType
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.region.*
import it.cava99.towny3d.storage.BatchStorageWriter

/**
 * Facade service for managing Region operations.
 *
 * Delegates to focused services for specific responsibilities:
 * - RegionCrudService: Create, Read, Update, Delete operations
 * - RegionIndexService: Spatial index queries and location lookups
 * - PlotService: Plot-specific operations (ownership, sale, toggles, tax)
 *
 * @property regionCache Hot cache for regions
 * @property spatialIndex Spatial index for fast lookups
 * @property townCache Hot cache for towns (for validation)
 * @property batchWriter Batch writer for async storage
 */
class RegionService(
    regionCache: RegionCache,
    spatialIndex: SpatialRegionIndex,
    townCache: TownCache,
    batchWriter: BatchStorageWriter
) {
    // Initialize focused services
    private val crudService = RegionCrudService(regionCache, spatialIndex, townCache, batchWriter)
    private val indexService = RegionIndexService(regionCache, spatialIndex)
    private val plotService = PlotService(regionCache, townCache, crudService)

    // ==================== CRUD Operations (delegated to RegionCrudService) ====================

    suspend fun createRegion(
        townId: TownId,
        chunkPos: ChunkPos,
        y1: Int,
        y2: Int,
        type: RegionType = RegionType.NORMAL,
        ownerId: ResidentId? = null,
        permissions: it.cava99.towny3d.model.PermissionSet = when (type) {
            RegionType.HOME -> it.cava99.towny3d.model.PermissionSet.defaultHome()
            RegionType.EMBASSY -> it.cava99.towny3d.model.PermissionSet.defaultEmbassy()
            RegionType.NORMAL -> it.cava99.towny3d.model.PermissionSet.defaultRegion()
        }
    ): Region = crudService.createRegion(townId, chunkPos, y1, y2, type, ownerId, permissions)

    suspend fun createHomeRegion(townId: TownId, chunkPos: ChunkPos, y1: Int = -64, y2: Int = 320): Region =
        crudService.createHomeRegion(townId, chunkPos, y1, y2)

    suspend fun deleteRegion(regionId: RegionId): Boolean = crudService.deleteRegion(regionId)

    suspend fun getRegion(regionId: RegionId): Region? = crudService.getRegion(regionId)

    suspend fun updateRegion(region: Region) = crudService.updateRegion(region)

    suspend fun deleteRegionsByTown(townId: TownId): Int = crudService.deleteRegionsByTown(townId)

    suspend fun getRegionsByTown(townId: TownId): List<Region> = crudService.getRegionsByTown(townId)

    suspend fun getAllRegions(): List<Region> = crudService.getAllRegions()

    // ==================== Index Operations (delegated to RegionIndexService) ====================

    suspend fun getRegionAt(location: BlockLocation): Region? = indexService.getRegionAt(location)

    suspend fun getRegionAt(chunkPos: ChunkPos, y: Int): Region? = indexService.getRegionAt(chunkPos, y)

    fun getRegionAtSync(chunkPos: ChunkPos, y: Int): Region? = indexService.getRegionAtSync(chunkPos, y)

    suspend fun getRegionsInChunk(chunkPos: ChunkPos): List<Region> = indexService.getRegionsInChunk(chunkPos)

    suspend fun getRegionsAt(chunkPos: ChunkPos): List<Region> = indexService.getRegionsAt(chunkPos)

    suspend fun isClaimed(location: BlockLocation): Boolean = indexService.isClaimed(location)

    suspend fun isClaimed(chunkPos: ChunkPos): Boolean = indexService.isClaimed(chunkPos)

    suspend fun isOverlapping(chunkPos: ChunkPos, y1: Int, y2: Int): Boolean = indexService.isOverlapping(chunkPos, y1, y2)

    // ==================== Plot Operations (delegated to PlotService) ====================

    suspend fun setForSale(regionId: RegionId, forSale: Boolean, price: Double): Region =
        plotService.setForSale(regionId, forSale, price)

    suspend fun setOwner(regionId: RegionId, ownerId: ResidentId): Region =
        plotService.setOwner(regionId, ownerId)

    suspend fun removeOwner(regionId: RegionId): Region = plotService.removeOwner(regionId)

    suspend fun setRegionType(regionId: RegionId, type: RegionType): Region =
        plotService.setRegionType(regionId, type)

    suspend fun setToggleFlags(regionId: RegionId, pvp: Boolean? = null, fire: Boolean? = null, explosion: Boolean? = null): Region =
        plotService.setToggleFlags(regionId, pvp, fire, explosion)

    suspend fun setPlotTax(regionId: RegionId, plotTax: it.cava99.towny3d.model.TaxConfig): Region =
        plotService.setPlotTax(regionId, plotTax)

    suspend fun getRegionsForSale(townId: TownId): List<Region> = plotService.getRegionsForSale(townId)

    suspend fun getRegionsByOwner(ownerId: ResidentId): List<Region> =
        plotService.getRegionsByOwner(ownerId)

    suspend fun countRegionsByTown(townId: TownId): Int = plotService.countRegionsByTown(townId)

    suspend fun calculateTotalLayers(townId: TownId): Int = plotService.calculateTotalLayers(townId)
}
