package it.cava99.towny3d

import it.cava99.towny3d.cache.*
import it.cava99.towny3d.repository.*
import it.cava99.towny3d.storage.StorageBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages cache initialization and warmup for the plugin.
 * Handles creating and configuring all entity caches with their repositories.
 */
class CacheManager(
    private val storageBackend: StorageBackend,
    private val pluginScope: CoroutineScope,
    private val services: ServiceLocator
) {
    /**
     * Initializes all caches with their repositories.
     */
    fun initializeCaches() {
        val executor = java.util.concurrent.Executors.newFixedThreadPool(CacheConstants.DEFAULT_CACHE_THREAD_POOL_SIZE) { r ->
            java.lang.Thread(r, "TownyCache-Thread-${r.hashCode()}")
        }

        // Create repositories
        val townRepository = TownRepository(storageBackend)
        val nationRepository = NationRepository(storageBackend)
        val regionRepository = RegionRepository(storageBackend)
        val residentRepository = ResidentRepository(storageBackend)

        // Initialize caches with repositories
        services.regionCache = RegionCache(regionRepository, executor)
        services.townCache = TownCache(townRepository, executor)
        services.residentCache = ResidentCache(residentRepository, executor)
        services.nationCache = NationCache(nationRepository, executor)
        services.spatialIndex = SpatialRegionIndex()
    }

    /**
     * Warms up all caches by pre-loading data from storage.
     */
    fun warmUpCaches(logInfo: (String) -> Unit, logError: (String, Throwable?) -> Unit) {
        pluginScope.launch {
            try {
                services.townCache.warmUp()
                services.nationCache.warmUp()
                services.regionCache.warmUp()
                services.residentCache.warmUp()
                logInfo("Cache warm-up complete")
            } catch (e: Exception) {
                logError("Cache warm-up failed", e)
            }
        }
    }

    /**
     * Clears all caches.
     */
    suspend fun clearAllCaches() {
        services.regionCache.invalidateAll()
        services.townCache.invalidateAll()
        services.residentCache.invalidateAll()
        services.nationCache.invalidateAll()
        services.spatialIndex.clear()
    }
}
