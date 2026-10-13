package it.cava99.towny3d.service.tax

import it.cava99.towny3d.cache.NationCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.economy.EconomyService
import it.cava99.towny3d.storage.BatchStorageWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Collector for nation upkeep payments.
 *
 * Nations pay upkeep to the server. Nations that can't pay become ruined.
 *
 * @property nationCache Cache for nation data
 * @property economyService Economy service for balance operations
 * @property config Plugin configuration
 * @property pluginScope Coroutine scope for async operations
 * @property batchWriter Batch storage writer for persisting changes
 */
class NationUpkeepCollector(
    private val nationCache: NationCache,
    private val economyService: EconomyService,
    private val config: TownyConfig,
    private val pluginScope: CoroutineScope,
    private val batchWriter: BatchStorageWriter
) {
    /**
     * Collects upkeep from all nations.
     * Nations that can't pay become ruined.
     *
     * @return List of nation upkeep results
     */
    suspend fun collect(): List<NationUpkeepResult> {
        if (!config.enableNationUpkeep || config.nationUpkeep <= 0) {
            return emptyList()
        }

        val results = mutableListOf<NationUpkeepResult>()
        val nations = nationCache.getAllCached()

        for (nation in nations) {
            // Skip ruined nations
            if (nation.isRuined) continue

            val amount = config.nationUpkeep
            val couldPay = economyService.hasNationBalance(nation.id, amount)

            if (couldPay) {
                economyService.withdrawFromNation(nation.id, amount)
            } else {
                // Mark nation as ruined
                val ruinedNation = nation.withModification(
                    isRuined = true,
                    ruinedAt = Instant.now()
                )
                nationCache.put(ruinedNation)
                pluginScope.launch {
                    batchWriter.saveNation(ruinedNation)
                }
            }

            results.add(
                NationUpkeepResult(
                    nationId = nation.id,
                    nationName = nation.name,
                    couldPay = couldPay,
                    amount = amount
                )
            )
        }

        return results
    }
}
