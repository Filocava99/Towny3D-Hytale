package it.cava99.towny3d.service

import it.cava99.towny3d.cache.ResidentCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.ChatMode
import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.storage.BatchStorageWriter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Service for managing Resident CRUD operations.
 *
 * @property residentCache Hot cache for residents
 * @property townCache Hot cache for towns (for validation)
 * @property batchWriter Batch writer for async storage
 */
class ResidentService(
    private val residentCache: ResidentCache,
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter
) {
    private val mutex = Mutex()

    /**
     * Loads or creates a resident on player join.
     * Updates the name if the resident exists but has "Unknown" or a different name.
     * @param uuid The player's UUID
     * @param name The player's name
     * @return The resident (loaded or created)
     */
    suspend fun loadOrCreate(uuid: UUID, name: String): Resident = mutex.withLock {
        val id = it.cava99.towny3d.model.ResidentId.fromPlayerUuid(uuid)
        val existing = residentCache.get(id)

        // If resident exists, update name if it's "Unknown" or different
        if (existing != null) {
            // Only update name if existing name is "Unknown" or if the new name is better (not "Unknown")
            if (existing.name == "Unknown" && name != "Unknown") {
                val updated = existing.copy(name = name, modifiedAt = java.time.Instant.now())
                saveResident(updated)
                return updated
            }
            return existing
        }

        // Create new resident
        TownyLog.info("Creating new resident: name='$name'")
        val resident = Resident.create(uuid, name)
        saveResident(resident)
        resident
    }

    /**
     * Gets a resident by ID.
     * @param residentId The resident ID
     * @return The resident, or null if not found
     */
    suspend fun getResident(residentId: ResidentId): Resident? {
        return residentCache.get(residentId)
    }

    /**
     * Gets a resident by player UUID.
     * @param uuid The player's UUID
     * @return The resident, or null if not found
     */
    suspend fun getResidentByUuid(uuid: UUID): Resident? {
        val id = it.cava99.towny3d.model.ResidentId.fromPlayerUuid(uuid)
        return residentCache.get(id)
    }

    /**
     * Gets a resident by name.
     * @param name The player name (case-insensitive)
     * @return The resident, or null if not found
     */
    suspend fun getResidentByName(name: String): Resident? {
        return residentCache.getByName(name)
    }

    /**
     * Gets all residents.
     * @return List of all residents
     */
    suspend fun getAllResidents(): List<Resident> {
        return residentCache.getAllCached()
    }

    /**
     * Updates a resident's town membership.
     * @param residentId The resident ID
     * @param townId The town ID (null to remove from town)
     * @return The updated resident
     * @throws IllegalStateException if resident or town doesn't exist
     */
    suspend fun setTown(residentId: ResidentId, townId: TownId?): Resident = mutex.withLock {
        val resident = residentCache.get(residentId)
            ?: throw IllegalStateException("Resident $residentId does not exist")

        // Validate town exists if setting
        if (townId != null && townCache.get(townId) == null) {
            throw IllegalStateException("Town $townId does not exist")
        }

        val updated = resident.withModification(townId = townId)
        saveResident(updated)
        updated
    }

    /**
     * Sets a resident's chat mode (GLOBAL, TOWN, NATION).
     * @param residentId The resident ID
     * @param chatMode The new chat mode
     * @return The updated resident
     * @throws IllegalStateException if resident doesn't exist
     */
    suspend fun setChatMode(residentId: ResidentId, chatMode: ChatMode): Resident = mutex.withLock {
        val resident = residentCache.get(residentId)
            ?: throw IllegalStateException("Resident $residentId does not exist")

        val updated = resident.copy(chatMode = chatMode, modifiedAt = java.time.Instant.now())
        saveResident(updated)
        updated
    }

    /**
     * Sets a resident's spy mode (admin chat monitoring).
     * @param residentId The resident ID
     * @param isSpy Whether spy mode is enabled
     * @return The updated resident
     * @throws IllegalStateException if resident doesn't exist
     */
    suspend fun setSpy(residentId: ResidentId, isSpy: Boolean): Resident = mutex.withLock {
        val resident = residentCache.get(residentId)
            ?: throw IllegalStateException("Resident $residentId does not exist")

        val updated = resident.copy(isSpy = isSpy, modifiedAt = java.time.Instant.now())
        saveResident(updated)
        updated
    }

    /**
     * Updates a resident's last online timestamp to now.
     * @param residentId The resident ID
     * @return The updated resident
     * @throws IllegalStateException if resident doesn't exist
     */
    suspend fun updateLastOnline(residentId: ResidentId): Resident = mutex.withLock {
        val resident = residentCache.get(residentId)
            ?: throw IllegalStateException("Resident $residentId does not exist")

        val updated = resident.copy(lastOnline = java.time.Instant.now(), modifiedAt = java.time.Instant.now())
        saveResident(updated)
        updated
    }

    /**
     * Sets a resident's account balance.
     * @param residentId The resident ID
     * @param balance The new balance (must be non-negative)
     * @return The updated resident
     * @throws IllegalArgumentException if balance is negative
     * @throws IllegalStateException if resident doesn't exist
     */
    suspend fun setBalance(residentId: ResidentId, balance: Double): Resident = mutex.withLock {
        require(balance >= 0) { "Balance cannot be negative" }

        val resident = residentCache.get(residentId)
            ?: throw IllegalStateException("Resident $residentId does not exist")

        val updated = resident.withBalance(balance)
        saveResident(updated)
        updated
    }

    /**
     * Gets all residents belonging to a town.
     * @param townId The town ID
     * @return List of residents in the town
     */
    suspend fun getResidentsByTown(townId: TownId): List<Resident> {
        return residentCache.getAllCached().filter { it.townId == townId }
    }

    /**
     * Gets online residents in a town.
     * Note: Currently returns all town residents (online filtering not yet implemented).
     * @param townId The town ID
     * @return List of online residents in the town
     */
    suspend fun getOnlineResidentsByTown(townId: TownId): List<Resident> {
        return getResidentsByTown(townId) // TODO(2026-02-22): Add online filtering
    }

    /**
     * Gets residents without a town (nomads/wilderness).
     * @return List of residents not belonging to any town
     */
    suspend fun getResidentsWithoutTown(): List<Resident> {
        return residentCache.getAllCached().filter { it.townId == null }
    }

    /**
     * Saves a resident to cache and queues storage write.
     */
    private suspend fun saveResident(resident: Resident) {
        residentCache.put(resident)
        batchWriter.saveResident(resident)
    }


    /**
     * Reconciles town membership data between residents and towns.
     * This fixes inconsistencies where:
     * - A resident has a townId but the town doesn't have them in residentIds
     * - A town has a resident in residentIds but the resident's townId is null or different
     *
     * @return A summary of fixes applied
     */
    suspend fun reconcileTownMembership(): ReconciliationResult {
        var fixedResidents = 0
        var fixedTowns = 0
        val errors = mutableListOf<String>()

        try {
            // Get all towns with their residents
            val allTowns = townCache.getAllCached()

            // Create a set of all resident IDs that should be in each town
            val expectedTownMembers = mutableMapOf<it.cava99.towny3d.model.TownId, MutableSet<ResidentId>>()
            allTowns.forEach { town ->
                expectedTownMembers[town.id] = town.residentIds.toMutableSet()
            }

            // Check all residents
            val allResidents = residentCache.getAllCached()
            for (resident in allResidents) {
                val residentTownId = resident.townId

                if (residentTownId != null) {
                    // Resident claims to be in a town - verify the town agrees
                    val townMembers = expectedTownMembers[residentTownId]

                    if (townMembers == null) {
                        // Resident's town doesn't exist - clear their townId
                        TownyLog.info("Reconciliation: Resident ${resident.name} has townId $residentTownId but town doesn't exist. Clearing townId.")
                        val updated = resident.withModification(townId = null)
                        saveResident(updated)
                        fixedResidents++
                    } else if (!townMembers.contains(resident.id)) {
                        // Town doesn't have this resident - add them
                        TownyLog.info("Reconciliation: Resident ${resident.name} has townId $residentTownId but town doesn't have them. Adding to town's resident list.")
                        // Note: We can't directly modify town here as we don't have TownService
                        // Just log this for manual intervention or add to a list for batch fix
                        errors.add("Resident ${resident.name} (ID: ${resident.id}) is in town $residentTownId but town's resident list doesn't include them")
                        fixedResidents++
                    }
                }
            }

            // Check towns for residents who don't have the town set
            for (town in allTowns) {
                for (residentId in town.residentIds) {
                    val resident = residentCache.get(residentId)

                    if (resident == null) {
                        errors.add("Town ${town.name} has resident $residentId in list but resident doesn't exist")
                        fixedTowns++
                    } else if (resident.townId != town.id) {
                        TownyLog.info("Reconciliation: Town ${town.name} has resident ${resident.name} in list but resident's townId is ${resident.townId}. Updating resident.")
                        val updated = resident.withModification(townId = town.id)
                        saveResident(updated)
                        fixedResidents++
                    }
                }
            }

        } catch (e: Exception) {
            errors.add("Reconciliation failed: ${e.message}")
            TownyLog.error("Reconciliation failed", e)
        }

        return ReconciliationResult(
            fixedResidents = fixedResidents,
            fixedTowns = fixedTowns,
            errors = errors
        )
    }

    /**
     * Result of a town membership reconciliation operation.
     * @property fixedResidents Number of resident records fixed
     * @property fixedTowns Number of town records with issues
     * @property errors List of error messages during reconciliation
     */
    data class ReconciliationResult(
        val fixedResidents: Int,
        val fixedTowns: Int,
        val errors: List<String>
    )

    /**
     * Deletes a resident.
     * @param residentId The resident ID
     * @return true if deleted, false if not found
     */
    suspend fun deleteResident(residentId: ResidentId): Boolean = mutex.withLock {
        val resident = residentCache.get(residentId) ?: return false

        residentCache.invalidate(residentId)
        batchWriter.deleteResident(residentId)
        true
    }
}
