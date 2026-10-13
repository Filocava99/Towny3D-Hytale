package it.cava99.towny3d.listener

import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.PermissionType
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Event listener for PvP (Player vs Player) combat.
 * Controls whether players can attack each other based on region settings.
 */
class PvPProtectionListener(
    private val deps: ProtectionDependencies
) {
    // Cache of recent PvP violations to prevent spam
    private val violationCache = mutableMapOf<UUID, Long>()
    private val VIOLATION_COOLDOWN = 5000L // 5 seconds

    /**
     * Handles player attacking another player.
     * Returns true if attack should be cancelled.
     */
    fun onPlayerAttack(
        attackerUuid: UUID,
        attackerName: String,
        victimUuid: UUID,
        victimName: String,
        world: String,
        x: Double,
        y: Double,
        z: Double
    ): Boolean {
        // Check cooldown to prevent message spam
        val now = System.currentTimeMillis()
        val lastViolation = violationCache[attackerUuid]

        if (lastViolation != null && now - lastViolation < VIOLATION_COOLDOWN) {
            return true // Still on cooldown, block attack silently
        }

        val location = BlockLocation(world, x.toInt(), y.toInt(), z.toInt())

        // Check if PvP is allowed at this location
        val allowed = isPvPAllowed(location, attackerUuid, victimUuid)

        if (!allowed) {
            violationCache[attackerUuid] = now
            // Log protection event
            TownyLog.info("Blocked PvP: $attackerName attacked $victimName at ($x, $y, $z)")
            // TODO(2026-02-22): Send message to attacker when player messaging is implemented
            // Example: sendMessageToPlayer(attackerUuid, "PvP is disabled in this area")
            return true // Cancel attack
        }

        return false // Allow attack
    }

    /**
     * Handles projectile hits (arrows, snowballs, etc.).
     * Returns true if projectile should be cancelled.
     */
    fun onProjectileHit(
        shooterUuid: UUID?,
        shooterName: String?,
        victimUuid: UUID,
        victimName: String,
        world: String,
        x: Double,
        y: Double,
        z: Double,
        projectileType: String
    ): Boolean {
        // If no shooter (dispenser, etc.), allow
        if (shooterUuid == null) {
            return false
        }

        val location = BlockLocation(world, x.toInt(), y.toInt(), z.toInt())

        // Check if PvP is allowed
        val allowed = isPvPAllowed(location, shooterUuid, victimUuid)

        if (!allowed) {
            // Log protection event
            TownyLog.info("Blocked projectile PvP: $shooterName hit $victimName with $projectileType at ($x, $y, $z)")
            // TODO(2026-02-22): Send message to shooter when player messaging is implemented
            return true // Cancel projectile
        }

        return false
    }

    /**
     * Checks if PvP is allowed at a location between two players.
     */
    private fun isPvPAllowed(
        location: BlockLocation,
        attackerUuid: UUID,
        victimUuid: UUID
    ): Boolean {
        val regionId = deps.spatialIndex.getRegionAt(location)

        // No region = wilderness, PvP depends on config
        if (regionId == null) {
            return true // Wilderness allows PvP by default
        }

        // Check if any region allows PvP
        // TODO(2026-02-22): Check town's PvP toggle flag
        // For now, allow PvP in all regions
        val pvpEnabledInRegions = true

        if (!pvpEnabledInRegions) {
            return false // PvP is disabled in all regions
        }

        // Check if players are in the same town (optional: disable same-town PvP)
        val attackerResident = deps.residentCache.getSync(
            it.cava99.towny3d.model.ResidentId.fromPlayerUuid(attackerUuid)
        )
        val victimResident = deps.residentCache.getSync(
            it.cava99.towny3d.model.ResidentId.fromPlayerUuid(victimUuid)
        )

        if (attackerResident != null && victimResident != null) {
            // Same town?
            if (attackerResident.townId != null &&
                attackerResident.townId == victimResident.townId) {
                // Check if same-town PvP is enabled
                // TODO(2026-02-22): Make this configurable
                return false // Same-town PvP disabled by default
            }

            // Allied towns?
            if (attackerResident.townId != null && victimResident.townId != null) {
                val attackerTown = deps.townCache.getSync(attackerResident.townId)
                val victimTown = deps.townCache.getSync(victimResident.townId)

                if (attackerTown?.nationId != null &&
                    attackerTown.nationId == victimTown?.nationId) {
                    // Allied nations, no PvP
                    return false
                }
            }
        }

        return true
    }

    /**
     * Checks if a location is a PvP-safe zone.
     * Useful for other systems to check before initiating combat.
     */
    fun isPvPSafeZone(world: String, x: Int, y: Int, z: Int): Boolean {
        val location = BlockLocation(world, x, y, z)
        val regionId = deps.spatialIndex.getRegionAt(location)

        if (regionId == null) {
            return false // Wilderness is not a safe zone
        }

        // If any region has PvP disabled, it's a safe zone
        // TODO(2026-02-22): Check town's PvP toggle flag
        return false
    }

    /**
     * Gets PvP status at a location for display purposes.
     */
    fun getPvPStatus(world: String, x: Int, y: Int, z: Int): PvPStatus {
        val location = BlockLocation(world, x, y, z)
        val regionId = deps.spatialIndex.getRegionAt(location)

        if (regionId == null) {
            return PvPStatus.WILDERNESS
        }

        // TODO(2026-02-22): Check town's PvP toggle flag
        return PvPStatus.ENABLED
    }

    /**
     * Represents the PvP status at a location.
     */
    enum class PvPStatus {
        WILDERNESS,
        ENABLED,
        DISABLED
    }

    /**
     * Cleans up old violation entries.
     */
    fun cleanupViolations() {
        val now = System.currentTimeMillis()
        violationCache.entries.removeIf { now - it.value > VIOLATION_COOLDOWN }
    }
}
