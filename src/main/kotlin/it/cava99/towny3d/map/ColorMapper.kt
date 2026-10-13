package it.cava99.towny3d.map

import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.util.TownyLog
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.TownId

/**
 * Handles town/nation color assignment for map rendering.
 *
 * Determines the appropriate color for each chunk based on town and nation ownership,
 * and identifies border chunks between different territories.
 */
class ColorMapper(
    private val plugin: Towny3D
) {
    /**
     * Data class holding color information for a chunk.
     */
    data class ChunkColorInfo(
        val color: Int?,
        val regionId: RegionId?,
        val townId: TownId?
    )

    /**
     * Gets the color information for a chunk at the given position.
     *
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @return ChunkColorInfo containing the color (if claimed) and ownership info
     */
    fun getChunkColorInfo(chunkX: Int, chunkZ: Int): ChunkColorInfo {
        val chunkPos = ChunkPos(chunkX, chunkZ)
        val regionId = plugin.spatialIndex.getRegionAtIgnoringY(chunkPos)

        val region = regionId?.let { plugin.regionCache.getSync(it) }

        val town = region?.let { plugin.townCache.getSync(region.townId) }

        if (town != null && chunkX % 10 == 0 && chunkZ % 10 == 0) {
            TownyLog.info("Town found: ${town.name}, nationId=${town.nationId}, mapColor=${town.mapColor}")
        }

        val nation = town?.nationId?.let { plugin.nationCache.getSync(it) }

        if (nation != null && chunkX % 10 == 0 && chunkZ % 10 == 0) {
            TownyLog.info("Nation found: ${nation.name}, mapColor=${nation.mapColor}")
        }

        val townColor = nation?.mapColor ?: town?.mapColor

        return ChunkColorInfo(
            color = townColor,
            regionId = regionId,
            townId = region?.townId
        )
    }

    /**
     * Gets the region IDs of neighboring chunks.
     * Order: NORTH, SOUTH, EAST, WEST
     *
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @return Array of region IDs for neighboring chunks (null if unclaimed)
     */
    fun getNearbyRegions(chunkX: Int, chunkZ: Int): Array<RegionId?> {
        return arrayOf(
            plugin.spatialIndex.getRegionAtIgnoringY(ChunkPos(chunkX, chunkZ + 1)), // NORTH
            plugin.spatialIndex.getRegionAtIgnoringY(ChunkPos(chunkX, chunkZ - 1)), // SOUTH
            plugin.spatialIndex.getRegionAtIgnoringY(ChunkPos(chunkX + 1, chunkZ)), // EAST
            plugin.spatialIndex.getRegionAtIgnoringY(ChunkPos(chunkX - 1, chunkZ)), // WEST
        )
    }

    /**
     * Gets the town ID for a given region.
     *
     * @param regionId The region ID to look up
     * @return The town ID that owns the region, or null if not found
     */
    fun getTownIdForRegion(regionId: RegionId?): TownId? {
        if (regionId == null) return null
        val region = plugin.regionCache.getSync(regionId) ?: return null
        return region.townId
    }

    /**
     * Checks if a pixel position is on a border between different territories.
     *
     * @param ix The pixel X coordinate
     * @param iz The pixel Z coordinate
     * @param imageWidth The image width
     * @param imageHeight The image height
     * @param nearbyRegions Array of neighboring region IDs
     * @param currentTownId The current chunk's town ID
     * @return true if the pixel is on a border
     */
    fun isBorder(
        ix: Int,
        iz: Int,
        imageWidth: Int,
        imageHeight: Int,
        nearbyRegions: Array<RegionId?>,
        currentTownId: TownId?
    ): Boolean {
        val borderSize = 2
        val isWestBorder = ix <= borderSize && (nearbyRegions[3] == null || getTownIdForRegion(nearbyRegions[3]) != currentTownId)
        val isEastBorder = ix >= imageWidth - borderSize - 1 && (nearbyRegions[2] == null || getTownIdForRegion(nearbyRegions[2]) != currentTownId)
        val isNorthBorder = iz <= borderSize && (nearbyRegions[1] == null || getTownIdForRegion(nearbyRegions[1]) != currentTownId)
        val isSouthBorder = iz >= imageHeight - borderSize - 1 && (nearbyRegions[0] == null || getTownIdForRegion(nearbyRegions[0]) != currentTownId)

        return isWestBorder || isEastBorder || isNorthBorder || isSouthBorder
    }
}
