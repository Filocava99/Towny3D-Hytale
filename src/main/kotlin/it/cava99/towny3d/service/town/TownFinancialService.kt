package it.cava99.towny3d.service.town

import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.RuinRecoveryService
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles Town financial operations (balance, deposits, withdrawals, taxes, debt).
 */
class TownFinancialService(
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter,
    private val crudService: TownCrudService
) {
    private val mutex = Mutex()

    suspend fun setBalance(townId: TownId, balance: Double): Town = mutex.withLock {
        require(balance >= 0) { "Balance cannot be negative" }
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        val updated = town.withModification(balance = balance)
        crudService.saveTown(updated)
        updated
    }

    suspend fun deposit(townId: TownId, amount: Double): Town = mutex.withLock {
        require(amount > 0) { "Amount must be positive" }
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        val updated = town.withModification(balance = town.balance + amount)
        crudService.saveTown(updated)
        updated
    }

    suspend fun withdraw(townId: TownId, amount: Double): Town = mutex.withLock {
        require(amount > 0) { "Amount must be positive" }
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "withdraw money")

        if (town.balance < amount) {
            throw IllegalStateException("Insufficient funds. Current: ${town.balance}, required: $amount")
        }

        val updated = town.withModification(balance = town.balance - amount)
        crudService.saveTown(updated)
        updated
    }

    suspend fun setTaxToResidents(townId: TownId, taxConfig: TaxConfig) {
        val town = crudService.getTown(townId) ?: throw IllegalArgumentException("Town not found: $townId")
        val updated = town.withModification(taxToResidents = taxConfig)
        crudService.saveTown(updated)
    }

    suspend fun setNationDebt(townId: TownId, debt: Double) {
        val town = crudService.getTown(townId) ?: throw IllegalArgumentException("Town not found: $townId")
        val updated = town.withModification(nationDebt = debt)
        crudService.saveTown(updated)
    }

    suspend fun setRuined(townId: TownId, isRuined: Boolean, ruinedAt: java.time.Instant?) {
        val town = crudService.getTown(townId) ?: throw IllegalArgumentException("Town not found: $townId")
        val updated = town.withModification(isRuined = isRuined, ruinedAt = ruinedAt)
        crudService.saveTown(updated)
    }
}
