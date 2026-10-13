package it.cava99.towny3d.model

/**
 * Represents the type of tax calculation.
 */
enum class TaxType {
    /** Fixed amount tax (e.g., 100 coins) */
    FLAT,
    /** Percentage-based tax (e.g., 10% of balance) */
    PERCENTAGE
}

/**
 * Configuration for a tax.
 *
 * @property enabled Whether this tax is active
 * @property type The type of tax calculation (flat or percentage)
 * @property amount The tax amount (flat value OR percentage 0-100)
 */
data class TaxConfig(
    val enabled: Boolean = false,
    val type: TaxType = TaxType.FLAT,
    val amount: Double = 0.0
) {
    init {
        require(amount >= 0) { "Tax amount cannot be negative, got $amount" }
        require(
            when (type) {
                TaxType.PERCENTAGE -> amount <= 100
                TaxType.FLAT -> true
            }
        ) { "Percentage tax cannot exceed 100%, got $amount%" }
    }

    /**
     * Creates a disabled tax config.
     */
    companion object {
        fun disabled() = TaxConfig(enabled = false)

        /**
         * Creates a flat tax config.
         */
        fun flat(amount: Double) = TaxConfig(
            enabled = true,
            type = TaxType.FLAT,
            amount = amount
        )

        /**
         * Creates a percentage tax config.
         */
        fun percentage(amount: Double) = TaxConfig(
            enabled = true,
            type = TaxType.PERCENTAGE,
            amount = amount
        )
    }

    /**
     * Returns a copy with tax disabled.
     */
    fun disabled(): TaxConfig = copy(enabled = false)

    /**
     * Returns a copy with the tax cleared (disabled and amount reset).
     */
    fun cleared(): TaxConfig = TaxConfig.disabled()
}
