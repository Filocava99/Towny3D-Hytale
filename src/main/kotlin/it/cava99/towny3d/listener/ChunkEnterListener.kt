package it.cava99.towny3d.listener

import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.RegionService
import it.cava99.towny3d.service.TownService
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Detects when players enter different territory types (wilderness vs town)
 * and sends title messages.
 *
 * Only sends title when territory TYPE changes:
 * - Wilderness → Town: Show "Entering [TownName]"
 * - Town → Wilderness: Show "Entering Wilderness"
 * - Town → Town: No title (same type)
 * - Wilderness → Wilderness: No title (same type)
 */
class ChunkEnterListener(
    private val plugin: Towny3D,
    private val regionService: RegionService,
    private val townService: TownService
) {
    // Track last territory type for each player
    private val lastTerritory = ConcurrentHashMap<UUID, TerritoryType>()

    /**
     * Call this when player moves to a new block position.
     * @param playerUuid The player's UUID
     * @param blockX The block X coordinate
     * @param blockZ The block Z coordinate
     * @param sendTitle Function to send title to player
     */
    fun onPlayerMove(
        playerUuid: UUID,
        blockX: Int,
        blockZ: Int,
        sendTitle: (title: String, subtitle: String) -> Unit
    ) {
        val chunkX = blockX shr 4
        val chunkZ = blockZ shr 4

        val oldTerritory = lastTerritory[playerUuid]

        plugin.pluginScope.launch {
            try {
                val newTerritory = determineTerritory(chunkX, chunkZ)

                // Only show title if territory TYPE changed
                if (oldTerritory?.javaClass != newTerritory.javaClass) {
                    when (newTerritory) {
                        is TerritoryType.Wilderness -> {
                            sendTitle("Entering Wilderness", "Unclaimed territory")
                        }
                        is TerritoryType.Town -> {
                            val town = townService.getTown(newTerritory.townId)
                            if (town != null) {
                                val mayor = plugin.residentService.getResident(town.mayorId)
                                val subtitle = buildString {
                                    append("Owned by ")
                                    append(mayor?.name ?: "Unknown")
                                }
                                sendTitle("Entering ${town.name}", subtitle)
                            }
                        }
                    }
                    lastTerritory[playerUuid] = newTerritory
                }
            } catch (e: Exception) {
                TownyLog.error("Failed to send title", e)
            }
        }
    }

    /**
     * Determines the territory type at the given chunk position.
     */
    private suspend fun determineTerritory(chunkX: Int, chunkZ: Int): TerritoryType {
        val blockX = chunkX * 16
        val blockZ = chunkZ * 16
        val worldName = plugin.getConfig().defaultWorldName
        val blockLocation = it.cava99.towny3d.model.BlockLocation(worldName, blockX, 64, blockZ)
        val region = plugin.regionService.getRegionAt(blockLocation)
        return if (region != null) {
            TerritoryType.Town(region.townId)
        } else {
            TerritoryType.Wilderness
        }
    }

    /**
     * Clears tracking for a player (call when they disconnect).
     */
    fun clearPlayer(playerUuid: UUID) {
        lastTerritory.remove(playerUuid)
    }
}

/**
 * Sealed class representing different territory types.
 */
sealed class TerritoryType {
    /** Wilderness (unclaimed territory) */
    object Wilderness : TerritoryType()

    /** Town territory */
    data class Town(val townId: TownId) : TerritoryType()
}
