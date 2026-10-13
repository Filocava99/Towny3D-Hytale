package it.cava99.towny3d.repository

import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.storage.StorageBackend

/**
 * Repository for Town entity persistence operations.
 * Provides a clean abstraction layer over StorageBackend for Town data.
 *
 * @param storage The underlying storage backend
 */
class TownRepository(private val storage: StorageBackend) : EntityRepository<TownId, Town> {

    override suspend fun findById(id: TownId): Town? = storage.loadTown(id)

    override suspend fun save(entity: Town) = storage.saveTown(entity)

    override suspend fun delete(id: TownId) = storage.deleteTown(id)

    override suspend fun findAll(): List<Town> = storage.loadAllTowns()

    /**
     * Finds a town by its name.
     * @param name The town name to search for
     * @return The town, or null if not found
     */
    suspend fun findByName(name: String): Town? = storage.findTownByName(name)
}
