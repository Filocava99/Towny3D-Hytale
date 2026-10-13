package it.cava99.towny3d.model

/**
 * Represents a group of players for permission checking.
 * Used in conjunction with PermissionSet to determine if an action is allowed.
 */
enum class PermissionGroup {
    /**
     * Residents of the town that owns the region.
     */
    RESIDENT,

    /**
     * Players from towns allied with the region's town (same nation).
     */
    ALLY,

    /**
     * Players with no special relationship (outsiders).
     */
    OUTSIDER
}
