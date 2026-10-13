package it.cava99.towny3d.service

import it.cava99.towny3d.cache.NationCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service for managing Nation CRUD operations.
 *
 * @property nationCache Hot cache for nations
 * @property townCache Hot cache for towns
 * @property batchWriter Batch writer for async storage
 */
class NationService(
    private val nationCache: NationCache,
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter
) {
    private val mutex = Mutex()

    /**
     * Creates a new nation.
     * @param name The nation name
     * @param kingId The king's resident ID
     * @param capitalTownId The capital town ID
     * @return The created nation
     * @throws IllegalStateException if nation name exists or town invalid
     */
    suspend fun createNation(
        name: String,
        kingId: ResidentId,
        capitalTownId: TownId
    ): Nation = mutex.withLock {
        // Check name uniqueness
        if (nationCache.getByName(name) != null) {
            throw IllegalStateException("Nation '$name' already exists")
        }

        // Validate capital town exists and has no nation
        val capital = townCache.get(capitalTownId)
            ?: throw IllegalStateException("Capital town $capitalTownId does not exist")

        if (capital.hasNation) {
            throw IllegalStateException("Capital town already belongs to a nation")
        }

        // Create the nation
        val nation = Nation.create(name, kingId, capitalTownId)

        // Save nation
        saveNation(nation)

        // Update capital town
        // Note: TownService.setNation is called by the caller

        nation
    }

    /**
     * Deletes a nation.
     * @param nationId The nation ID
     * @return true if deleted, false if not found
     */
    suspend fun deleteNation(nationId: NationId): Boolean = mutex.withLock {
        val nation = nationCache.get(nationId) ?: return false

        // Invalidate cache
        nationCache.invalidate(nationId)

        // Queue deletion
        batchWriter.deleteNation(nationId)

        // Note: Town nations are cleared by caller
        true
    }

    /**
     * Gets a nation by ID.
     * @param nationId The nation ID
     * @return The nation, or null if not found
     */
    suspend fun getNation(nationId: NationId): Nation? {
        return nationCache.get(nationId)
    }

    /**
     * Gets a nation by name.
     * @param name The nation name (case-insensitive)
     * @return The nation, or null if not found
     */
    suspend fun getNationByName(name: String): Nation? {
        return nationCache.getByName(name)
    }

    /**
     * Gets all nations.
     * @return List of all nations
     */
    suspend fun getAllNations(): List<Nation> {
        return nationCache.getAllCached()
    }

    /**
     * Adds a town to the nation.
     * @param nationId The nation ID
     * @param townId The town ID to add
     * @return The updated nation
     * @throws IllegalStateException if nation/town doesn't exist, nation/town is ruined, or town already has a nation
     */
    suspend fun addTown(nationId: NationId, townId: TownId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        // Check if nation is ruined
        RuinRecoveryService.requireNationNotRuined(nation, "accept new towns")

        // Validate town exists and has no nation
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is also ruined
        RuinRecoveryService.requireTownNotRuined(town, "join a nation")

        if (town.hasNation) {
            throw IllegalStateException("Town already belongs to a nation")
        }

        val updated = nation.withModification(townIds = nation.townIds + townId)
        saveNation(updated)
        updated
    }

    /**
     * Removes a town from a nation.
     * Cannot remove the capital town.
     * @param nationId The nation ID
     * @param townId The town ID to remove
     * @return The updated nation
     * @throws IllegalStateException if nation doesn't exist or town is the capital
     */
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

    /**
     * Sets a new king for the nation.
     * @param nationId The nation ID
     * @param newKingId The new king's resident ID
     * @return The updated nation
     * @throws IllegalStateException if nation doesn't exist or is ruined
     */
    suspend fun setKing(nationId: NationId, newKingId: ResidentId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        // Check if nation is ruined
        RuinRecoveryService.requireNationNotRuined(nation, "change leadership")

        val updated = nation.withModification(kingId = newKingId)
        saveNation(updated)
        updated
    }

    /**
     * Sets a new capital town for the nation.
     * The town must already be a member of the nation.
     * @param nationId The nation ID
     * @param newCapitalId The new capital town ID
     * @return The updated nation
     * @throws IllegalStateException if nation doesn't exist, is ruined, or town is not in nation
     */
    suspend fun setCapital(nationId: NationId, newCapitalId: TownId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        // Check if nation is ruined
        RuinRecoveryService.requireNationNotRuined(nation, "change capital")

        // Validate town is in nation
        if (newCapitalId !in nation.townIds) {
            throw IllegalStateException("Capital must be a town in the nation")
        }

        val updated = nation.withModification(capitalId = newCapitalId)
        saveNation(updated)
        updated
    }

    /**
     * Adds an ally nation.
     * @param nationId The nation ID
     * @param allyNationId The ally nation ID to add
     * @return The updated nation
     * @throws IllegalStateException if either nation doesn't exist or is ruined
     */
    suspend fun addAlly(nationId: NationId, allyNationId: NationId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        // Check if nation is ruined
        RuinRecoveryService.requireNationNotRuined(nation, "form alliances")

        // Validate ally nation exists
        val allyNation = nationCache.get(allyNationId)
            ?: throw IllegalStateException("Ally nation $allyNationId does not exist")

        // Check if ally nation is also ruined
        RuinRecoveryService.requireNationNotRuined(allyNation, "form alliances")

        val updated = nation.withModification(allyNationIds = nation.allyNationIds + allyNationId)
        saveNation(updated)
        updated
    }

    /**
     * Removes an ally nation.
     * @param nationId The nation ID
     * @param allyNationId The ally nation ID to remove
     * @return The updated nation
     * @throws IllegalStateException if nation doesn't exist or is ruined
     */
    suspend fun removeAlly(nationId: NationId, allyNationId: NationId): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        // Check if nation is ruined
        RuinRecoveryService.requireNationNotRuined(nation, "remove alliances")

        val updated = nation.withModification(allyNationIds = nation.allyNationIds - allyNationId)
        saveNation(updated)
        updated
    }

    /**
     * Sets the nation's board message.
     * @param nationId The nation ID
     * @param board The board message
     * @return The updated nation
     * @throws IllegalStateException if nation doesn't exist or is ruined
     */
    suspend fun setBoard(nationId: NationId, board: String): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        // Check if nation is ruined
        RuinRecoveryService.requireNationNotRuined(nation, "change board message")

        val updated = nation.withModification(board = board)
        saveNation(updated)
        updated
    }

    /**
     * Sets the nation's public visibility.
     * @param nationId The nation ID
     * @param isPublic Whether the nation is publicly visible
     * @return The updated nation
     * @throws IllegalStateException if nation doesn't exist or is ruined
     */
    suspend fun setPublic(nationId: NationId, isPublic: Boolean): Nation = mutex.withLock {
        val nation = nationCache.get(nationId)
            ?: throw IllegalStateException("Nation $nationId does not exist")

        // Check if nation is ruined
        RuinRecoveryService.requireNationNotRuined(nation, "change visibility")

        val updated = nation.withModification(isPublic = isPublic)
        saveNation(updated)
        updated
    }

    /**
     * Saves a nation to cache and queues storage write.
     */
    private suspend fun saveNation(nation: Nation) {
        nationCache.put(nation)
        batchWriter.saveNation(nation)
    }

    /**
     * Gets towns in a nation.
     * @param nationId The nation ID
     * @return List of towns in the nation
     */
    suspend fun getTownsInNation(nationId: NationId): List<Town> {
        val nation = nationCache.get(nationId) ?: return emptyList()
        return nation.townIds.mapNotNull { townCache.get(it) }
    }

    /**
     * Gets nations allied with a nation.
     * @param nationId The nation ID
     * @return List of allied nations
     */
    suspend fun getAllies(nationId: NationId): List<Nation> {
        val nation = nationCache.get(nationId) ?: return emptyList()
        return nation.allyNationIds.mapNotNull { nationCache.get(it) }
    }

    /**
     * Checks if two nations are allies.
     * @param nationId1 First nation ID
     * @param nationId2 Second nation ID
     * @return true if both nations have each other as allies
     */
    suspend fun areAllies(nationId1: NationId, nationId2: NationId): Boolean {
        val nation1 = nationCache.get(nationId1) ?: return false
        val nation2 = nationCache.get(nationId2) ?: return false

        return nationId2 in nation1.allyNationIds && nationId1 in nation2.allyNationIds
    }

    /**
     * Set the tax configuration for member towns.
     * @param nationId The nation ID
     * @param taxConfig The tax configuration
     * @throws IllegalArgumentException if nation not found
     */
    suspend fun setTaxToTowns(nationId: NationId, taxConfig: TaxConfig) {
        val nation = getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val updated = nation.withModification(taxToTowns = taxConfig)
        saveNation(updated)
    }

    /**
     * Set the nation's bank balance.
     * @param nationId The nation ID
     * @param balance The new balance
     * @throws IllegalArgumentException if nation not found
     */
    suspend fun setBalance(nationId: NationId, balance: Double) {
        val nation = getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val updated = nation.withModification(balance = balance)
        saveNation(updated)
    }

    /**
     * Deposit to the nation bank.
     * @param nationId The nation ID
     * @param amount The amount to deposit
     * @throws IllegalArgumentException if nation not found
     */
    suspend fun deposit(nationId: NationId, amount: Double) {
        val nation = getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val newBalance = nation.balance + amount
        val updated = nation.withModification(balance = newBalance)
        saveNation(updated)
    }

    /**
     * Withdraw from the nation bank.
     * @param nationId The nation ID
     * @param amount The amount to withdraw
     * @throws IllegalArgumentException if nation not found or insufficient funds
     */
    suspend fun withdraw(nationId: NationId, amount: Double) {
        val nation = getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        if (nation.balance < amount) {
            throw IllegalArgumentException("Insufficient funds")
        }
        val newBalance = nation.balance - amount
        val updated = nation.withModification(balance = newBalance)
        saveNation(updated)
    }

    /**
     * Get a town's debt to the nation.
     * @param nationId The nation ID
     * @param townId The town ID
     * @return The town's debt amount, or 0.0 if town not found
     */
    suspend fun getTownDebt(nationId: NationId, townId: TownId): Double {
        val town = townCache.get(townId)
        return town?.nationDebt ?: 0.0
    }

    /**
     * Set the ruined state of a nation.
     * @param nationId The nation ID
     * @param isRuined Whether the nation is ruined
     * @param ruinedAt When the nation became ruined (null if not ruined)
     */
    suspend fun setRuined(nationId: NationId, isRuined: Boolean, ruinedAt: java.time.Instant?) {
        val nation = getNation(nationId) ?: throw IllegalArgumentException("Nation not found: $nationId")
        val updated = nation.withModification(isRuined = isRuined, ruinedAt = ruinedAt)
        saveNation(updated)
    }
}
