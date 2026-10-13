package it.cava99.towny3d.service.tax

import it.cava99.towny3d.cache.NationCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.economy.EconomyService
import it.cava99.towny3d.storage.BatchStorageWriter
import it.cava99.towny3d.util.TaxCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Collector for nation taxes from member towns.
 *
 * Towns pay taxes to their nation. Towns that can't pay go into debt
 * (up to a configurable limit).
 *
 * @property nationCache Cache for nation data
 * @property townCache Cache for town data
 * @property economyService Economy service for balance operations
 * @property config Plugin configuration
 * @property pluginScope Coroutine scope for async operations
 * @property batchWriter Batch storage writer for persisting changes
 */
class NationTaxCollector(
    private val nationCache: NationCache,
    private val townCache: TownCache,
    private val economyService: EconomyService,
    private val config: TownyConfig,
    private val pluginScope: CoroutineScope,
    private val batchWriter: BatchStorageWriter
) {
    /**
     * Collects nation taxes from member towns.
     * Towns that can't pay go into debt (up to a limit).
     *
     * @return List of nation tax results
     */
    suspend fun collect(): List<NationTaxResult> {
        val results = mutableListOf<NationTaxResult>()
        val nations = nationCache.getAllCached()

        for (nation in nations) {
            // Skip ruined nations
            if (nation.isRuined) continue

            // Skip if nation tax is disabled
            if (!nation.taxToTowns.enabled) continue

            for (townId in nation.townIds) {
                val town = townCache.get(townId) ?: continue

                // Skip ruined towns
                if (town.isRuined) continue

                // Calculate tax amount
                val balance = economyService.getTownBalance(townId)
                val amount = TaxCalculator.calculateTax(
                    amount = balance,
                    config = nation.taxToTowns,
                    balance = balance
                )

                if (amount <= 0) continue

                val couldPay = economyService.hasTownBalance(townId, amount)
                val wentToDebt = !couldPay &&
                    (town.nationDebt + amount) <= (config.townUpkeep * config.maxNationDebtMultiplier)

                if (couldPay) {
                    // Transfer from town to nation
                    economyService.depositToNationFromTown(townId, nation.id, amount)
                } else if (wentToDebt) {
                    // Town goes into debt
                    val updatedTown = town.withModification(
                        nationDebt = town.nationDebt + amount
                    )
                    townCache.put(updatedTown)
                    pluginScope.launch {
                        batchWriter.saveTown(updatedTown)
                    }

                    // Nation still gets the "virtual" payment (adds to nation balance)
                    economyService.depositToNation(nation.id, amount)
                }
                // If town can't pay and would exceed debt limit, skip collection

                results.add(
                    NationTaxResult(
                        townId = townId,
                        townName = town.name,
                        nationId = nation.id,
                        nationName = nation.name,
                        couldPay = couldPay,
                        amount = amount,
                        wentToDebt = !couldPay && wentToDebt
                    )
                )
            }
        }

        return results
    }
}
