package it.cava99.towny3d.repository

import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.storage.StorageBackend
import java.util.UUID

/**
 * Repository for Resident entity persistence operations.
 * Provides a clean abstraction layer over StorageBackend for Resident data.
 *
 * @param storage The underlying storage backend
 */
class ResidentRepository(private val storage: StorageBackend) : EntityRepository<ResidentId, Resident> {

    override suspend fun findById(id: ResidentId): Resident? = storage.loadResident(id)

    override suspend fun save(entity: Resident) = storage.saveResident(entity)

    override suspend fun delete(id: ResidentId) = storage.deleteResident(id)

    override suspend fun findAll(): List<Resident> = storage.loadAllResidents()

    /**
     * Finds a resident by their player UUID.
     * @param uuid The player's UUID
     * @return The resident, or null if not found
     */
    suspend fun findByUuid(uuid: UUID): Resident? = storage.findResidentByUuid(uuid)

    /**
     * Finds a resident by their name.
     * @param name The resident name to search for
     * @return The resident, or null if not found
     */
    suspend fun findByName(name: String): Resident? = storage.findResidentByName(name)
}
