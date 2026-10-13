package it.cava99.towny3d.config

import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.TaxType

/**
 * Tax and upkeep-related configuration settings.
 *
 * @property townUpkeep Daily cost towns must pay to stay alive
 * @property nationUpkeep Daily cost nations must pay to stay alive
 * @property enableTownUpkeep Whether towns must pay daily upkeep
 * @property enableNationUpkeep Whether nations must pay daily upkeep
 * @property taxCollectionIntervalHours How often taxes are collected (in hours)
 * @property defaultTownTaxEnabled Default tax enabled state for new towns
 * @property defaultTownTaxType Default tax type for new towns (FLAT or PERCENTAGE)
 * @property defaultTownTaxAmount Default tax amount for new towns
 * @property defaultNationTaxEnabled Default tax enabled state for new nations
 * @property defaultNationTaxType Default tax type for new nations (FLAT or PERCENTAGE)
 * @property defaultNationTaxAmount Default tax amount for new nations
 * @property upkeepTimeOfDay Time of day when upkeep is collected (HH:MM format)
 * @property reclaimFeeMultiplier Multiplier for reclaiming ruined towns/nations
 * @property ruinedDeletionDays Days before ruined towns/nations are deleted
 * @property maxNationTaxPercent Maximum percentage tax nations can charge towns
 * @property maxTownTaxPercent Maximum percentage tax towns can charge residents
 * @property maxPlotTaxPercent Maximum percentage tax for plot taxes
 * @property taxWarningMinutesBefore Minutes before tax collection to send warning
 * @property taxWarningMessage Message sent as tax warning
 * @property taxSummaryMessageEnabled Whether to send tax collection summary
 * @property maxNationDebtMultiplier Maximum debt multiplier for nations
 * @property allowTownLeaveWithDebt Whether residents can leave towns with debt
 */
data class TaxSettings(
    // Upkeep settings
    val townUpkeep: Double = 0.0,
    val nationUpkeep: Double = 0.0,
    val enableTownUpkeep: Boolean = false,
    val enableNationUpkeep: Boolean = false,

    // Tax collection settings
    val taxCollectionIntervalHours: Int = 24,

    // Default town tax settings
    val defaultTownTaxEnabled: Boolean = false,
    val defaultTownTaxType: String = "FLAT",
    val defaultTownTaxAmount: Double = 0.0,

    // Default nation tax settings
    val defaultNationTaxEnabled: Boolean = false,
    val defaultNationTaxType: String = "FLAT",
    val defaultNationTaxAmount: Double = 0.0,

    // Advanced tax settings
    val upkeepTimeOfDay: String = "18:00",
    val reclaimFeeMultiplier: Double = 2.0,
    val ruinedDeletionDays: Int = 30,
    val maxNationTaxPercent: Double = 10.0,
    val maxTownTaxPercent: Double = 25.0,
    val maxPlotTaxPercent: Double = 50.0,
    val taxWarningMinutesBefore: Int = 30,
    val taxWarningMessage: String = "&e[Tax] Taxes will be collected in {time}!",
    val taxSummaryMessageEnabled: Boolean = true,
    val maxNationDebtMultiplier: Double = 5.0,
    val allowTownLeaveWithDebt: Boolean = false
) {
    init {
        // Validate upkeep settings
        require(townUpkeep >= 0) { "Town upkeep cannot be negative" }
        require(nationUpkeep >= 0) { "Nation upkeep cannot be negative" }

        // Validate tax settings
        require(taxCollectionIntervalHours > 0) { "Tax collection interval must be positive" }
        require(defaultTownTaxAmount >= 0) { "Default town tax amount cannot be negative" }
        require(defaultNationTaxAmount >= 0) { "Default nation tax amount cannot be negative" }

        // Validate tax type values
        val validTaxTypes = setOf("FLAT", "PERCENTAGE")
        require(defaultTownTaxType in validTaxTypes) {
            "Invalid town tax type: '$defaultTownTaxType'. Must be FLAT or PERCENTAGE"
        }
        require(defaultNationTaxType in validTaxTypes) {
            "Invalid nation tax type: '$defaultNationTaxType'. Must be FLAT or PERCENTAGE"
        }

        // Validate percentage taxes don't exceed 100%
        if (defaultTownTaxType == "PERCENTAGE") {
            require(defaultTownTaxAmount <= 100) {
                "Town tax percentage cannot exceed 100%, got $defaultTownTaxAmount%"
            }
        }
        if (defaultNationTaxType == "PERCENTAGE") {
            require(defaultNationTaxAmount <= 100) {
                "Nation tax percentage cannot exceed 100%, got $defaultNationTaxAmount%"
            }
        }

        // Validate time format (HH:MM)
        require(upkeepTimeOfDay.matches(Regex("^([01]\\d|2[0-3]):([0-5]\\d)$"))) {
            "Invalid upkeep time format: '$upkeepTimeOfDay'. Expected HH:MM format (e.g., '18:00')"
        }

        // Validate positive multipliers and limits
        require(reclaimFeeMultiplier >= 0) { "Reclaim fee multiplier cannot be negative" }
        require(ruinedDeletionDays > 0) { "Ruined deletion days must be positive" }
        require(maxNationTaxPercent >= 0 && maxNationTaxPercent <= 100) {
            "Max nation tax percent must be between 0 and 100, got $maxNationTaxPercent"
        }
        require(maxTownTaxPercent >= 0 && maxTownTaxPercent <= 100) {
            "Max town tax percent must be between 0 and 100, got $maxTownTaxPercent"
        }
        require(maxPlotTaxPercent >= 0 && maxPlotTaxPercent <= 100) {
            "Max plot tax percent must be between 0 and 100, got $maxPlotTaxPercent"
        }
        require(taxWarningMinutesBefore >= 0) { "Tax warning minutes cannot be negative" }
        require(maxNationDebtMultiplier >= 0) { "Max nation debt multiplier cannot be negative" }
    }

    /**
     * Gets the default tax config for new towns.
     */
    fun getDefaultTownTaxConfig(): TaxConfig {
        return TaxConfig(
            enabled = defaultTownTaxEnabled,
            type = TaxType.valueOf(defaultTownTaxType),
            amount = defaultTownTaxAmount
        )
    }

    /**
     * Gets the default tax config for new nations.
     */
    fun getDefaultNationTaxConfig(): TaxConfig {
        return TaxConfig(
            enabled = defaultNationTaxEnabled,
            type = TaxType.valueOf(defaultNationTaxType),
            amount = defaultNationTaxAmount
        )
    }
}
