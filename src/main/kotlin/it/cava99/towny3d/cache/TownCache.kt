package it.cava99.towny3d.cache

import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.repository.TownRepository
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Hot cache for Town data using Caffeine.
 *
 * Features:
 * - Maximum 1,000 entries (towns are less numerous than regions)
 * - 30 minutes TTL after access (towns change less frequently)
 * - Async loading from repository
 *
 * Note: Uses expireAfterAccess only (no refreshAfterWrite) to prevent
 * stale data reloads. The cache loader reads from disk, which may have
 * outdated data if BatchStorageWriter hasn't flushed yet.
 *
 * @param repository The repository for loading towns
 * @param executor The executor for async operations
 */
class TownCache(
    private val repository: TownRepository,
    executor: Executor = Executors.newFixedThreadPool(CacheConstants.TOWN_CACHE_THREAD_POOL_SIZE) { r ->
        Thread(r, "TownCache-Thread-${r.hashCode()}")
    }
) : EntityCache<TownId, Town>(
    repository = repository,
    config = CacheConfig(
        maxSize = CacheConstants.TOWN_CACHE_MAX_SIZE,
        expireAfterAccess = CacheConstants.TOWN_CACHE_EXPIRY_AFTER_ACCESS
    ),
    executor = executor,
    nullSentinel = NULL_TOWN
) {
    /**
     * Gets a town by name.
     * @param name The town name
     * @return The town, or null if not found
     */
    suspend fun getByName(name: String): Town? {
        return repository.findByName(name)?.also { put(it) }
    }

    override fun getEntityId(entity: Town): TownId = entity.id

    companion object {
        // Sentinel value to represent null (uses a special name to satisfy validation)
        private val NULL_TOWN = Town(
            id = TownId.fromString("00000000-0000-0000-0000-000000000000"),
            name = "__NULL_TOWN_SENTINEL__",
            mayorId = ResidentId.fromString("00000000-0000-0000-0000-000000000000")
        )
    }
}
