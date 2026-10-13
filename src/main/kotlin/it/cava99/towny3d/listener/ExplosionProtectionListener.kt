package it.cava99.towny3d.listener

import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.PermissionType
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Event listener for explosions and fire.
 * Prevents explosions and fire spread in protected regions based on toggle flags.
 */
class ExplosionProtectionListener(
    private val deps: ProtectionDependencies
) {

    /**
     * Handles entity explosions (creepers, TNT, etc.).
     * Returns true if explosion should be cancelled.
     */
    fun onEntityExplosion(
        entityUuid: UUID,
        world: String,
        x: Double,
        y: Double,
        z: Double,
        power: Float,
        affectedBlocks: List<BlockLocation>
    ): Boolean {
        // Check if any affected block is in a protected region
        for (blockLocation in affectedBlocks) {
            if (!shouldAllowExplosion(blockLocation)) {
                TownyLog.info("Cancelled explosion at ($x, $y, $z) - protected region")
                return true // Cancel explosion
            }
        }

        return false // Allow explosion
    }

    /**
     * Handles block explosions (bed, nether portal, etc.).
     * Returns true if explosion should be cancelled.
     */
    fun onBlockExplosion(
        world: String,
        x: Int,
        y: Int,
        z: Int,
        power: Float,
        affectedBlocks: List<BlockLocation>
    ): Boolean {
        for (blockLocation in affectedBlocks) {
            if (!shouldAllowExplosion(blockLocation)) {
                TownyLog.info("Cancelled block explosion at ($x, $y, $z) - protected region")
                return true
            }
        }

        return false
    }

    /**
     * Handles fire spread from one block to another.
     * Returns true if fire spread should be cancelled.
     */
    fun onFireSpread(
        world: String,
        fromX: Int,
        fromY: Int,
        fromZ: Int,
        toX: Int,
        toY: Int,
        toZ: Int
    ): Boolean {
        val toLocation = BlockLocation(world, toX, toY, toZ)

        // Check if fire spread is blocked at destination
        return shouldBlockFireSpread(toLocation)
    }

    /**
     * Checks if an explosion should be allowed at a location.
     */
    private fun shouldAllowExplosion(location: BlockLocation): Boolean {
        val regionId = deps.spatialIndex.getRegionAt(location)

        if (regionId == null) {
            return true // Wilderness - allow explosions
        }

        // Get the region to check its toggle flags
        val region = deps.regionCache.getSync(regionId)

        // If region not found in cache, block explosion (fail-closed for safety)
        if (region == null) {
            return false
        }

        // Allow explosion only if the explosion toggle is enabled
        return region.toggles.explosion
    }

    /**
     * Checks if fire spread should be allowed.
     * Returns true if fire spread should be blocked.
     */
    fun shouldBlockFireSpread(
        world: String,
        x: Int,
        y: Int,
        z: Int
    ): Boolean {
        val location = BlockLocation(world, x, y, z)
        return shouldBlockFireSpread(location)
    }

    /**
     * Checks if fire spread should be blocked at a location.
     * Returns true if fire spread should be blocked.
     */
    private fun shouldBlockFireSpread(location: BlockLocation): Boolean {
        val regionId = deps.spatialIndex.getRegionAt(location)

        if (regionId == null) {
            return false // Wilderness - allow fire
        }

        // Get the region to check its toggle flags
        val region = deps.regionCache.getSync(regionId)

        // If region not found in cache, allow fire (fail-open for safety)
        if (region == null) {
            return false
        }

        // Block fire spread if the fire toggle is disabled
        return !region.toggles.fire
    }
}
