package it.cava99.towny3d.config

/**
 * Economy-related configuration settings.
 *
 * @property townCost BASE cost to create a new town (the TOTAL cost is: townCost + homeRegionHeight × layerCost)
 *                     With defaults: 1000 + (384 × 10) = $4840 total
 * @property layerCost Cost per claimed layer (Y-range)
 * @property layersPerPlayer Bonus layers per resident
 * @property defaultTownBalance Starting balance for new towns
 * @property homeRegionHeight Default height for home regions (-64 to 320)
 * @property nationCreationCost Cost to create a nation
 * @property enableEconomy Whether the economy system is enabled
 * @property enablePlotSales Whether plot sales are enabled
 */
data class EconomyConfig(
    val townCost: Double = 1000.0,
    val layerCost: Double = 10.0,
    val layersPerPlayer: Int = 3840,
    val defaultTownBalance: Double = 0.0,
    val homeRegionHeight: Int = 384,
    val nationCreationCost: Double = 10000.0,
    val enableEconomy: Boolean = true,
    val enablePlotSales: Boolean = true
) {
    init {
        require(townCost >= 0) { "Town cost cannot be negative" }
        require(layerCost >= 0) { "Layer cost cannot be negative" }
        require(layersPerPlayer >= 0) { "Layers per player cannot be negative" }
        require(defaultTownBalance >= 0) { "Default balance cannot be negative" }
        require(homeRegionHeight > 0) { "Home region height must be positive" }
        require(nationCreationCost >= 0) { "Nation creation cost cannot be negative" }
    }

    /**
     * Calculates the max layers for a town based on resident count.
     */
    fun calculateMaxLayers(residentCount: Int): Int {
        return 256 + (residentCount * layersPerPlayer)
    }

    /**
     * Calculates the cost to claim a specific height range.
     */
    fun calculateClaimCost(height: Int): Double {
        return height * layerCost
    }

    /**
     * Calculates the total cost to create a town with home region.
     *
     * Formula: townCost + (homeRegionHeight × layerCost)
     * Example with defaults: 1000 + (384 × 10) = $4840
     *
     * This includes the base town creation cost PLUS the cost of claiming
     * the initial home region (384 layers from bedrock to sky limit).
     */
    fun calculateTownCreationCost(): Double {
        return townCost + (homeRegionHeight * layerCost)
    }
}
