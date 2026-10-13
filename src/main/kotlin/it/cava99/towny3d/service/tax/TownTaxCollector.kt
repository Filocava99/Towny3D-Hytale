package it.cava99.towny3d.service.tax

import it.cava99.towny3d.cache.ResidentCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.economy.EconomyService
import it.cava99.towny3d.util.TaxCalculator

/**
 * Collector for town taxes from residents.
 *
 * Residents pay taxes to their town. Residents that can't pay are skipped
 * (no debt for town taxes).
 *
 * @property townCache Cache for town data
 * @property residentCache Cache for resident data
 * @property economyService Economy service for balance operations
 */
class TownTaxCollector(
    private val townCache: TownCache,
    private val residentCache: ResidentCache,
    private val economyService: EconomyService
) {
    /**
     * Collects town taxes from residents.
     * Residents that can't pay are skipped (no debt for town taxes).
     *
     * @return List of town tax results
     */
    suspend fun collect(): List<TownTaxResult> {
        val results = mutableListOf<TownTaxResult>()
        val towns = townCache.getAllCached()

        for (town in towns) {
            // Skip ruined towns
            if (town.isRuined) continue

            // Skip if town tax is disabled
            if (!town.taxToResidents.enabled) continue

            for (residentId in town.residentIds) {
                val resident = residentCache.get(residentId) ?: continue

                // Calculate tax amount
                val balance = economyService.getBalance(residentId)
                val amount = TaxCalculator.calculateTax(
                    amount = balance,
                    config = town.taxToResidents,
                    balance = balance
                )

                if (amount <= 0) continue

                val couldPay = economyService.hasBalance(residentId, amount)

                if (couldPay) {
                    // Transfer from resident to town
                    economyService.depositToTownFromPlayer(residentId, town.id, amount)
                }
                // If resident can't pay, skip collection

                results.add(
                    TownTaxResult(
                        residentId = residentId,
                        residentName = resident.name,
                        townId = town.id,
                        townName = town.name,
                        couldPay = couldPay,
                        amount = amount
                    )
                )
            }
        }

        return results
    }
}
