package it.cava99.towny3d.repository

import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.storage.StorageBackend

/**
 * Repository for Region entity persistence operations.
 * Provides a clean abstraction layer over StorageBackend for Region data.
 *
 * @param storage The underlying storage backend
 */
class RegionRepository(private val storage: StorageBackend) : EntityRepository<RegionId, Region> {

    override suspend fun findById(id: RegionId): Region? = storage.loadRegion(id)

    override suspend fun save(entity: Region) = storage.saveRegion(entity)

    override suspend fun delete(id: RegionId) = storage.deleteRegion(id)

    override suspend fun findAll(): List<Region> = storage.loadAllRegions()

    /**
     * Saves multiple regions in a single batch operation.
     * @param regions The regions to save
     */
    suspend fun saveAll(regions: List<Region>) = storage.saveRegions(regions)
}
