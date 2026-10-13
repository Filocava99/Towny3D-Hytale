package it.cava99.towny3d.cache

import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.repository.RegionRepository
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Hot cache for Region data using Caffeine.
 *
 * Features:
 * - Maximum 10,000 entries
 * - 10 minutes TTL after access
 * - 5 minutes refresh after write
 * - Async loading from repository
 *
 * @param repository The repository for loading regions
 * @param executor The executor for async operations
 */
class RegionCache(
    repository: RegionRepository,
    executor: Executor = Executors.newFixedThreadPool(CacheConstants.REGION_CACHE_THREAD_POOL_SIZE) { r ->
        Thread(r, "RegionCache-Thread-${r.hashCode()}")
    }
) : EntityCache<RegionId, Region>(
    repository = repository,
    config = CacheConfig(
        maxSize = CacheConstants.REGION_CACHE_MAX_SIZE,
        expireAfterAccess = CacheConstants.REGION_CACHE_EXPIRY_AFTER_ACCESS,
        refreshAfterWrite = CacheConstants.REGION_CACHE_REFRESH_AFTER_WRITE
    ),
    executor = executor,
    nullSentinel = NULL_REGION
) {
    override fun getEntityId(entity: Region): RegionId = entity.id

    companion object {
        // Sentinel value to represent null in Caffeine (which doesn't support null values)
        private val NULL_REGION = Region(
            id = RegionId.fromString("00000000-0000-0000-0000-000000000000"),
            chunkPos = it.cava99.towny3d.model.ChunkPos(0, 0),
            y1 = 0,
            y2 = 0,
            townId = it.cava99.towny3d.model.TownId.fromString("00000000-0000-0000-0000-000000000000")
        )
    }
}
