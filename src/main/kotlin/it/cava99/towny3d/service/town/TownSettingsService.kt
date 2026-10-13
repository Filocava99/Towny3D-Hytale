package it.cava99.towny3d.service.town

import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.RuinRecoveryService
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles Town settings and configuration operations (home, board, public, open, layers, map color, nation).
 */
class TownSettingsService(
    private val townCache: TownCache,
    private val batchWriter: BatchStorageWriter,
    private val crudService: TownCrudService
) {
    private val mutex = Mutex()

    suspend fun setHomeLocation(townId: TownId, location: BlockLocation): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        RuinRecoveryService.requireTownNotRuined(town, "change home location")
        val updated = town.withModification(homeLocation = location)
        crudService.saveTown(updated)
        updated
    }

    suspend fun setBoard(townId: TownId, board: String): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        RuinRecoveryService.requireTownNotRuined(town, "change board message")
        val updated = town.withModification(board = board)
        crudService.saveTown(updated)
        updated
    }

    suspend fun setPublic(townId: TownId, isPublic: Boolean): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        RuinRecoveryService.requireTownNotRuined(town, "change visibility")
        val updated = town.withModification(isPublic = isPublic)
        crudService.saveTown(updated)
        updated
    }

    suspend fun setOpen(townId: TownId, isOpen: Boolean): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        RuinRecoveryService.requireTownNotRuined(town, "change open status")
        val updated = town.withModification(isOpen = isOpen)
        crudService.saveTown(updated)
        updated
    }

    suspend fun updateLayers(townId: TownId, newLayers: Int): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        RuinRecoveryService.requireTownNotRuined(town, "claim new land")
        require(newLayers >= 0) { "Layers cannot be negative" }
        require(newLayers <= town.maxLayers) { "Layers cannot exceed max" }
        val updated = town.withModification(layers = newLayers)
        crudService.saveTown(updated)
        updated
    }

    suspend fun setMapColor(townId: TownId, color: Int): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        RuinRecoveryService.requireTownNotRuined(town, "change map color")
        val updated = town.withModification(mapColor = color)
        crudService.saveTown(updated)
        updated
    }

    suspend fun setNation(townId: TownId, nationId: it.cava99.towny3d.model.NationId?): Town = mutex.withLock {
        val town = townCache.get(townId) ?: throw IllegalStateException("Town $townId does not exist")
        RuinRecoveryService.requireTownNotRuined(town, "change nation affiliation")

        val newMapColor = if (nationId != null) {
            town.mapColor
        } else {
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
        crudService.saveTown(updated)
        updated
    }
}
