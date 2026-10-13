package it.cava99.towny3d.service.tax

import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import java.time.Instant

/**
 * Result of a town's upkeep payment attempt.
 *
 * @property townId The town's ID
 * @property townName The town's name
 * @property couldPay Whether the town could pay the upkeep
 * @property amount The upkeep amount
 */
data class TownUpkeepResult(
    val townId: TownId,
    val townName: String,
    val couldPay: Boolean,
    val amount: Double
)

/**
 * Result of a nation's upkeep payment attempt.
 *
 * @property nationId The nation's ID
 * @property nationName The nation's name
 * @property couldPay Whether the nation could pay the upkeep
 * @property amount The upkeep amount
 */
data class NationUpkeepResult(
    val nationId: NationId,
    val nationName: String,
    val couldPay: Boolean,
    val amount: Double
)

/**
 * Result of a town's nation tax payment attempt.
 *
 * @property townId The town's ID
 * @property townName The town's name
 * @property nationId The nation's ID
 * @property nationName The nation's name
 * @property couldPay Whether the town could pay the tax
 * @property amount The tax amount
 * @property wentToDebt Whether the town went into debt (negative balance) from this payment
 */
data class NationTaxResult(
    val townId: TownId,
    val townName: String,
    val nationId: NationId,
    val nationName: String,
    val couldPay: Boolean,
    val amount: Double,
    val wentToDebt: Boolean
)

/**
 * Result of a resident's town tax payment attempt.
 *
 * @property residentId The resident's ID
 * @property residentName The resident's name
 * @property townId The town's ID
 * @property townName The town's name
 * @property couldPay Whether the resident could pay the tax
 * @property amount The tax amount
 */
data class TownTaxResult(
    val residentId: ResidentId,
    val residentName: String,
    val townId: TownId,
    val townName: String,
    val couldPay: Boolean,
    val amount: Double
)

/**
 * Result of a plot tax payment attempt.
 *
 * @property regionId The plot/region's ID
 * @property residentId The owner's resident ID (null if unowned)
 * @property residentName The owner's name (null if unowned)
 * @property townId The town's ID that owns the plot
 * @property townName The town's name
 * @property couldPay Whether the owner could pay the tax
 * @property amount The tax amount
 * @property lostOwnership Whether the owner lost ownership of the plot due to non-payment
 */
data class PlotTaxResult(
    val regionId: RegionId,
    val residentId: ResidentId?,
    val residentName: String?,
    val townId: TownId,
    val townName: String,
    val couldPay: Boolean,
    val amount: Double,
    val lostOwnership: Boolean
)

/**
 * Complete result of a tax collection cycle.
 *
 * @property timestamp When the tax collection occurred
 * @property townUpkeepResults Results of town upkeep payments
 * @property nationUpkeepResults Results of nation upkeep payments
 * @property nationTaxResults Results of nation tax collections from towns
 * @property townTaxResults Results of town tax collections from residents
 * @property plotTaxResults Results of plot tax collections from residents
 */
data class TaxCollectionResult(
    val timestamp: Instant,
    val townUpkeepResults: List<TownUpkeepResult>,
    val nationUpkeepResults: List<NationUpkeepResult>,
    val nationTaxResults: List<NationTaxResult>,
    val townTaxResults: List<TownTaxResult>,
    val plotTaxResults: List<PlotTaxResult>
) {
    /**
     * Returns the total amount collected from all taxes.
     */
    val totalCollected: Double
        get() = nationTaxResults.filter { it.couldPay }.sumOf { it.amount } +
                 townTaxResults.filter { it.couldPay }.sumOf { it.amount } +
                 plotTaxResults.filter { it.couldPay }.sumOf { it.amount }

    /**
     * Returns the total amount lost from failed upkeep payments.
     */
    val totalUpkeepLost: Double
        get() = townUpkeepResults.filter { !it.couldPay }.sumOf { it.amount } +
                 nationUpkeepResults.filter { !it.couldPay }.sumOf { it.amount }

    /**
     * Returns the number of towns that couldn't pay upkeep.
     */
    val failedTownUpkeepCount: Int
        get() = townUpkeepResults.count { !it.couldPay }

    /**
     * Returns the number of nations that couldn't pay upkeep.
     */
    val failedNationUpkeepCount: Int
        get() = nationUpkeepResults.count { !it.couldPay }

    /**
     * Returns the number of plots lost due to unpaid tax.
     */
    val lostPlotCount: Int
        get() = plotTaxResults.count { it.lostOwnership }
}

/**
 * Result of a reclaim operation for a ruined town or nation.
 *
 * @property success Whether the reclaim was successful
 * @property message A message describing the result
 */
data class ReclaimResult(
    val success: Boolean,
    val message: String
)
