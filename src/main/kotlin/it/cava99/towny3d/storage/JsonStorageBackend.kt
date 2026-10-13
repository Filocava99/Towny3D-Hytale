package it.cava99.towny3d.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.io.path.createDirectories

/**
 * JSON file-based implementation of StorageBackend.
 *
 * Data is organized as:
 * data/
 * ├── regions/
 * │   └── {regionId}.json
 * ├── towns/
 * │   └── {townId}.json
 * ├── residents/
 * │   └── {residentId}.json
 * └── nations/
 *     └── {nationId}.json
 *
 * @param dataDir The root directory for data storage
 * @param gson The Gson instance to use for serialization
 * @param config Plugin configuration
 */
class JsonStorageBackend(
    private val dataDir: Path,
    private val gson: Gson = createDefaultGson(),
    private val config: TownyConfig? = null
) : StorageBackend {

    private val regionsDir = dataDir.resolve("regions")
    private val townsDir = dataDir.resolve("towns")
    private val residentsDir = dataDir.resolve("residents")
    private val nationsDir = dataDir.resolve("nations")

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        // Create directories if they don't exist
        regionsDir.createDirectories()
        townsDir.createDirectories()
        residentsDir.createDirectories()
        nationsDir.createDirectories()
        Unit
    }

    override suspend fun shutdown() = withContext(Dispatchers.IO) {
        // Nothing to clean up for JSON storage
    }

    // ========== Region Operations ==========

    override suspend fun loadRegion(id: RegionId): Region? {
        return StorageIo.loadJson(regionsDir, id.value.toString(), gson, Region::class.java)
    }

    override suspend fun saveRegion(region: Region) {
        StorageIo.saveJson(regionsDir, region.id.value.toString(), region, gson)
    }

    override suspend fun deleteRegion(id: RegionId) {
        StorageIo.deleteJson(regionsDir, id.value.toString())
    }

    override suspend fun loadAllRegions(): List<Region> {
        return StorageIo.loadAllJson(regionsDir, gson, Region::class.java)
    }

    // ========== Town Operations ==========

    override suspend fun loadTown(id: TownId): Town? {
        return StorageIo.loadJson(townsDir, id.value.toString(), gson, Town::class.java)
    }

    override suspend fun saveTown(town: Town) {
        StorageIo.saveJson(townsDir, town.id.value.toString(), town, gson)
    }

    override suspend fun deleteTown(id: TownId) {
        StorageIo.deleteJson(townsDir, id.value.toString())
    }

    override suspend fun loadAllTowns(): List<Town> {
        val towns = StorageIo.loadAllJson(townsDir, gson, Town::class.java)

        // Migrate old maxLayers values (384 or 256) to new formula: 256 + (residentCount × layersPerPlayer)
        val migratedTowns = towns.map { town ->
            val shouldMigrate = town.maxLayers == 384 || town.maxLayers == 256
            if (shouldMigrate) {
                // Use config if available, otherwise use default formula
                val newMaxLayers = config?.calculateMaxLayers(town.residentCount)
                    ?: (256 + town.residentCount * 3840)
                town.copy(maxLayers = newMaxLayers)
            } else {
                town
            }
        }

        // Save migrated towns back to disk
        migratedTowns.filter { newTown ->
            val originalTown = towns.find { it.id == newTown.id }
            originalTown != null && originalTown.maxLayers != newTown.maxLayers
        }.forEach { saveTown(it) }

        return migratedTowns
    }

    override suspend fun findTownByName(name: String): Town? {
        return loadAllTowns().find { it.name.equals(name, ignoreCase = true) }
    }

    // ========== Resident Operations ==========

    override suspend fun loadResident(id: ResidentId): Resident? {
        return StorageIo.loadJson(residentsDir, id.value.toString(), gson, Resident::class.java)
    }

    override suspend fun saveResident(resident: Resident) {
        StorageIo.saveJson(residentsDir, resident.id.value.toString(), resident, gson)
    }

    override suspend fun deleteResident(id: ResidentId) {
        StorageIo.deleteJson(residentsDir, id.value.toString())
    }

    override suspend fun loadAllResidents(): List<Resident> {
        return StorageIo.loadAllJson(residentsDir, gson, Resident::class.java)
    }

    override suspend fun findResidentByUuid(uuid: UUID): Resident? {
        return StorageIo.loadJson(residentsDir, uuid.toString(), gson, Resident::class.java)
    }

    override suspend fun findResidentByName(name: String): Resident? {
        return loadAllResidents().find { it.name.equals(name, ignoreCase = true) }
    }

    // ========== Nation Operations ==========

    override suspend fun loadNation(id: NationId): Nation? {
        return StorageIo.loadJson(nationsDir, id.value.toString(), gson, Nation::class.java)
    }

    override suspend fun saveNation(nation: Nation) {
        StorageIo.saveJson(nationsDir, nation.id.value.toString(), nation, gson)
    }

    override suspend fun deleteNation(id: NationId) {
        StorageIo.deleteJson(nationsDir, id.value.toString())
    }

    override suspend fun loadAllNations(): List<Nation> {
        return StorageIo.loadAllJson(nationsDir, gson, Nation::class.java)
    }

    override suspend fun findNationByName(name: String): Nation? {
        return loadAllNations().find { it.name.equals(name, ignoreCase = true) }
    }

    companion object {
        /**
         * Creates the default Gson instance with proper adapters.
         */
        fun createDefaultGson(): Gson {
            return GsonBuilder()
                .setPrettyPrinting()
                // Add custom type adapters for inline value classes
                .registerTypeAdapter(RegionId::class.java, RegionIdAdapter())
                .registerTypeAdapter(TownId::class.java, TownIdAdapter())
                .registerTypeAdapter(NationId::class.java, NationIdAdapter())
                .registerTypeAdapter(ResidentId::class.java, ResidentIdAdapter())
                .registerTypeAdapter(ChunkPos::class.java, ChunkPosAdapter())
                // Add Instant adapter for timestamps
                .registerTypeAdapter(Instant::class.java, InstantAdapter())
                // Add UUID adapter
                .registerTypeAdapter(UUID::class.java, UUIDAdapter())
                // Add ChatMode adapter
                .registerTypeAdapter(it.cava99.towny3d.model.ChatMode::class.java, ChatModeAdapter())
                // Add Resident adapter to ensure balance is saved
                .registerTypeAdapter(Resident::class.java, ResidentAdapter())
                // Add TaxConfig adapter for proper enum serialization
                .registerTypeAdapter(it.cava99.towny3d.model.TaxConfig::class.java, TaxConfigAdapter())
                // Add Town adapter to ensure isOpen, isPublic, isRuined are saved
                .registerTypeAdapter(it.cava99.towny3d.model.Town::class.java, TownAdapter())
                .create()
        }
    }
}
