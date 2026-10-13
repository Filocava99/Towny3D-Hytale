package it.cava99.towny3d.economy

import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.storage.StorageBackend
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Internal implementation of EconomyService that stores balance
 * directly in Resident and Town models.
 *
 * This implementation is thread-safe and uses the provided StorageBackend
 * to persist balance changes.
 *
 * This file is intentionally large because:
 * - It implements a single cohesive responsibility (fallback economy service)
 * - Must handle three entity types (Resident, Town, Nation) with identical operation patterns
 * - Each entity type needs getBalance, withdraw, and deposit methods for the interface
 * - Cache management (invalidate, clear, warmup) is tightly coupled to the balance operations
 * - Separating by entity type would duplicate the mutex and caching logic
 *
 * Future improvements:
 * - Could potentially use generics to reduce repetitive balance operation patterns
 * - Consider extracting cache management to a separate helper if caching becomes more complex
 *
 * @param storage The storage backend for loading/saving data
 * @param currencyName The name of the currency (singular)
 * @param currencyNamePlural The name of the currency (plural)
 * @param currencySymbol The currency symbol (optional)
 */
class InternalEconomyService(
    private val storage: StorageBackend,
    private val currencyName: String = "coin",
    private val currencyNamePlural: String = "coins",
    private val currencySymbol: String = ""
) : EconomyService {

    // Mutexes for thread-safe operations
    private val playerMutex = Mutex()
    private val townMutex = Mutex()
    private val nationMutex = Mutex()

    // Cache to avoid excessive storage reads
    private val playerBalanceCache = mutableMapOf<ResidentId, Double>()
    private val townBalanceCache = mutableMapOf<TownId, Double>()
    private val nationBalanceCache = mutableMapOf<NationId, Double>()

    override suspend fun getBalance(residentId: ResidentId): Double = playerMutex.withLock {
        // Check cache first
        playerBalanceCache[residentId]?.let { return it }

        // Load from storage
        val resident = storage.loadResident(residentId)
        return if (resident != null) {
            playerBalanceCache[residentId] = resident.balance
            resident.balance
        } else {
            0.0
        }
    }

    override suspend fun withdraw(residentId: ResidentId, amount: Double): Boolean = playerMutex.withLock {
        if (amount < 0) return false

        // Get current balance from cache (don't call getBalance() to avoid reentrant lock)
        val currentBalance = playerBalanceCache[residentId] ?: run {
            val resident = storage.loadResident(residentId)
            if (resident != null) {
                playerBalanceCache[residentId] = resident.balance
                resident.balance
            } else {
                0.0
            }
        }
        if (currentBalance < amount) return false

        val newBalance = currentBalance - amount
        playerBalanceCache[residentId] = newBalance

        // Persist to storage
        val resident = storage.loadResident(residentId)
        if (resident != null) {
            storage.saveResident(resident.withBalance(newBalance))
        }

        return true
    }

    override suspend fun deposit(residentId: ResidentId, amount: Double): Boolean = playerMutex.withLock {
        if (amount < 0) return false

        // Get current balance from cache (don't call getBalance() to avoid reentrant lock)
        val currentBalance = playerBalanceCache[residentId] ?: run {
            val resident = storage.loadResident(residentId)
            if (resident != null) {
                playerBalanceCache[residentId] = resident.balance
                resident.balance
            } else {
                0.0
            }
        }
        val newBalance = currentBalance + amount
        playerBalanceCache[residentId] = newBalance

        // Persist to storage
        val resident = storage.loadResident(residentId)
        if (resident != null) {
            storage.saveResident(resident.withBalance(newBalance))
        }

        return true
    }

    override suspend fun getTownBalance(townId: TownId): Double = townMutex.withLock {
        // Check cache first
        townBalanceCache[townId]?.let { return it }

        // Load from storage
        val town = storage.loadTown(townId)
        return if (town != null) {
            townBalanceCache[townId] = town.balance
            town.balance
        } else {
            0.0
        }
    }

    override suspend fun withdrawFromTown(townId: TownId, amount: Double): Boolean = townMutex.withLock {
        if (amount < 0) return false

        val currentBalance = getTownBalance(townId)
        if (currentBalance < amount) return false

        val newBalance = currentBalance - amount
        townBalanceCache[townId] = newBalance

        // Persist to storage
        val town = storage.loadTown(townId)
        if (town != null) {
            storage.saveTown(town.withModification(balance = newBalance))
        }

        return true
    }

    override suspend fun depositToTown(townId: TownId, amount: Double): Boolean = townMutex.withLock {
        if (amount < 0) return false

        val currentBalance = getTownBalance(townId)
        val newBalance = currentBalance + amount
        townBalanceCache[townId] = newBalance

        // Persist to storage
        val town = storage.loadTown(townId)
        if (town != null) {
            storage.saveTown(town.withModification(balance = newBalance))
        }

        return true
    }

    override suspend fun getNationBalance(nationId: NationId): Double = nationMutex.withLock {
        // Check cache first
        nationBalanceCache[nationId]?.let { return it }

        // Load from storage
        val nation = storage.loadNation(nationId)
        return if (nation != null) {
            nationBalanceCache[nationId] = nation.balance
            nation.balance
        } else {
            0.0
        }
    }

    override suspend fun withdrawFromNation(nationId: NationId, amount: Double): Boolean = nationMutex.withLock {
        if (amount < 0) return false

        // Get current balance from cache (don't call getNationBalance() to avoid reentrant lock)
        val currentBalance = nationBalanceCache[nationId] ?: run {
            val nation = storage.loadNation(nationId)
            if (nation != null) {
                nationBalanceCache[nationId] = nation.balance
                nation.balance
            } else {
                0.0
            }
        }
        if (currentBalance < amount) return false

        val newBalance = currentBalance - amount
        nationBalanceCache[nationId] = newBalance

        // Persist to storage
        val nation = storage.loadNation(nationId)
        if (nation != null) {
            storage.saveNation(nation.withModification(balance = newBalance))
        }

        return true
    }

    override suspend fun depositToNation(nationId: NationId, amount: Double): Boolean = nationMutex.withLock {
        if (amount < 0) return false

        // Get current balance from cache (don't call getNationBalance() to avoid reentrant lock)
        val currentBalance = nationBalanceCache[nationId] ?: run {
            val nation = storage.loadNation(nationId)
            if (nation != null) {
                nationBalanceCache[nationId] = nation.balance
                nation.balance
            } else {
                0.0
            }
        }
        val newBalance = currentBalance + amount
        nationBalanceCache[nationId] = newBalance

        // Persist to storage
        val nation = storage.loadNation(nationId)
        if (nation != null) {
            storage.saveNation(nation.withModification(balance = newBalance))
        }

        return true
    }

    override fun formatAmount(amount: Double): String {
        return if (currencySymbol.isNotEmpty()) {
            "$currencySymbol${"%.2f".format(amount)}"
        } else {
            "${"%.2f".format(amount)} $currencyNamePlural"
        }
    }

    override fun getCurrencyName(): String = currencyName

    override fun getCurrencyNamePlural(): String = currencyNamePlural

    /**
     * Invalidates cached balance for a player.
     * Call this after external balance modifications.
     */
    suspend fun invalidatePlayerCache(residentId: ResidentId) = playerMutex.withLock {
        playerBalanceCache.remove(residentId)
    }

    /**
     * Invalidates cached balance for a town.
     * Call this after external balance modifications.
     */
    suspend fun invalidateTownCache(townId: TownId) = townMutex.withLock {
        townBalanceCache.remove(townId)
    }

    /**
     * Invalidates cached balance for a nation.
     * Call this after external balance modifications.
     */
    suspend fun invalidateNationCache(nationId: NationId) = nationMutex.withLock {
        nationBalanceCache.remove(nationId)
    }

    /**
     * Clears all cached balances.
     */
    suspend fun clearAllCaches() {
        playerMutex.withLock {
            playerBalanceCache.clear()
        }
        townMutex.withLock {
            townBalanceCache.clear()
        }
        nationMutex.withLock {
            nationBalanceCache.clear()
        }
    }

    /**
     * Warms up the cache by loading all balances from storage.
     */
    suspend fun warmUpCache() {
        // Load all residents
        storage.loadAllResidents().forEach { resident ->
            playerBalanceCache[resident.id] = resident.balance
        }

        // Load all towns
        storage.loadAllTowns().forEach { town ->
            townBalanceCache[town.id] = town.balance
        }

        // Load all nations
        storage.loadAllNations().forEach { nation ->
            nationBalanceCache[nation.id] = nation.balance
        }
    }

    companion object {
        /**
         * Creates a default InternalEconomyService with standard settings.
         */
        fun create(storage: StorageBackend): InternalEconomyService {
            return InternalEconomyService(
                storage = storage,
                currencyName = "coin",
                currencyNamePlural = "coins",
                currencySymbol = ""
            )
        }

        /**
         * Creates an InternalEconomyService with dollar-style formatting.
         */
        fun createWithDollar(storage: StorageBackend): InternalEconomyService {
            return InternalEconomyService(
                storage = storage,
                currencyName = "dollar",
                currencyNamePlural = "dollars",
                currencySymbol = "$"
            )
        }
    }
}
