package it.cava99.towny3d.config

import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.TaxType

/**
 * Configuration data class for Towny plugin settings.
 *
 * This class composes focused configuration classes for better organization:
 * - EconomyConfig: economy-related settings (town costs, claim costs, etc.)
 * - TaxSettings: tax and upkeep settings
 * - WorldSettings: world and claim-related settings
 *
 * @property economy Economy-related configuration
 * @property tax Tax and upkeep configuration
 * @property world World and claim configuration
 * @property maxTownNameLength Maximum characters in town names
 * @property maxNationNameLength Maximum characters in nation names
 * @property maxResidentNameLength Maximum characters in resident names
 * @property autoSaveIntervalSeconds Interval between auto-saves
 * @property cacheCleanupIntervalMinutes Interval between cache cleanups
 * @property cacheCleanupEntriesRemoved Number of entries to remove during cleanup
 * @property defaultLanguage Default language locale for the server (BCP 47 format, e.g., "en-US")
 * @property enableNations Whether the nation system is enabled
 * @property enableTownChat Whether town chat is enabled
 * @property enableNationChat Whether nation chat is enabled
 * @property version Config version for migration purposes
 */
data class TownyConfig(
    // Composed configuration classes
    val economy: EconomyConfig = EconomyConfig(),
    val tax: TaxSettings = TaxSettings(),
    val world: WorldSettings = WorldSettings(),

    // Name validation
    val maxTownNameLength: Int = 32,
    val maxNationNameLength: Int = 32,
    val maxResidentNameLength: Int = 16,

    // Maintenance settings
    val autoSaveIntervalSeconds: Int = 300, // 5 minutes
    val cacheCleanupIntervalMinutes: Int = 10,
    val cacheCleanupEntriesRemoved: Int = 100,

    // Feature flags
    val enableNations: Boolean = true,
    val version: String = "1.0.0",
    val defaultLanguage: String = "en-US",
    val enableTownChat: Boolean = true,
    val enableNationChat: Boolean = true
) {
    init {
        require(defaultLanguage.matches(Regex("^[a-z]{2}-[A-Z]{2}$"))) {
            "Invalid language tag format: '$defaultLanguage'. Expected BCP 47 format (e.g., 'en-US')"
        }
        require(autoSaveIntervalSeconds > 0) { "Auto-save interval must be positive" }
        require(cacheCleanupIntervalMinutes > 0) { "Cache cleanup interval must be positive" }
    }

    // ===== Backward compatibility delegate properties =====
    // These delegate to the composed config classes for backward compatibility

    // Economy delegates
    val townCost get() = economy.townCost
    val layerCost get() = economy.layerCost
    val layersPerPlayer get() = economy.layersPerPlayer
    val defaultTownBalance get() = economy.defaultTownBalance
    val homeRegionHeight get() = economy.homeRegionHeight
    val nationCreationCost get() = economy.nationCreationCost
    val enableEconomy get() = economy.enableEconomy
    val enablePlotSales get() = economy.enablePlotSales

    // World delegates
    val maxClaimHeight get() = world.maxClaimHeight
    val claimCooldownMs get() = world.claimCooldownMs
    val defaultWorldName get() = world.defaultWorldName

    // Tax/upkeep delegates
    val townUpkeep get() = tax.townUpkeep
    val nationUpkeep get() = tax.nationUpkeep
    val enableTownUpkeep get() = tax.enableTownUpkeep
    val enableNationUpkeep get() = tax.enableNationUpkeep
    val taxCollectionIntervalHours get() = tax.taxCollectionIntervalHours
    val defaultTownTaxEnabled get() = tax.defaultTownTaxEnabled
    val defaultTownTaxType get() = tax.defaultTownTaxType
    val defaultTownTaxAmount get() = tax.defaultTownTaxAmount
    val defaultNationTaxEnabled get() = tax.defaultNationTaxEnabled
    val defaultNationTaxType get() = tax.defaultNationTaxType
    val defaultNationTaxAmount get() = tax.defaultNationTaxAmount
    val upkeepTimeOfDay get() = tax.upkeepTimeOfDay
    val reclaimFeeMultiplier get() = tax.reclaimFeeMultiplier
    val ruinedDeletionDays get() = tax.ruinedDeletionDays
    val maxNationTaxPercent get() = tax.maxNationTaxPercent
    val maxTownTaxPercent get() = tax.maxTownTaxPercent
    val maxPlotTaxPercent get() = tax.maxPlotTaxPercent
    val taxWarningMinutesBefore get() = tax.taxWarningMinutesBefore
    val taxWarningMessage get() = tax.taxWarningMessage
    val taxSummaryMessageEnabled get() = tax.taxSummaryMessageEnabled
    val maxNationDebtMultiplier get() = tax.maxNationDebtMultiplier
    val allowTownLeaveWithDebt get() = tax.allowTownLeaveWithDebt

    /**
     * Calculates the max layers for a town based on resident count.
     * Delegates to EconomyConfig.
     */
    fun calculateMaxLayers(residentCount: Int): Int {
        return economy.calculateMaxLayers(residentCount)
    }

    /**
     * Calculates the cost to claim a specific height range.
     * Delegates to EconomyConfig.
     */
    fun calculateClaimCost(height: Int): Double {
        return economy.calculateClaimCost(height)
    }

    /**
     * Calculates the total cost to create a town with home region.
     * Delegates to EconomyConfig.
     *
     * Formula: townCost + (homeRegionHeight × layerCost)
     * Example with defaults: 1000 + (384 × 10) = $4840
     *
     * This includes the base town creation cost PLUS the cost of claiming
     * the initial home region (384 layers from bedrock to sky limit).
     */
    fun calculateTownCreationCost(): Double {
        return economy.calculateTownCreationCost()
    }

    /**
     * Gets the default tax config for new towns.
     * Delegates to TaxSettings.
     */
    fun getDefaultTownTaxConfig(): TaxConfig {
        return tax.getDefaultTownTaxConfig()
    }

    /**
     * Gets the default tax config for new nations.
     * Delegates to TaxSettings.
     */
    fun getDefaultNationTaxConfig(): TaxConfig {
        return tax.getDefaultNationTaxConfig()
    }

    companion object {
        /**
         * Default configuration instance.
         */
        fun default() = TownyConfig()

        /**
         * Creates a config for testing (lower costs, faster timers).
         */
        fun testing() = TownyConfig(
            economy = EconomyConfig(
                townCost = 10.0,
                layerCost = 1.0,
                layersPerPlayer = 5,
                defaultTownBalance = 100.0,
                homeRegionHeight = 64,
                nationCreationCost = 50.0
            ),
            world = WorldSettings(
                claimCooldownMs = 100L
            ),
            autoSaveIntervalSeconds = 60,
            cacheCleanupIntervalMinutes = 1,
            defaultLanguage = "en-US"
        )
    }
}
