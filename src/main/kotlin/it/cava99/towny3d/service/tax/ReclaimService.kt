package it.cava99.towny3d.service.tax

import it.cava99.towny3d.cache.NationCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.economy.EconomyService
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.storage.BatchStorageWriter
import java.time.Instant

/**
 * Service for reclaiming ruined towns and nations.
 *
 * Allows paying a reclaim fee to restore a ruined town or nation.
 * The reclaim fee is calculated as: upkeep * reclaimFeeMultiplier.
 *
 * @property townCache Cache for town data
 * @property nationCache Cache for nation data
 * @property economyService Economy service for balance operations
 * @property config Plugin configuration
 * @property batchWriter Batch storage writer for persisting changes
 */
class ReclaimService(
    private val townCache: TownCache,
    private val nationCache: NationCache,
    private val economyService: EconomyService,
    private val config: TownyConfig,
    private val batchWriter: BatchStorageWriter
) {
    /**
     * Allows paying the reclaim fee to restore a ruined town.
     *
     * The reclaim fee is calculated as: townUpkeep * reclaimFeeMultiplier
     *
     * @param townId The ID of the ruined town
     * @param payerId The ID of the resident paying the reclaim fee
     * @return ReclaimResult indicating success or failure
     */
    suspend fun reclaimTown(townId: TownId, payerId: ResidentId): ReclaimResult {
        // Get the town
        val town = townCache.get(townId)

        if (town == null) {
            return ReclaimResult(
                success = false,
                message = "Town not found."
            )
        }

        if (!town.isRuined) {
            return ReclaimResult(
                success = false,
                message = "Town '${town.name}' is not ruined."
            )
        }

        // Check if payer is the mayor
        if (town.mayorId != payerId) {
            return ReclaimResult(
                success = false,
                message = "Only the mayor can reclaim the town."
            )
        }

        // Calculate reclaim fee
        val reclaimFee = config.townUpkeep * config.reclaimFeeMultiplier

        // Check if payer can afford the fee
        if (!economyService.hasBalance(payerId, reclaimFee)) {
            return ReclaimResult(
                success = false,
                message = "You cannot afford the reclaim fee of ${economyService.formatAmount(reclaimFee)}."
            )
        }

        // Check if town has been ruined too long (deletion threshold)
        val ruinedAt = town.ruinedAt ?: return ReclaimResult(
            success = false,
            message = "Invalid ruined state."
        )

        val daysSinceRuined = java.time.Duration.between(ruinedAt, Instant.now()).toDays()
        if (daysSinceRuined >= config.ruinedDeletionDays.toLong()) {
            return ReclaimResult(
                success = false,
                message = "Town has been ruined for too long and cannot be reclaimed."
            )
        }

        // Pay the reclaim fee (money goes to server/void)
        economyService.withdraw(payerId, reclaimFee)

        // Restore the town
        val restoredTown = town.withModification(
            isRuined = false,
            ruinedAt = null
        )
        townCache.put(restoredTown)
        batchWriter.saveTown(restoredTown)

        return ReclaimResult(
            success = true,
            message = "Successfully reclaimed town '${town.name}' for ${economyService.formatAmount(reclaimFee)}."
        )
    }

    /**
     * Allows paying the reclaim fee to restore a ruined nation.
     *
     * The reclaim fee is calculated as: nationUpkeep * reclaimFeeMultiplier
     *
     * @param nationId The ID of the ruined nation
     * @param payerId The ID of the resident paying the reclaim fee
     * @return ReclaimResult indicating success or failure
     */
    suspend fun reclaimNation(nationId: NationId, payerId: ResidentId): ReclaimResult {
        // Get the nation
        val nation = nationCache.get(nationId)

        if (nation == null) {
            return ReclaimResult(
                success = false,
                message = "Nation not found."
            )
        }

        if (!nation.isRuined) {
            return ReclaimResult(
                success = false,
                message = "Nation '${nation.name}' is not ruined."
            )
        }

        // Check if payer is the king
        if (nation.kingId != payerId) {
            return ReclaimResult(
                success = false,
                message = "Only the king can reclaim the nation."
            )
        }

        // Calculate reclaim fee
        val reclaimFee = config.nationUpkeep * config.reclaimFeeMultiplier

        // Check if payer can afford the fee
        if (!economyService.hasBalance(payerId, reclaimFee)) {
            return ReclaimResult(
                success = false,
                message = "You cannot afford the reclaim fee of ${economyService.formatAmount(reclaimFee)}."
            )
        }

        // Check if nation has been ruined too long (deletion threshold)
        val ruinedAt = nation.ruinedAt ?: return ReclaimResult(
            success = false,
            message = "Invalid ruined state."
        )

        val daysSinceRuined = java.time.Duration.between(ruinedAt, Instant.now()).toDays()
        if (daysSinceRuined >= config.ruinedDeletionDays.toLong()) {
            return ReclaimResult(
                success = false,
                message = "Nation has been ruined for too long and cannot be reclaimed."
            )
        }

        // Pay the reclaim fee (money goes to server/void)
        economyService.withdraw(payerId, reclaimFee)

        // Restore the nation
        val restoredNation = nation.withModification(
            isRuined = false,
            ruinedAt = null
        )
        nationCache.put(restoredNation)
        batchWriter.saveNation(restoredNation)

        return ReclaimResult(
            success = true,
            message = "Successfully reclaimed nation '${nation.name}' for ${economyService.formatAmount(reclaimFee)}."
        )
    }
}
