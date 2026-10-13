package it.cava99.towny3d.util

import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.TaxType

/**
 * Helper object for tax calculations and validation.
 */
object TaxCalculator {

    /**
     * Calculates the tax amount based on the tax configuration and balance.
     *
     * @param amount The base amount (unused for FLAT type, used as multiplier for PERCENTAGE)
     * @param config The tax configuration
     * @param balance The balance to calculate percentage tax from
     * @return The calculated tax amount. Returns 0.0 if tax is disabled.
     */
    fun calculateTax(amount: Double, config: TaxConfig, balance: Double): Double {
        if (!config.enabled) {
            return 0.0
        }

        return when (config.type) {
            TaxType.FLAT -> config.amount
            TaxType.PERCENTAGE -> (balance * config.amount / 100.0)
        }
    }

    /**
     * Validates that a tax configuration is within acceptable bounds.
     *
     * @param config The tax configuration to validate
     * @param maxPercent The maximum allowed percentage (typically 100.0)
     * @return true if the configuration is valid, false otherwise
     */
    fun validateTaxConfig(config: TaxConfig, maxPercent: Double = 100.0): Boolean {
        if (!config.enabled) {
            return true
        }

        return when (config.type) {
            TaxType.FLAT -> config.amount >= 0
            TaxType.PERCENTAGE -> config.amount >= 0 && config.amount <= maxPercent
        }
    }
}
