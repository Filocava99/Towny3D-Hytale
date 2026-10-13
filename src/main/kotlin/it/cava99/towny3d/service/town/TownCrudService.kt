package it.cava99.towny3d.service.town

import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.RegionService
import it.cava99.towny3d.service.ResidentService
import it.cava99.towny3d.service.RuinRecoveryService
import it.cava99.towny3d.storage.BatchStorageWriter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles basic Town CRUD operations (Create, Read, Update, Delete).
 */
class TownCrudService(
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter,
    private val regionService: RegionService,
    private val residentService: ResidentService,
    private val config: TownyConfig
) {
    private val mutex = Mutex()

    /**
     * Creates a new town.
     */
    suspend fun createTown(
        name: String,
        mayorId: ResidentId,
        homeLocation: BlockLocation,
        initialBalance: Double = 0.0,
        initialLayers: Int = 384,
        maxLayers: Int = config.calculateMaxLayers(1)
    ): Town = mutex.withLock {
        if (townCache.getByName(name) != null) {
            throw IllegalStateException("Town '$name' already exists")
        }

        val town = Town.create(
            name = name,
            mayorId = mayorId,
            homeLocation = homeLocation,
            initialBalance = initialBalance,
            initialLayers = initialLayers,
            maxLayers = maxLayers
        )

        saveTown(town)

        val chunkX = homeLocation.chunkPos.x
        val chunkZ = homeLocation.chunkPos.z
        regionService.createHomeRegion(
            townId = town.id,
            chunkPos = it.cava99.towny3d.model.ChunkPos(chunkX, chunkZ),
            y1 = -64,
            y2 = 320
        )

        TownyLog.info("Created home region for town '$name' at chunk ($chunkX, $chunkZ)")
        town
    }

    /**
     * Deletes a town and cleans up all associated data.
     */
    suspend fun deleteTown(townId: TownId): Boolean = mutex.withLock {
        val town = townCache.get(townId) ?: return false

        val deletedRegions = regionService.deleteRegionsByTown(townId)
        TownyLog.info("Deleted $deletedRegions regions for town '${town.name}'")

        val residents = residentService.getResidentsByTown(townId)
        residents.forEach { resident ->
            residentService.setTown(resident.id, null)
        }
        TownyLog.info("Removed town membership from ${residents.size} residents")

        townCache.invalidate(townId)
        batchWriter.deleteTown(townId)

        TownyLog.info("Deleted town '${town.name}'")
        true
    }

    suspend fun getTown(townId: TownId): Town? = townCache.get(townId)

    suspend fun getTownByName(name: String): Town? = townCache.getByName(name)

    fun getAllTowns(): List<Town> = townCache.getAllCached()

    suspend fun saveTown(town: Town) {
        townCache.put(town)
        batchWriter.saveTown(town)
    }
}
