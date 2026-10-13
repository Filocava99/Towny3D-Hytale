package it.cava99.towny3d.model

/**
 * Represents the relationship of a player to a region.
 * Used for permission checking.
 */
enum class PlayerType {
    /**
     * The player owns this specific plot/region.
     * Owners have all permissions regardless of other settings.
     */
    OWNER,

    /**
     * The player is a resident of the town that owns this region.
     */
    RESIDENT,

    /**
     * The player is in a town that is allied with the region's town (same nation).
     */
    ALLY,

    /**
     * The player has no special relationship to the region (outsider/wilderness).
     */
    OUTSIDER
}
