package it.cava99.towny3d.service.town

import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.RuinRecoveryService
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles Town resident management operations (add, remove, mayor, assistants).
 */
class TownResidentService(
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter,
    private val config: TownyConfig,
    private val crudService: TownCrudService
) {
    private val mutex = Mutex()

    suspend fun addResident(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "accept new residents")

        if (residentId in town.residentIds) return town

        val updated = town.withModification(residentIds = town.residentIds + residentId)
        val newMaxLayers = config.calculateMaxLayers(updated.residentCount)
        val finalUpdated = updated.copy(maxLayers = newMaxLayers)
        crudService.saveTown(finalUpdated)
        finalUpdated
    }

    suspend fun removeResident(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")

        if (residentId == town.mayorId) {
            throw IllegalStateException("Cannot remove mayor from town")
        }

        val updated = town.withModification(residentIds = town.residentIds - residentId)
        val newMaxLayers = config.calculateMaxLayers(updated.residentCount)
        val finalUpdated = updated.copy(maxLayers = newMaxLayers)
        crudService.saveTown(finalUpdated)
        finalUpdated
    }

    suspend fun setMayor(townId: TownId, newMayorId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")

        if (newMayorId !in town.residentIds) {
            throw IllegalStateException("New mayor must be a resident")
        }

        RuinRecoveryService.requireTownNotRuined(town, "change leadership")

        val updated = town.withModification(mayorId = newMayorId)
        crudService.saveTown(updated)
        updated
    }

    suspend fun addAssistant(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")

        if (residentId !in town.residentIds) {
            throw IllegalStateException("Assistant must be a resident")
        }

        RuinRecoveryService.requireTownNotRuined(town, "add assistants")

        val updated = town.withModification(assistantIds = town.assistantIds + residentId)
        crudService.saveTown(updated)
        updated
    }

    suspend fun removeAssistant(townId: TownId, residentId: ResidentId): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")

        RuinRecoveryService.requireTownNotRuined(town, "remove assistants")

        val updated = town.withModification(assistantIds = town.assistantIds - residentId)
        crudService.saveTown(updated)
        updated
    }

    suspend fun getOnlineResidents(townId: TownId): List<ResidentId> {
        val town = townCache.get(townId) ?: return emptyList()
        return town.residentIds.toList()
    }
}
