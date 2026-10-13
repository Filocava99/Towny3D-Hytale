package it.cava99.towny3d.model

/**
 * Represents environment toggle flags for a region.
 * These flags control various environmental behaviors within claimed regions.
 *
 * @property pvp Whether PvP (player vs player) combat is allowed
 * @property fire Whether fire can spread/burn in the region
 * @property explosion Whether explosions can damage blocks in the region
 * @property friendlyMobsSpawn Whether friendly/passive mobs can spawn naturally
 * @property hostileMobsSpawn Whether hostile mobs can spawn naturally
 */
data class ToggleFlags(
    val pvp: Boolean = true,
    val fire: Boolean = true,
    val explosion: Boolean = true,
    val friendlyMobsSpawn: Boolean = true,
    val hostileMobsSpawn: Boolean = true
) {
    /**
     * Creates a builder pattern for toggling flags.
     */
    companion object {
        /**
         * Default toggle flags for new regions.
         * All features enabled by default.
         */
        fun default() = ToggleFlags(
            pvp = true,
            fire = true,
            explosion = true,
            friendlyMobsSpawn = true,
            hostileMobsSpawn = true
        )

        /**
         * Safe toggle flags (PvP, fire and explosions disabled).
         */
        fun safe() = ToggleFlags(
            pvp = false,
            fire = false,
            explosion = false,
            friendlyMobsSpawn = true,
            hostileMobsSpawn = true
        )

        /**
         * All flags disabled.
         */
        fun none() = ToggleFlags(
            pvp = false,
            fire = false,
            explosion = false,
            friendlyMobsSpawn = false,
            hostileMobsSpawn = false
        )
    }

    /**
     * Sets a flag value.
     */
    fun withPvp(value: Boolean) = copy(pvp = value)
    fun withFire(value: Boolean) = copy(fire = value)
    fun withExplosion(value: Boolean) = copy(explosion = value)
    fun withFriendlyMobsSpawn(value: Boolean) = copy(friendlyMobsSpawn = value)
    fun withHostileMobsSpawn(value: Boolean) = copy(hostileMobsSpawn = value)
}
