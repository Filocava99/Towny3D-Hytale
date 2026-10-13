package it.cava99.towny3d.config

/**
 * World and claim-related configuration settings.
 *
 * @property defaultWorldName Default world name for the server
 * @property maxClaimHeight Maximum height of a single claim
 * @property claimCooldownMs Cooldown between claim attempts in milliseconds
 */
data class WorldSettings(
    val defaultWorldName: String = "world",
    val maxClaimHeight: Int = 256,
    val claimCooldownMs: Long = 1000L
) {
    init {
        require(maxClaimHeight > 0) { "Max claim height must be positive" }
        require(claimCooldownMs >= 0) { "Claim cooldown cannot be negative" }
    }
}
