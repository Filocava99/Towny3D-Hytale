package it.cava99.towny3d.model

/**
 * Represents the type of a region/plot.
 */
enum class RegionType {
    /**
     * A normal region owned by the town.
     * Residents can build/destroy based on town permissions.
     */
    NORMAL,

    /**
     * An embassy region owned by a specific player from another town.
     * The owner has full control, but the region still belongs to a town.
     */
    EMBASSY,

    /**
     * The home region of a town.
     * Automatically created when a town is founded.
     * Contains the town spawn point.
     */
    HOME
}
