package it.cava99.towny3d.service.tax

import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.economy.EconomyService
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Collector for town upkeep payments.
 *
 * Towns pay upkeep to the server. Towns that can't pay become ruined.
 *
 * @property townCache Cache for town data
 * @property economyService Economy service for balance operations
 * @property config Plugin configuration
 * @property pluginScope Coroutine scope for async operations
 * @property batchWriter Batch storage writer for persisting changes
 */
class TownUpkeepCollector(
    private val townCache: TownCache,
    private val economyService: EconomyService,
    private val config: TownyConfig,
    private val pluginScope: CoroutineScope,
    private val batchWriter: BatchStorageWriter
) {
    /**
     * Collects upkeep from all towns.
     * Towns that can't pay become ruined.
     *
     * @return List of town upkeep results
     */
    suspend fun collect(): List<TownUpkeepResult> {
        if (!config.enableTownUpkeep || config.townUpkeep <= 0) {
            return emptyList()
        }

        val results = mutableListOf<TownUpkeepResult>()
        val towns = townCache.getAllCached()

        for (town in towns) {
            // Skip ruined towns
            if (town.isRuined) continue

            val amount = config.townUpkeep
            val couldPay = economyService.hasTownBalance(town.id, amount)

            if (couldPay) {
                economyService.withdrawFromTown(town.id, amount)
            } else {
                // Mark town as ruined
                val ruinedTown = town.withModification(
                    isRuined = true,
                    ruinedAt = Instant.now()
                )
                townCache.put(ruinedTown)
                pluginScope.launch {
                    batchWriter.saveTown(ruinedTown)
                }
            }

            results.add(
                TownUpkeepResult(
                    townId = town.id,
                    townName = town.name,
                    couldPay = couldPay,
                    amount = amount
                )
            )
        }

        return results
    }
}
