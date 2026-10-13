package it.cava99.towny3d.service

import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.storage.BatchStorageWriter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service for managing Town CRUD operations.
 *
 * @property townCache Hot cache for towns
 * @property batchWriter Batch writer for async storage
 * @property regionService Region service for creating home regions
 * @property config Plugin configuration
 */
class TownService(
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter,
    private val regionService: RegionService,
    private val residentService: it.cava99.towny3d.service.ResidentService,
    private val config: TownyConfig
) {
    private val mutex = Mutex()

    /**
     * Creates a new town.
     * @param name The town name
     * @param mayorId The mayor's resident ID
     * @param homeLocation The town's spawn location
     * @param initialBalance Starting balance
     * @param initialLayers Starting layer count
     * @return The created town
     * @throws IllegalStateException if town name exists
     */
    suspend fun createTown(
        name: String,
        mayorId: ResidentId,
        homeLocation: BlockLocation,
        initialBalance: Double = 0.0,
        initialLayers: Int = 384,
        maxLayers: Int = config.calculateMaxLayers(1) // 1 resident (mayor)
    ): Town = mutex.withLock {
        // Check name uniqueness
        if (townCache.getByName(name) != null) {
            throw IllegalStateException("Town '$name' already exists")
        }

        // Create the town
        val town = Town.create(
            name = name,
            mayorId = mayorId,
            homeLocation = homeLocation,
            initialBalance = initialBalance,
            initialLayers = initialLayers,
            maxLayers = maxLayers
        )

        // Save to cache and storage
        saveTown(town)

        // Create home region at the town's spawn location
        val chunkX = homeLocation.chunkPos.x
        val chunkZ = homeLocation.chunkPos.z
        val homeRegion = regionService.createHomeRegion(
            townId = town.id,
            chunkPos = it.cava99.towny3d.model.ChunkPos(chunkX, chunkZ),
            y1 = -64,
            y2 = 320
        )

        TownyLog.info("Created home region for town '$name' at chunk ($chunkX, $chunkZ)")

        town
    }

    /**
     * Deletes a town and cleans up all associated data.
     * This will:
     * 1. Delete all regions owned by the town
     * 2. Remove the townId from all residents
     * 3. Delete the town itself
     *
     * @param townId The town ID
     * @return true if deleted, false if not found
     */
    suspend fun deleteTown(townId: TownId): Boolean = mutex.withLock {
        val town = townCache.get(townId) ?: return false

        // Step 1: Delete all regions owned by this town
        val deletedRegions = regionService.deleteRegionsByTown(townId)
        TownyLog.info("Deleted $deletedRegions regions for town '${town.name}'")

        // Step 2: Remove townId from all residents
        val residents = residentService.getResidentsByTown(townId)
        residents.forEach { resident ->
            residentService.setTown(resident.id, null)
        }
        TownyLog.info("Removed town membership from ${residents.size} residents")

        // Step 3: Invalidate cache and delete town
        townCache.invalidate(townId)
        batchWriter.deleteTown(townId)

        TownyLog.info("Deleted town '${town.name}'")
        true
    }

    /**
     * Gets a town by ID.
     * @param townId The town ID to look up
     * @return The town, or null if not found
     */
    suspend fun getTown(townId: TownId): Town? {
        return townCache.get(townId)
    }

    /**
     * Gets a town by name.
     * @param name The town name (case-insensitive)
     * @return The town, or null if not found
     */
    suspend fun getTownByName(name: String): Town? {
        return townCache.getByName(name)
    }

    /**
     * Gets all towns.
     * @return List of all towns
     */
    suspend fun getAllTowns(): List<Town> {
        return townCache.getAllCached()
    }

    /**
     * Adds a resident to a town.
     * @param townId The town ID
     * @param residentId The resident ID to add
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun addResident(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "accept new residents")

        if (residentId in town.residentIds) {
            return town // Already a member
        }

        val updated = town.withModification(residentIds = town.residentIds + residentId)
        // Recalculate maxLayers using config: base + (residentCount × layersPerPlayer)
        val newMaxLayers = config.calculateMaxLayers(updated.residentCount)
        val finalUpdated = updated.copy(maxLayers = newMaxLayers)
        saveTown(finalUpdated)
        finalUpdated
    }

    /**
     * Removes a resident from a town.
     * Cannot remove the mayor from the town.
     * @param townId The town ID
     * @param residentId The resident ID to remove
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or trying to remove mayor
     */
    suspend fun removeResident(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        if (residentId == town.mayorId) {
            throw IllegalStateException("Cannot remove mayor from town")
        }

        // Note: We allow removing residents from ruined towns
        // This allows players to leave a ruined town

        val updated = town.withModification(residentIds = town.residentIds - residentId)
        // Recalculate maxLayers using config: base + (residentCount × layersPerPlayer)
        val newMaxLayers = config.calculateMaxLayers(updated.residentCount)
        val finalUpdated = updated.copy(maxLayers = newMaxLayers)
        saveTown(finalUpdated)
        finalUpdated
    }

    /**
     * Sets a new mayor for the town.
     * The new mayor must already be a resident of the town.
     * @param townId The town ID
     * @param newMayorId The new mayor's resident ID
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist, is ruined, or new mayor is not a resident
     */
    suspend fun setMayor(townId: TownId, newMayorId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        if (newMayorId !in town.residentIds) {
            throw IllegalStateException("New mayor must be a resident")
        }

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "change leadership")

        val updated = town.withModification(mayorId = newMayorId)
        saveTown(updated)
        updated
    }

    /**
     * Adds an assistant to the town.
     * The assistant must already be a resident of the town.
     * @param townId The town ID
     * @param residentId The resident ID to make assistant
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist, is ruined, or resident is not a member
     */
    suspend fun addAssistant(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        if (residentId !in town.residentIds) {
            throw IllegalStateException("Assistant must be a resident")
        }

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "add assistants")

        val updated = town.withModification(assistantIds = town.assistantIds + residentId)
        saveTown(updated)
        updated
    }

    /**
     * Removes an assistant from the town.
     * @param townId The town ID
     * @param residentId The resident ID to remove as assistant
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun removeAssistant(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "remove assistants")

        val updated = town.withModification(assistantIds = town.assistantIds - residentId)
        saveTown(updated)
        updated
    }

    /**
     * Sets the town balance.
     */
    suspend fun setBalance(townId: TownId, balance: Double): Town = mutex.withLock {
        require(balance >= 0) { "Balance cannot be negative" }

        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        val updated = town.withModification(balance = balance)
        saveTown(updated)
        updated
    }

    /**
     * Deposits money to the town.
     * @param townId The town ID
     * @param amount The amount to deposit (must be positive)
     * @return The updated town
     * @throws IllegalArgumentException if amount is not positive
     * @throws IllegalStateException if town doesn't exist
     */
    suspend fun deposit(townId: TownId, amount: Double): Town = mutex.withLock {
        require(amount > 0) { "Amount must be positive" }

        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        val updated = town.withModification(balance = town.balance + amount)
        saveTown(updated)
        updated
    }

    /**
     * Withdraws money from the town bank.
     * @param townId The town ID
     * @param amount The amount to withdraw (must be positive)
     * @return The updated town
     * @throws IllegalArgumentException if amount is not positive
     * @throws IllegalStateException if town doesn't exist, is ruined, or has insufficient funds
     */
    suspend fun withdraw(townId: TownId, amount: Double): Town = mutex.withLock {
        require(amount > 0) { "Amount must be positive" }

        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "withdraw money")

        if (town.balance < amount) {
            throw IllegalStateException("Insufficient funds. Current: ${town.balance}, required: $amount")
        }

        val updated = town.withModification(balance = town.balance - amount)
        saveTown(updated)
        updated
    }

    /**
     * Sets the town's home/spawn location.
     * @param townId The town ID
     * @param location The new home location
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun setHomeLocation(townId: TownId, location: BlockLocation): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "change home location")

        val updated = town.withModification(homeLocation = location)
        saveTown(updated)
        updated
    }

    /**
     * Sets the town's board message.
     * @param townId The town ID
     * @param board The board message
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun setBoard(townId: TownId, board: String): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "change board message")

        val updated = town.withModification(board = board)
        saveTown(updated)
        updated
    }

    /**
     * Sets the town's public visibility.
     * @param townId The town ID
     * @param isPublic Whether the town is publicly visible
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun setPublic(townId: TownId, isPublic: Boolean): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "change visibility")

        val updated = town.withModification(isPublic = isPublic)
        saveTown(updated)
        updated
    }

    /**
     * Sets whether the town allows open joining.
     * @param townId The town ID
     * @param isOpen Whether anyone can join without invitation
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun setOpen(townId: TownId, isOpen: Boolean): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "change open status")

        val updated = town.withModification(isOpen = isOpen)
        saveTown(updated)
        updated
    }

    /**
     * Updates the town's claimed layer count.
     * @param townId The town ID
     * @param newLayers The new layer count (must be non-negative and <= maxLayers)
     * @return The updated town
     * @throws IllegalArgumentException if layers is negative or exceeds max
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun updateLayers(townId: TownId, newLayers: Int): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined before allowing land claims
        RuinRecoveryService.requireTownNotRuined(town, "claim new land")

        require(newLayers >= 0) { "Layers cannot be negative" }
        require(newLayers <= town.maxLayers) { "Layers cannot exceed max" }

        val updated = town.withModification(layers = newLayers)
        saveTown(updated)
        updated
    }

    /**
     * Sets the town's map color.
     * @param townId The town ID
     * @param color The RGB color value
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun setMapColor(townId: TownId, color: Int): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "change map color")

        val updated = town.withModification(mapColor = color)
        saveTown(updated)
        updated
    }

    /**
     * Sets the town's nation affiliation.
     * @param townId The town ID
     * @param nationId The nation ID (null to leave current nation)
     * @return The updated town
     * @throws IllegalStateException if town doesn't exist or is ruined
     */
    suspend fun setNation(townId: TownId, nationId: it.cava99.towny3d.model.NationId?): Town = mutex.withLock {
        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        // Check if town is ruined
        RuinRecoveryService.requireTownNotRuined(town, "change nation affiliation")

        // Determine new mapColor
        val newMapColor = if (nationId != null) {
            // Get nation color - will need NationCache for this
            // For now, keep current color (NationService will handle color update)
            town.mapColor
        } else {
            // Assign new random color when leaving nation
            val r = (0..255).random()
            val g = (0..255).random()
            val b = (0..255).random()
            (r shl 16) or (g shl 8) or b
        }

        val updated = town.copy(
            nationId = nationId,
            mapColor = newMapColor,
            modifiedAt = java.time.Instant.now()
        )
        saveTown(updated)
        updated
    }

    /**
     * Saves a town to cache and queues storage write.
     */
    private suspend fun saveTown(town: Town) {
        townCache.put(town)
        batchWriter.saveTown(town)
    }

    /**
     * Gets online residents of a town.
     * Note: This requires PlayerService to track online status
     */
    suspend fun getOnlineResidents(townId: TownId): List<ResidentId> {
        val town = townCache.get(townId) ?: return emptyList()
        // TODO(2026-02-22): Filter by online status when PlayerService is implemented
        return town.residentIds.toList()
    }

    /**
     * Set the tax configuration for residents.
     * @param townId The town ID
     * @param taxConfig The tax configuration to apply
     * @throws IllegalArgumentException if town not found
     */
    suspend fun setTaxToResidents(townId: TownId, taxConfig: TaxConfig) {
        val town = getTown(townId) ?: throw IllegalArgumentException("Town not found: $townId")
        val updated = town.withModification(taxToResidents = taxConfig)
        saveTown(updated)
    }

    /**
     * Set the town's debt to the nation.
     * @param townId The town ID
     * @param debt The debt amount
     * @throws IllegalArgumentException if town not found
     */
    suspend fun setNationDebt(townId: TownId, debt: Double) {
        val town = getTown(townId) ?: throw IllegalArgumentException("Town not found: $townId")
        val updated = town.withModification(nationDebt = debt)
        saveTown(updated)
    }

    /**
     * Set the ruined state of a town.
     * @param townId The town ID
     * @param isRuined Whether the town is ruined
     * @param ruinedAt When the town became ruined (null if not ruined)
     */
    suspend fun setRuined(townId: TownId, isRuined: Boolean, ruinedAt: java.time.Instant?) {
        val town = getTown(townId) ?: throw IllegalArgumentException("Town not found: $townId")
        val updated = town.withModification(isRuined = isRuined, ruinedAt = ruinedAt)
        saveTown(updated)
    }

    /**
     * Sets the town's maximum layers.
     * @param townId The town ID
     * @param maxLayers The new maximum layer count
     * @return The updated town
     * @throws IllegalArgumentException if maxLayers is not positive
     * @throws IllegalStateException if town doesn't exist
     */
    suspend fun setMaxLayers(townId: TownId, maxLayers: Int): Town = mutex.withLock {
        require(maxLayers > 0) { "Max layers must be positive" }

        val town = townCache.get(townId)
            ?: throw IllegalStateException("Town $townId does not exist")

        val updated = town.withModification(maxLayers = maxLayers)
        saveTown(updated)
        updated
    }
}
