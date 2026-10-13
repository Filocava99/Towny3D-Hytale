package it.cava99.towny3d.service.region

import it.cava99.towny3d.cache.RegionCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionType
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.RuinRecoveryService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles plot-specific operations (ownership, sale, toggles, tax).
 */
class PlotService(
    private val regionCache: RegionCache,
    private val townCache: TownCache,
    private val crudService: RegionCrudService
) {
    private val mutex = Mutex()

    suspend fun setForSale(regionId: RegionId, forSale: Boolean, price: Double): Region = mutex.withLock {
        val region = regionCache.get(regionId)
            ?: throw IllegalStateException("Region $regionId does not exist")

        val town = townCache.get(region.townId)
            ?: throw IllegalStateException("Town ${region.townId} does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "manage regions")

        val updated = region.copy(
            forSale = forSale,
            salePrice = if (forSale) price else 0.0
        )

        crudService.saveRegion(updated)
        updated
    }

    suspend fun setOwner(regionId: RegionId, ownerId: ResidentId): Region = mutex.withLock {
        val region = regionCache.get(regionId)
            ?: throw IllegalStateException("Region $regionId does not exist")

        val town = townCache.get(region.townId)
            ?: throw IllegalStateException("Town ${region.townId} does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "manage regions")

        val updated = region.copy(ownerId = ownerId)
        crudService.saveRegion(updated)
        updated
    }

    suspend fun removeOwner(regionId: RegionId): Region = mutex.withLock {
        val region = regionCache.get(regionId)
            ?: throw IllegalStateException("Region $regionId does not exist")

        val town = townCache.get(region.townId)
            ?: throw IllegalStateException("Town ${region.townId} does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "manage regions")

        val updated = region.copy(ownerId = null)
        crudService.saveRegion(updated)
        updated
    }

    suspend fun setRegionType(regionId: RegionId, type: RegionType): Region = mutex.withLock {
        val region = regionCache.get(regionId)
            ?: throw IllegalStateException("Region $regionId does not exist")

        val town = townCache.get(region.townId)
            ?: throw IllegalStateException("Town ${region.townId} does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "change region type")

        val updated = region.copy(type = type)
        crudService.saveRegion(updated)
        updated
    }

    suspend fun setToggleFlags(
        regionId: RegionId,
        pvp: Boolean? = null,
        fire: Boolean? = null,
        explosion: Boolean? = null
    ): Region = mutex.withLock {
        val region = regionCache.get(regionId)
            ?: throw IllegalStateException("Region $regionId does not exist")

        val town = townCache.get(region.townId)
            ?: throw IllegalStateException("Town ${region.townId} does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "change region settings")

        val currentToggles = region.toggles
        val updated = region.copy(
            toggles = it.cava99.towny3d.model.ToggleFlags(
                pvp = pvp ?: currentToggles.pvp,
                fire = fire ?: currentToggles.fire,
                explosion = explosion ?: currentToggles.explosion
            )
        )
        crudService.saveRegion(updated)
        updated
    }

    suspend fun setPlotTax(
        regionId: RegionId,
        plotTax: it.cava99.towny3d.model.TaxConfig
    ): Region = mutex.withLock {
        val region = regionCache.get(regionId)
            ?: throw IllegalStateException("Region $regionId does not exist")

        val updated = region.copy(plotTax = plotTax)
        crudService.saveRegion(updated)
        updated
    }

    suspend fun getRegionsForSale(townId: TownId): List<Region> =
        crudService.getRegionsByTown(townId).filter { it.forSale }

    suspend fun getRegionsByOwner(ownerId: ResidentId): List<Region> =
        regionCache.getAllCached().filter { it.ownerId == ownerId }

    suspend fun countRegionsByTown(townId: TownId): Int = crudService.getRegionsByTown(townId).size

    suspend fun calculateTotalLayers(townId: TownId): Int =
        crudService.getRegionsByTown(townId).sumOf { it.height }
}
