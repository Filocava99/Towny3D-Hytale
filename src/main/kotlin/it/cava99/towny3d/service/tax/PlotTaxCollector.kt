package it.cava99.towny3d.service.tax

import it.cava99.towny3d.cache.RegionCache
import it.cava99.towny3d.cache.ResidentCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.economy.EconomyService
import it.cava99.towny3d.storage.BatchStorageWriter
import it.cava99.towny3d.util.TaxCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Collector for plot taxes from plot owners.
 *
 * Plot owners pay taxes to the town that owns the plot. Owners that can't pay
 * lose ownership of the plot.
 *
 * @property regionCache Cache for region data
 * @property residentCache Cache for resident data
 * @property townCache Cache for town data
 * @property economyService Economy service for balance operations
 * @property pluginScope Coroutine scope for async operations
 * @property batchWriter Batch storage writer for persisting changes
 */
class PlotTaxCollector(
    private val regionCache: RegionCache,
    private val residentCache: ResidentCache,
    private val townCache: TownCache,
    private val economyService: EconomyService,
    private val pluginScope: CoroutineScope,
    private val batchWriter: BatchStorageWriter
) {
    /**
     * Collects plot taxes from plot owners.
     * Owners that can't pay lose ownership of the plot.
     *
     * @return List of plot tax results
     */
    suspend fun collect(): List<PlotTaxResult> {
        val results = mutableListOf<PlotTaxResult>()
        val regions = regionCache.getAllCached()

        for (region in regions) {
            // Skip town-owned plots (no owner)
            if (region.ownerId == null) continue

            val ownerId = region.ownerId
            val resident = residentCache.get(ownerId) ?: continue
            val town = townCache.get(region.townId) ?: continue

            // Skip ruined towns
            if (town.isRuined) continue

            // Skip if plot tax is disabled
            if (!region.plotTax.enabled) continue

            // Calculate tax amount
            val balance = economyService.getBalance(ownerId)
            val amount = TaxCalculator.calculateTax(
                amount = balance,
                config = region.plotTax,
                balance = balance
            )

            if (amount <= 0) continue

            val couldPay = economyService.hasBalance(ownerId, amount)
            var lostOwnership = false

            if (couldPay) {
                // Transfer from resident to town
                economyService.depositToTownFromPlayer(ownerId, region.townId, amount)
            } else {
                // Owner loses plot ownership
                val updatedRegion = region.withModification(ownerId = null)
                regionCache.put(updatedRegion)
                pluginScope.launch {
                    batchWriter.saveRegion(updatedRegion)
                }
                lostOwnership = true
            }

            results.add(
                PlotTaxResult(
                    regionId = region.id,
                    residentId = ownerId,
                    residentName = resident.name,
                    townId = region.townId,
                    townName = town.name,
                    couldPay = couldPay,
                    amount = amount,
                    lostOwnership = lostOwnership
                )
            )
        }

        return results
    }
}
