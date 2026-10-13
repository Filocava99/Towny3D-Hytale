package it.cava99.towny3d.service.tax

import it.cava99.towny3d.config.TownyConfig
import kotlinx.coroutines.CoroutineScope
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Service for collecting taxes and managing the tax collection cycle.
 *
 * This service orchestrates the tax collection process by delegating to
 * specialized collectors:
 * - [TownUpkeepCollector] - Town upkeep collection (towns pay server)
 * - [NationUpkeepCollector] - Nation upkeep collection (nations pay server)
 * - [NationTaxCollector] - Nation tax collection (towns pay nations)
 * - [TownTaxCollector] - Town tax collection (residents pay towns)
 * - [PlotTaxCollector] - Plot tax collection (plot owners pay towns)
 * - [ReclaimService] - Reclaiming ruined towns and nations
 *
 * Collection order:
 * 1. Town upkeep (towns pay server, ruined if can't pay)
 * 2. Nation upkeep (nations pay server, ruined if can't pay)
 * 3. Nation taxes (towns pay nations, goes to debt if can't pay)
 * 4. Town taxes (residents pay towns, skipped if can't pay)
 * 5. Plot taxes (plot owners pay towns, lose ownership if can't pay)
 *
 * @property townUpkeepCollector Collector for town upkeep payments
 * @property nationUpkeepCollector Collector for nation upkeep payments
 * @property nationTaxCollector Collector for nation taxes from towns
 * @property townTaxCollector Collector for town taxes from residents
 * @property plotTaxCollector Collector for plot taxes from owners
 * @property reclaimService Service for reclaiming ruined towns/nations
 * @property config Plugin configuration
 */
class TaxCollectionService(
    private val townUpkeepCollector: TownUpkeepCollector,
    private val nationUpkeepCollector: NationUpkeepCollector,
    private val nationTaxCollector: NationTaxCollector,
    private val townTaxCollector: TownTaxCollector,
    private val plotTaxCollector: PlotTaxCollector,
    val reclaimService: ReclaimService,
    private val config: TownyConfig
) {

    /**
     * The timestamp of the last tax collection.
     */
    @Volatile
    private var lastCollectionTime: Instant = Instant.now()

    /**
     * Collects all taxes in the correct order.
     *
     * Collection order:
     * 1. Town upkeep (towns pay server, ruined if can't pay)
     * 2. Nation upkeep (nations pay server, ruined if can't pay)
     * 3. Nation taxes (towns pay nations, goes to debt if can't pay)
     * 4. Town taxes (residents pay towns, skipped if can't pay)
     * 5. Plot taxes (plot owners pay towns, lose ownership if can't pay)
     *
     * @return TaxCollectionResult containing all collection results
     */
    suspend fun collectAllTaxes(): TaxCollectionResult {
        val timestamp = Instant.now()

        // Step 1: Collect town upkeep
        val townUpkeepResults = townUpkeepCollector.collect()

        // Step 2: Collect nation upkeep
        val nationUpkeepResults = nationUpkeepCollector.collect()

        // Step 3: Collect nation taxes from towns
        val nationTaxResults = nationTaxCollector.collect()

        // Step 4: Collect town taxes from residents
        val townTaxResults = townTaxCollector.collect()

        // Step 5: Collect plot taxes
        val plotTaxResults = plotTaxCollector.collect()

        // Update last collection time
        lastCollectionTime = timestamp

        return TaxCollectionResult(
            timestamp = timestamp,
            townUpkeepResults = townUpkeepResults,
            nationUpkeepResults = nationUpkeepResults,
            nationTaxResults = nationTaxResults,
            townTaxResults = townTaxResults,
            plotTaxResults = plotTaxResults
        )
    }

    /**
     * Calculates when the next tax collection should occur.
     *
     * The collection time is based on the configured upkeepTimeOfDay.
     * If that time has already passed today, the next collection is tomorrow.
     *
     * @return Instant when the next tax collection should occur
     */
    fun getNextCollectionTime(): Instant {
        val now = LocalDateTime.now()
        val collectionTime = LocalTime.parse(config.upkeepTimeOfDay)

        var nextCollection = LocalDateTime.of(now.toLocalDate(), collectionTime)

        // If the collection time has already passed today, schedule for tomorrow
        if (now.isAfter(nextCollection)) {
            nextCollection = nextCollection.plusDays(1)
        }

        return nextCollection.atZone(ZoneId.systemDefault()).toInstant()
    }

    /**
     * Checks if it's time to collect taxes and does so if needed.
     *
     * This method should be called periodically (e.g., every minute).
     * It will only trigger collection once per day at the configured time.
     *
     * @return TaxCollectionResult if collection occurred, null otherwise
     */
    suspend fun checkAndCollectIfNeeded(): TaxCollectionResult? {
        val now = Instant.now()
        val nextCollection = getNextCollectionTime()

        // Calculate the time until next collection
        val timeUntilNext = java.time.Duration.between(now, nextCollection).toMinutes()

        // If we're within 1 minute of collection time and haven't collected recently
        val hoursSinceLastCollection = java.time.Duration.between(
            lastCollectionTime,
            now
        ).toHours()

        if (timeUntilNext <= 1 && hoursSinceLastCollection >= config.taxCollectionIntervalHours.toLong() - 1) {
            return collectAllTaxes()
        }

        return null
    }
}
