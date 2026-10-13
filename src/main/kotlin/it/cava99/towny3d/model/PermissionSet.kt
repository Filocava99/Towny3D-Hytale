package it.cava99.towny3d.model

/**
 * Represents a set of permissions for a region.
 * Uses bitmask storage for efficiency (24 bytes vs 99 booleans in Towny3D).
 *
 * Each permission group (resident, ally, outsider) has a Long mask where each bit
 * represents whether a specific permission is granted.
 *
 * @property residentMask Permission mask for town residents
 * @property allyMask Permission mask for allied players
 * @property outsiderMask Permission mask for outsiders
 */
data class PermissionSet(
    val residentMask: Long = 0L,
    val allyMask: Long = 0L,
    val outsiderMask: Long = 0L
) {
    /**
     * Checks if a specific permission is granted for a player group.
     *
     * @param group The player group to check
     * @param type The permission type to check
     * @return true if the permission is granted, false otherwise
     */
    fun has(group: PermissionGroup, type: PermissionType): Boolean {
        val mask = when (group) {
            PermissionGroup.RESIDENT -> residentMask
            PermissionGroup.ALLY -> allyMask
            PermissionGroup.OUTSIDER -> outsiderMask
        }
        return (mask and type.bit) != 0L
    }

    /**
     * Sets a permission for a player group.
     *
     * @param group The player group to modify
     * @param type The permission type to set
     * @param value true to grant, false to revoke
     * @return A new PermissionSet with the modified permission
     */
    fun set(group: PermissionGroup, type: PermissionType, value: Boolean): PermissionSet {
        val mask = when (group) {
            PermissionGroup.RESIDENT -> residentMask
            PermissionGroup.ALLY -> allyMask
            PermissionGroup.OUTSIDER -> outsiderMask
        }

        val newMask = if (value) {
            mask or type.bit
        } else {
            mask and type.bit.inv()
        }

        return when (group) {
            PermissionGroup.RESIDENT -> copy(residentMask = newMask)
            PermissionGroup.ALLY -> copy(allyMask = newMask)
            PermissionGroup.OUTSIDER -> copy(outsiderMask = newMask)
        }
    }

    /**
     * Creates a builder for constructing PermissionSets.
     */
    companion object {
        /**
         * Default permissions for town regions.
         * Residents can build/destroy, outsiders cannot.
         */
        fun defaultRegion() = PermissionSet(
            residentMask = PermissionType.BUILD.bit or
                           PermissionType.DESTROY.bit or
                           PermissionType.INTERACT.bit,
            allyMask = PermissionType.INTERACT.bit,
            outsiderMask = 0L
        )

        /**
         * Default permissions for embassy regions.
         * Only the plot owner can build/destroy.
         */
        fun defaultEmbassy() = PermissionSet(
            residentMask = PermissionType.INTERACT.bit,
            allyMask = PermissionType.INTERACT.bit,
            outsiderMask = PermissionType.INTERACT.bit
        )

        /**
         * Default permissions for home regions.
         */
        fun defaultHome() = defaultRegion()

        /**
         * All permissions granted.
         */
        fun all() = PermissionSet(
            residentMask = PermissionType.entries.fold(0L) { acc, p -> acc or p.bit },
            allyMask = PermissionType.entries.fold(0L) { acc, p -> acc or p.bit },
            outsiderMask = PermissionType.entries.fold(0L) { acc, p -> acc or p.bit }
        )

        /**
         * No permissions granted.
         */
        fun none() = PermissionSet(0L, 0L, 0L)
    }
}
