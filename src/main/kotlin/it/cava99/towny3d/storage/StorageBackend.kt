package it.cava99.towny3d.storage

import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId

/**
 * Interface for persistent storage of Towny data.
 * Implementations can use JSON files, SQLite, or other storage mechanisms.
 *
 * All operations are suspend functions to support async I/O.
 */
interface StorageBackend {
    // ========== Region Operations ==========

    /**
     * Loads a region by ID.
     * @return The region, or null if not found
     */
    suspend fun loadRegion(id: RegionId): Region?

    /**
     * Saves a region to storage.
     */
    suspend fun saveRegion(region: Region)

    /**
     * Deletes a region from storage.
     */
    suspend fun deleteRegion(id: RegionId)

    /**
     * Loads all regions.
     * @return List of all regions (empty list if none exist)
     */
    suspend fun loadAllRegions(): List<Region>

    // ========== Town Operations ==========

    /**
     * Loads a town by ID.
     * @return The town, or null if not found
     */
    suspend fun loadTown(id: TownId): Town?

    /**
     * Saves a town to storage.
     */
    suspend fun saveTown(town: Town)

    /**
     * Deletes a town from storage.
     */
    suspend fun deleteTown(id: TownId)

    /**
     * Loads all towns.
     * @return List of all towns (empty list if none exist)
     */
    suspend fun loadAllTowns(): List<Town>

    /**
     * Finds a town by name.
     * @return The town, or null if not found
     */
    suspend fun findTownByName(name: String): Town?

    // ========== Resident Operations ==========

    /**
     * Loads a resident by ID.
     * @return The resident, or null if not found
     */
    suspend fun loadResident(id: ResidentId): Resident?

    /**
     * Saves a resident to storage.
     */
    suspend fun saveResident(resident: Resident)

    /**
     * Deletes a resident from storage.
     */
    suspend fun deleteResident(id: ResidentId)

    /**
     * Loads all residents.
     * @return List of all residents (empty list if none exist)
     */
    suspend fun loadAllResidents(): List<Resident>

    /**
     * Finds a resident by UUID.
     * @return The resident, or null if not found
     */
    suspend fun findResidentByUuid(uuid: java.util.UUID): Resident?

    /**
     * Finds a resident by name.
     * @return The resident, or null if not found
     */
    suspend fun findResidentByName(name: String): Resident?

    // ========== Nation Operations ==========

    /**
     * Loads a nation by ID.
     * @return The nation, or null if not found
     */
    suspend fun loadNation(id: NationId): Nation?

    /**
     * Saves a nation to storage.
     */
    suspend fun saveNation(nation: Nation)

    /**
     * Deletes a nation from storage.
     */
    suspend fun deleteNation(id: NationId)

    /**
     * Loads all nations.
     * @return List of all nations (empty list if none exist)
     */
    suspend fun loadAllNations(): List<Nation>

    /**
     * Finds a nation by name.
     * @return The nation, or null if not found
     */
    suspend fun findNationByName(name: String): Nation?

    // ========== Bulk Operations ==========

    /**
     * Saves multiple regions in a single batch operation.
     */
    suspend fun saveRegions(regions: List<Region>) {
        regions.forEach { saveRegion(it) }
    }

    /**
     * Saves multiple towns in a single batch operation.
     */
    suspend fun saveTowns(towns: List<Town>) {
        towns.forEach { saveTown(it) }
    }

    /**
     * Saves multiple residents in a single batch operation.
     */
    suspend fun saveResidents(residents: List<Resident>) {
        residents.forEach { saveResident(it) }
    }

    /**
     * Saves multiple nations in a single batch operation.
     */
    suspend fun saveNations(nations: List<Nation>) {
        nations.forEach { saveNation(it) }
    }

    // ========== Maintenance ==========

    /**
     * Initializes the storage backend.
     * Called when the plugin starts.
     */
    suspend fun initialize()

    /**
     * Shuts down the storage backend.
     * Called when the plugin stops.
     */
    suspend fun shutdown()

    /**
     * Performs any necessary maintenance (e.g., compaction, backup).
     */
    suspend fun maintenance() {}
}
