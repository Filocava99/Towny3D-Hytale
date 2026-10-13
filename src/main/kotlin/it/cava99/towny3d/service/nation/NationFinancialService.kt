package it.cava99.towny3d.service.nation

import it.cava99.towny3d.cache.NationCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.service.RuinRecoveryService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles Nation financial and diplomatic operations (balance, taxes, alliances).
 */
class NationFinancialService(
    private val nationCache: NationCache,
    private val townCache: TownCache,
    private val crudService: NationCrudService
) {
    private val mutex = Mutex()

    // Financial operations
    suspend fun setTaxToTowns(nationId: NationId, taxConfig: TaxConfig) {
        val nation = crudService.getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val updated = nation.withModification(taxToTowns = taxConfig)
        crudService.saveNation(updated)
    }

    suspend fun setBalance(nationId: NationId, balance: Double) {
        val nation = crudService.getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val updated = nation.withModification(balance = balance)
        crudService.saveNation(updated)
    }

    suspend fun deposit(nationId: NationId, amount: Double) {
        val nation = crudService.getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val newBalance = nation.balance + amount
        val updated = nation.withModification(balance = newBalance)
        crudService.saveNation(updated)
    }

    suspend fun withdraw(nationId: NationId, amount: Double) {
        val nation = crudService.getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        if (nation.balance < amount) throw IllegalArgumentException("Insufficient funds")
        val newBalance = nation.balance - amount
        val updated = nation.withModification(balance = newBalance)
        crudService.saveNation(updated)
    }

    suspend fun getTownDebt(nationId: NationId, townId: TownId): Double {
        val town = townCache.get(townId)
        return town?.nationDebt ?: 0.0
    }

    suspend fun setRuined(nationId: NationId, isRuined: Boolean, ruinedAt: java.time.Instant?) {
        val nation = crudService.getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val updated = nation.withModification(isRuined = isRuined, ruinedAt = ruinedAt)
        crudService.saveNation(updated)
    }

    // Leadership operations
    suspend fun setKing(nationId: NationId, newKingId: ResidentId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId) ?: throw IllegalStateException("Nation $nationId does not exist")
        RuinRecoveryService.requireNationNotRuined(nation, "change leadership")
        val updated = nation.withModification(kingId = newKingId)
        crudService.saveNation(updated)
        updated
    }

    suspend fun setCapital(nationId: NationId, newCapitalId: TownId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId) ?: throw IllegalStateException("Nation $nationId does not exist")
        RuinRecoveryService.requireNationNotRuined(nation, "change capital")
        if (newCapitalId !in nation.townIds) throw IllegalStateException("Capital must be a town in the nation")
        val updated = nation.withModification(capitalId = newCapitalId)
        crudService.saveNation(updated)
        updated
    }

    // Diplomatic operations
    suspend fun addAlly(nationId: NationId, allyNationId: NationId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId) ?: throw IllegalStateException("Nation $nationId does not exist")
        RuinRecoveryService.requireNationNotRuined(nation, "form alliances")

        val allyNation = nationCache.get(allyNationId) ?: throw IllegalStateException("Ally nation $allyNationId does not exist")
        RuinRecoveryService.requireNationNotRuined(allyNation, "form alliances")

        val updated = nation.withModification(allyNationIds = nation.allyNationIds + allyNationId)
        crudService.saveNation(updated)
        updated
    }

    suspend fun removeAlly(nationId: NationId, allyNationId: NationId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId) ?: throw IllegalStateException("Nation $nationId does not exist")
        RuinRecoveryService.requireNationNotRuined(nation, "remove alliances")
        val updated = nation.withModification(allyNationIds = nation.allyNationIds - allyNationId)
        crudService.saveNation(updated)
        updated
    }

    suspend fun getAllies(nationId: NationId): List<Nation> {
        val nation = nationCache.get(nationId) ?: return emptyList()
        return nation.allyNationIds.mapNotNull { nationCache.get(it) }
    }

    suspend fun areAllies(nationId1: NationId, nationId2: NationId): Boolean {
        val nation1 = nationCache.get(nationId1) ?: return false
        val nation2 = nationCache.get(nationId2) ?: return false
        return nationId2 in nation1.allyNationIds && nationId1 in nation2.allyNationIds
    }

    // Settings operations
    suspend fun setBoard(nationId: NationId, board: String): Nation = mutex.withLock {
        val nation = nationCache.get(nationId) ?: throw IllegalStateException("Nation $nationId does not exist")
        RuinRecoveryService.requireNationNotRuined(nation, "change board message")
        val updated = nation.withModification(board = board)
        crudService.saveNation(updated)
        updated
    }

    suspend fun setPublic(nationId: NationId, isPublic: Boolean): Nation = mutex.withLock {
        val nation = nationCache.get(nationId) ?: throw IllegalStateException("Nation $nationId does not exist")
        RuinRecoveryService.requireNationNotRuined(nation, "change visibility")
        val updated = nation.withModification(isPublic = isPublic)
        crudService.saveNation(updated)
        updated
    }
}
