package it.cava99.towny3d.cache

import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.repository.ResidentRepository
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Hot cache for Resident data using Caffeine.
 *
 * Features:
 * - Maximum 10,000 entries (supports many online players)
 * - 15 minutes TTL after access
 * - NO refresh after write (prevents stale data reloads before BatchStorageWriter flushes)
 * - Async loading from repository
 * - Index by name for fast lookups
 *
 * @param repository The repository for loading residents
 * @param executor The executor for async operations
 */
class ResidentCache(
    repository: ResidentRepository,
    executor: Executor = Executors.newFixedThreadPool(CacheConstants.RESIDENT_CACHE_THREAD_POOL_SIZE) { r ->
        Thread(r, "ResidentCache-Thread-${r.hashCode()}")
    }
) : EntityCache<ResidentId, Resident>(
    repository = repository,
    config = CacheConfig(
        maxSize = CacheConstants.RESIDENT_CACHE_MAX_SIZE,
        expireAfterAccess = CacheConstants.RESIDENT_CACHE_EXPIRY_AFTER_ACCESS
    ),
    executor = executor,
    nullSentinel = NULL_RESIDENT
) {
    private val residentRepository = repository
    private val nameIndex: MutableMap<String, ResidentId> = mutableMapOf()

    /**
     * Gets a resident by UUID.
     */
    suspend fun getByUuid(uuid: UUID): Resident? {
        val id = ResidentId.fromPlayerUuid(uuid)
        return get(id)
    }

    /**
     * Gets a resident by name using the name index.
     */
    suspend fun getByName(name: String): Resident? {
        val id = nameIndex[name.lowercase()] ?: return residentRepository.findByName(name)?.also { put(it) }
        return get(id)
    }

    /**
     * Puts a resident into the cache and updates name index.
     */
    override fun put(entity: Resident) {
        super.put(entity)
        nameIndex[entity.name.lowercase()] = entity.id
    }

    /**
     * Puts multiple residents into the cache and updates name index.
     */
    override fun putAll(entities: Collection<Resident>) {
        super.putAll(entities)
        entities.forEach { nameIndex[it.name.lowercase()] = it.id }
    }

    /**
     * Invalidates a resident from the cache and removes from name index.
     */
    override suspend fun invalidate(id: ResidentId) {
        val resident = getSync(id)
        if (resident != null) {
            nameIndex.remove(resident.name.lowercase())
        }
        super.invalidate(id)
    }

    /**
     * Invalidates all residents from the cache and clears name index.
     */
    override fun invalidateAll() {
        nameIndex.clear()
        super.invalidateAll()
    }

    override fun getEntityId(entity: Resident): ResidentId = entity.id

    companion object {
        // Sentinel value to represent null (uses a special name to satisfy validation)
        private val NULL_RESIDENT = Resident(
            id = ResidentId.fromString("00000000-0000-0000-0000-000000000000"),
            name = "__NULL_RESIDENT_SENTINEL__"
        )
    }
}
