package it.cava99.towny3d.listener

import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.PermissionType
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Event listener for entity-related protections.
 * Handles mob spawning, animal interactions, and entity damage.
 * Note: This is a simplified version - full entity protection will be implemented later.
 */
class EntityProtectionListener(
    private val deps: ProtectionDependencies
) {
    /**
     * Handles natural mob spawning.
     * Returns true if spawn should be cancelled.
     */
    fun onMobSpawn(
        world: String,
        x: Double,
        y: Double,
        z: Double,
        mobType: String,
        spawnReason: String
    ): Boolean {
        val location = BlockLocation(world, x.toInt(), y.toInt(), z.toInt())
        val chunkPos = ChunkPos(location.x shr 5, location.z shr 5)

        // Get region at this location synchronously
        val region = deps.regionService.getRegionAtSync(chunkPos, location.y)

        if (region == null) {
            return false // Wilderness - allow spawning
        }

        // Determine if mob is hostile or friendly
        val isHostile = isHostileMob(mobType)
        val isFriendly = isPassiveMob(mobType)

        // Check region toggle flags for mob spawning
        // For hostile mobs, check hostileMobsSpawn flag
        if (isHostile && !region.toggles.hostileMobsSpawn) {
            return true // Cancel spawn - hostile mobs disabled
        }

        // For friendly mobs, check friendlyMobsSpawn flag
        if (isFriendly && !region.toggles.friendlyMobsSpawn) {
            return true // Cancel spawn - friendly mobs disabled
        }

        // Allow spawn by default
        return false
    }

    /**
     * Handles player breeding animals.
     * Returns true if breeding should be cancelled.
     */
    fun onAnimalBreed(
        playerUuid: UUID,
        world: String,
        x: Int,
        y: Int,
        z: Int
    ): Boolean {
        // Simplified: Check build permission
        val location = BlockLocation(world, x, y, z)

        // Launch coroutine for permission check
        var allowed = true
        deps.pluginScope.launch {
            allowed = checkPlayerPermission(playerUuid, location, PermissionType.BUILD)
        }

        // For now, just return false (allow) until full async implementation
        return false
    }

    /**
     * Handles player taming animals.
     * Returns true if taming should be cancelled.
     */
    fun onAnimalTame(
        playerUuid: UUID,
        world: String,
        x: Int,
        y: Int,
        z: Int
    ): Boolean {
        // Simplified: Check build permission
        val location = BlockLocation(world, x, y, z)

        // Launch coroutine for permission check
        var allowed = true
        deps.pluginScope.launch {
            allowed = checkPlayerPermission(playerUuid, location, PermissionType.BUILD)
        }

        // For now, just return false (allow) until full async implementation
        return false
    }

    /**
     * Checks if a player has permission at a location.
     */
    private suspend fun checkPlayerPermission(
        playerUuid: UUID,
        location: BlockLocation,
        permissionType: PermissionType
    ): Boolean {
        val chunkPos = ChunkPos(location.x shr 5, location.z shr 5)
        val regions = deps.regionService.getRegionsAt(chunkPos)

        if (regions.isEmpty()) {
            return true // Wilderness - allow everything
        }

        // Check if player has permission through region ownership or town membership
        val resident = deps.residentService.getResidentByUuid(playerUuid)
            ?: return false

        // TODO(2026-02-22): Implement full permission checking
        return true
    }

    /**
     * Determines if a mob is hostile.
     */
    private fun isHostileMob(mobType: String): Boolean {
        val hostileMobs = listOf("zombie", "skeleton", "spider", "creeper", "enderman")
        return hostileMobs.any { mobType.equals(it, ignoreCase = true) }
    }

    /**
     * Determines if a mob is passive.
     */
    private fun isPassiveMob(mobType: String): Boolean {
        val passiveMobs = listOf("cow", "pig", "sheep", "chicken", "rabbit")
        return passiveMobs.any { mobType.equals(it, ignoreCase = true) }
    }
}
