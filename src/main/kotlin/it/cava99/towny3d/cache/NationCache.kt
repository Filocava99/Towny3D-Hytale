package it.cava99.towny3d.cache

import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.repository.NationRepository
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Hot cache for Nation data using Caffeine.
 *
 * Features:
 * - Maximum 100 entries (nations are the least numerous)
 * - 1 hour TTL after write (nations change very infrequently)
 * - 15 minutes refresh after write
 * - Async loading from repository
 *
 * @param repository The repository for loading nations
 * @param executor The executor for async operations
 */
class NationCache(
    repository: NationRepository,
    executor: Executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "NationCache-Thread")
    }
) : EntityCache<NationId, Nation>(
    repository = repository,
    config = CacheConfig(
        maxSize = CacheConstants.NATION_CACHE_MAX_SIZE,
        expireAfterWrite = CacheConstants.NATION_CACHE_EXPIRY_AFTER_WRITE,
        refreshAfterWrite = CacheConstants.NATION_CACHE_REFRESH_AFTER_WRITE
    ),
    executor = executor,
    nullSentinel = NULL_NATION
) {
    private val nationRepository = repository

    /**
     * Gets a nation by name.
     */
    suspend fun getByName(name: String): Nation? {
        return nationRepository.findByName(name)?.also { put(it) }
    }

    override fun getEntityId(entity: Nation): NationId = entity.id

    companion object {
        // Sentinel value to represent null (uses a special name to satisfy validation)
        private val NULL_NATION = Nation(
            id = NationId.fromString("00000000-0000-0000-0000-000000000000"),
            name = "__NULL_NATION_SENTINEL__",
            kingId = it.cava99.towny3d.model.ResidentId.fromString("00000000-0000-0000-0000-000000000000"),
            capitalId = it.cava99.towny3d.model.TownId.fromString("00000000-0000-0000-0000-000000000000"),
            townIds = setOf(it.cava99.towny3d.model.TownId.fromString("00000000-0000-0000-0000-000000000000"))
        )
    }
}
