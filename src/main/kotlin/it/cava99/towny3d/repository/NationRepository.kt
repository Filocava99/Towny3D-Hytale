package it.cava99.towny3d.repository

import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.storage.StorageBackend

/**
 * Repository for Nation entity persistence operations.
 * Provides a clean abstraction layer over StorageBackend for Nation data.
 *
 * @param storage The underlying storage backend
 */
class NationRepository(private val storage: StorageBackend) : EntityRepository<NationId, Nation> {

    override suspend fun findById(id: NationId): Nation? = storage.loadNation(id)

    override suspend fun save(entity: Nation) = storage.saveNation(entity)

    override suspend fun delete(id: NationId) = storage.deleteNation(id)

    override suspend fun findAll(): List<Nation> = storage.loadAllNations()

    /**
     * Finds a nation by its name.
     * @param name The nation name to search for
     * @return The nation, or null if not found
     */
    suspend fun findByName(name: String): Nation? = storage.findNationByName(name)
}
