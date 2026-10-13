package it.cava99.towny3d.config

/**
 * Default permission settings for different region types.
 */
object PermissionDefaults {
    /**
     * Default permissions for town regions.
     */
    val REGION = it.cava99.towny3d.model.PermissionSet.defaultRegion()

    /**
     * Default permissions for embassy regions.
     */
    val EMBASSY = it.cava99.towny3d.model.PermissionSet.defaultEmbassy()

    /**
     * Default permissions for home regions.
     */
    val HOME = it.cava99.towny3d.model.PermissionSet.defaultHome()

    /**
     * Default toggle flags for regions.
     */
    val TOGGLES = it.cava99.towny3d.model.ToggleFlags.default()

    /**
     * Safe toggle flags (fire and explosion disabled).
     */
    val SAFE_TOGGLES = it.cava99.towny3d.model.ToggleFlags.safe()
}
