package it.cava99.towny3d.service.nation

import it.cava99.towny3d.cache.NationCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.service.RuinRecoveryService
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles basic Nation CRUD operations (Create, Read, Update, Delete).
 */
class NationCrudService(
    private val nationCache: NationCache,
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter
) {
    private val mutex = Mutex()

    suspend fun createNation(
        name: String,
        kingId: ResidentId,
        capitalTownId: TownId
    ): Nation = mutex.withLock {
        if (nationCache.getByName(name) != null) {
            throw IllegalStateException("Nation '$name' already exists")
        }

        val capital = townCache.get(capitalTownId)
            ?: throw IllegalStateException("Capital town $capitalTownId does not exist")

        if (capital.hasNation) {
            throw IllegalStateException("Capital town already belongs to a nation")
        }

        val nation = Nation.create(name, kingId, capitalTownId)
        saveNation(nation)
        nation
    }

    suspend fun deleteNation(nationId: NationId): Boolean = mutex.withLock {
        val nation = nationCache.get(nationId) ?: return false
        nationCache.invalidate(nationId)
        batchWriter.deleteNation(nationId)
        true
    }

    suspend fun getNation(nationId: NationId): Nation? = nationCache.get(nationId)

    suspend fun getNationByName(name: String): Nation? = nationCache.getByName(name)

    fun getAllNations(): List<Nation> = nationCache.getAllCached()

    suspend fun saveNation(nation: Nation) {
        nationCache.put(nation)
        batchWriter.saveNation(nation)
    }

    suspend fun addTown(nationId: NationId, townId: TownId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        RuinRecoveryService.requireNationNotRuined(nation, "accept new towns")

        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "join a nation")

        if (town.hasNation) {
            throw IllegalStateException("Town already belongs to a nation")
        }

        val updated = nation.withModification(townIds = nation.townIds + townId)
        saveNation(updated)
        updated
    }

    suspend fun removeTown(nationId: NationId, townId: TownId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        if (townId == nation.capitalId) {
            throw IllegalStateException("Cannot remove capital town from nation")
        }

        val updated = nation.withModification(townIds = nation.townIds - townId)
        saveNation(updated)
        updated
    }

    suspend fun getTownsInNation(nationId: NationId): List<Town> {
        val nation = nationCache.get(nationId) ?: return emptyList()
        return nation.townIds.mapNotNull { townCache.get(it) }
    }
}
