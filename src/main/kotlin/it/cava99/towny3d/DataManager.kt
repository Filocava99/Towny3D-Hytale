package it.cava99.towny3d

import it.cava99.towny3d.storage.StorageBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Manages data loading from storage into caches.
 * Handles parallel data loading and cache population.
 */
class DataManager(
    private val storageBackend: StorageBackend,
    private val pluginScope: CoroutineScope,
    private val services: ServiceLocator
) {
    /**
     * Loads all data from storage into caches.
     */
    fun loadData(logInfo: (String) -> Unit, logError: (String, Throwable?) -> Unit) {
        logInfo("Loading data from storage...")

        pluginScope.launch {
            try {
                // Load all data in parallel
                val loadRegions = async(Dispatchers.IO) { storageBackend.loadAllRegions() }
                val loadTowns = async(Dispatchers.IO) { storageBackend.loadAllTowns() }
                val loadResidents = async(Dispatchers.IO) { storageBackend.loadAllResidents() }
                val loadNations = async(Dispatchers.IO) { storageBackend.loadAllNations() }

                // Wait for all loads to complete
                val regions = loadRegions.await()
                val towns = loadTowns.await()
                val residents = loadResidents.await()
                val nations = loadNations.await()

                // Populate caches
                regions.forEach { services.regionCache.put(it) }
                towns.forEach { services.townCache.put(it) }
                residents.forEach { services.residentCache.put(it) }
                nations.forEach { services.nationCache.put(it) }

                // Rebuild spatial cache with loaded regions
                services.spatialIndex.rebuild(regions)

                logInfo("Data loading complete! Loaded ${towns.size} towns, ${nations.size} nations, ${regions.size} regions")
            } catch (e: Exception) {
                logError("Failed to load data", e)
            }
        }
    }
}
