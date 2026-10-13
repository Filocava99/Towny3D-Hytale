package it.cava99.towny3d.model

/**
 * Represents a type of permission that can be granted or denied.
 * Uses bitmask values for efficient storage and checking.
 */
enum class PermissionType(val bit: Long, val displayName: String) {
    /**
     * Whether PvP combat is allowed in the region.
     */
    PVP(1L shl 0, "pvp"),

    /**
     * Whether players can open their inventory.
     */
    INVENTORY(1L shl 1, "inventory"),

    /**
     * Whether players can interact with blocks (buttons, doors, etc.).
     */
    INTERACT(1L shl 2, "interact"),

    /**
     * Whether players can place blocks.
     */
    BUILD(1L shl 3, "build"),

    /**
     * Whether players can break blocks.
     */
    DESTROY(1L shl 4, "destroy"),

    /**
     * Whether players can drop items.
     */
    DROP(1L shl 5, "drop"),

    /**
     * Whether players can pick up items.
     */
    PICK(1L shl 6, "pick"),

    /**
     * Whether players can move through the region.
     */
    MOVE(1L shl 7, "move");

    companion object {
        /**
         * Gets a permission type by its display name.
         */
        fun fromDisplayName(name: String): PermissionType? {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}
