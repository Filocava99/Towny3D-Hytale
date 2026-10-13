package it.cava99.towny3d.service.tax

import com.hypixel.hytale.server.core.Message
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.cache.NationCache
import it.cava99.towny3d.cache.ResidentCache
import it.cava99.towny3d.cache.TownCache
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.economy.EconomyService
import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * Service for sending tax-related notifications to players.
 *
 * Handles:
 * - Pre-collection warnings (sent X minutes before tax collection)
 * - Post-collection summaries (showing what was collected)
 *
 * This file is intentionally large because:
 * - All notification logic is cohesive - it formats and sends messages for tax events
 * - Message formatting for different recipient types (mayor, king, resident, admin) is related
 * - The TaxSummaryItem sealed class is only used internally for message building
 * - Splitting would require passing TaxCollectionResult between multiple files
 *
 * Future improvements:
 * - Could extract message formatting to a separate TaxMessageFormatter if formatting becomes complex
 * - Consider using a message template system for localization support
 *
 * @property townCache Cache for town data
 * @property nationCache Cache for nation data
 * @property residentCache Cache for resident data
 * @property economyService Economy service for balance operations
 * @property config Plugin configuration
 * @property pluginScope Coroutine scope for async operations
 */
class TaxNotificationService(
    private val townCache: TownCache,
    private val nationCache: NationCache,
    private val residentCache: ResidentCache,
    private val economyService: EconomyService,
    private val config: TownyConfig,
    private val pluginScope: CoroutineScope
) {

    private var lastWarningTime: Instant = Instant.MIN

    /**
     * Checks if a pre-collection warning should be sent and sends it if needed.
     * This should be called periodically (e.g., every minute).
     *
     * @param nextCollectionTime The time when the next collection will occur
     * @return true if a warning was sent, false otherwise
     */
    fun sendWarningIfNeeded(nextCollectionTime: Instant): Boolean {
        if (!config.taxSummaryMessageEnabled) {
            return false
        }

        val now = Instant.now()
        val minutesUntilCollection = Duration.between(now, nextCollectionTime).toMinutes()

        // Send warning at configured time before collection
        if (minutesUntilCollection == config.taxWarningMinutesBefore.toLong()) {
            // Don't send duplicate warnings within the same hour
            val hoursSinceLastWarning = Duration.between(lastWarningTime, now).toHours()
            if (hoursSinceLastWarning >= 1) {
                sendPreCollectionWarning(minutesUntilCollection)
                lastWarningTime = now
                return true
            }
        }

        return false
    }

    /**
     * Sends a pre-collection warning to all online players who will be affected by taxes.
     *
     * @param minutesUntilCollection Minutes until tax collection
     */
    private fun sendPreCollectionWarning(minutesUntilCollection: Long) {
        val timeText = when {
            minutesUntilCollection == 0L -> "momentarily"
            minutesUntilCollection == 1L -> "1 minute"
            minutesUntilCollection < 60L -> "$minutesUntilCollection minutes"
            else -> {
                val hours = minutesUntilCollection / 60
                if (hours == 1L) "1 hour" else "$hours hours"
            }
        }

        // Format the warning message
        val message = config.taxWarningMessage.replace("{time}", timeText)

        // Broadcast to all online players
        // Note: In a real implementation, you'd send this only to affected players
        // For now, we'll use a server broadcast approach
        try {
            // Log the broadcast (server broadcast not yet implemented)
            TownyLog.info("Broadcast: $message")
        } catch (e: Exception) {
            TownyLog.error("Failed to send pre-collection warning: ${e.message}", e)
        }
    }

    /**
     * Sends a post-collection summary showing what was collected.
     *
     * @param result The tax collection result
     */
    fun sendPostCollectionSummary(result: TaxCollectionResult) {
        if (!config.taxSummaryMessageEnabled) {
            return
        }

        pluginScope.launch {
            try {
                // Send summary to town mayors whose towns paid or failed upkeep
                sendTownUpkeepSummary(result)

                // Send summary to nation kings whose nations paid or failed upkeep
                sendNationUpkeepSummary(result)

                // Send summary to residents who paid taxes
                sendResidentTaxSummary(result)

                // Send admin summary
                sendAdminSummary(result)
            } catch (e: Exception) {
                TownyLog.error("Failed to send post-collection summary: ${e.message}", e)
            }
        }
    }

    /**
     * Sends town upkeep summaries to mayors.
     */
    private suspend fun sendTownUpkeepSummary(result: TaxCollectionResult) {
        for (upkeepResult in result.townUpkeepResults) {
            val town = townCache.get(upkeepResult.townId) ?: continue
            val mayor = residentCache.get(town.mayorId) ?: continue

            if (upkeepResult.couldPay) {
                // Town paid successfully - no notification needed unless there were issues
            } else {
                // Town failed and became ruined
                try {
                    val message = MessageFormatter.error(
                        "Your town '${town.name}' could not pay upkeep of " +
                        "${economyService.formatAmount(upkeepResult.amount)} and has fallen to ruins. " +
                        "Use /town reclaim to restore it."
                    )
                    sendMessageToResident(mayor, message)
                } catch (e: Exception) {
                    TownyLog.error("Failed to notify mayor ${mayor.name}: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Sends nation upkeep summaries to kings.
     */
    private suspend fun sendNationUpkeepSummary(result: TaxCollectionResult) {
        for (upkeepResult in result.nationUpkeepResults) {
            val nation = nationCache.get(upkeepResult.nationId) ?: continue
            val king = residentCache.get(nation.kingId) ?: continue

            if (upkeepResult.couldPay) {
                // Nation paid successfully - no notification needed
            } else {
                // Nation failed and became ruined
                try {
                    val message = MessageFormatter.error(
                        "Your nation '${nation.name}' could not pay upkeep of " +
                        "${economyService.formatAmount(upkeepResult.amount)} and has fallen to ruins. " +
                        "Use /nation reclaim to restore it."
                    )
                    sendMessageToResident(king, message)
                } catch (e: Exception) {
                    TownyLog.error("Failed to notify king ${king.name}: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Sends resident tax summaries.
     */
    private suspend fun sendResidentTaxSummary(result: TaxCollectionResult) {
        // Group taxes by resident
        val residentTaxes = mutableMapOf<ResidentId, MutableList<TaxSummaryItem>>()

        // Add town taxes
        for (taxResult in result.townTaxResults) {
            residentTaxes.getOrPut(taxResult.residentId) { mutableListOf() }
                .add(TaxSummaryItem.TownTax(taxResult))
        }

        // Add plot taxes
        for (taxResult in result.plotTaxResults) {
            if (taxResult.residentId != null) {
                residentTaxes.getOrPut(taxResult.residentId) { mutableListOf() }
                    .add(TaxSummaryItem.PlotTax(taxResult))
            }
        }

        // Send summaries to residents with significant tax activity
        for ((residentId, items) in residentTaxes) {
            val resident = residentCache.get(residentId) ?: continue

            // Only send if there are failed payments or significant amounts
            val hasFailures = items.any { item ->
                when (item) {
                    is TaxSummaryItem.TownTax -> !item.result.couldPay
                    is TaxSummaryItem.PlotTax -> !item.result.couldPay
                }
            }

            val totalPaid = items.sumOf { item ->
                when (item) {
                    is TaxSummaryItem.TownTax -> if (item.result.couldPay) item.result.amount else 0.0
                    is TaxSummaryItem.PlotTax -> if (item.result.couldPay) item.result.amount else 0.0
                }
            }

            if (hasFailures || totalPaid > 0) {
                try {
                    val message = buildResidentTaxSummary(resident, items, totalPaid)
                    sendMessageToResident(resident, message)
                } catch (e: Exception) {
                    TownyLog.error("Failed to notify resident ${resident.name}: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Builds a tax summary message for a resident.
     */
    private fun buildResidentTaxSummary(
        resident: Resident,
        items: List<TaxSummaryItem>,
        totalPaid: Double
    ): Message {
        val builder = MessageFormatter.info("=== Tax Collection Summary ===")

        builder.insert(Message.raw("\n"))

        for (item in items) {
            when (item) {
                is TaxSummaryItem.TownTax -> {
                    val result = item.result
                    if (result.couldPay) {
                        builder.insert(MessageFormatter.success(
                            "Paid ${economyService.formatAmount(result.amount)} to town '${result.townName}'"
                        ))
                    } else {
                        builder.insert(MessageFormatter.error(
                            "Could not pay ${economyService.formatAmount(result.amount)} to town '${result.townName}'"
                        ))
                    }
                    builder.insert(Message.raw("\n"))
                }
                is TaxSummaryItem.PlotTax -> {
                    val result = item.result
                    if (result.couldPay) {
                        builder.insert(MessageFormatter.success(
                            "Paid ${economyService.formatAmount(result.amount)} plot tax to town '${result.townName}'"
                        ))
                    } else {
                        builder.insert(MessageFormatter.error(
                            "Could not pay plot tax - Lost ownership of plot in '${result.townName}'"
                        ))
                    }
                    builder.insert(Message.raw("\n"))
                }
            }
        }

        if (totalPaid > 0) {
            builder.insert(Message.raw("\n"))
            builder.insert(MessageFormatter.info("Total paid: ${economyService.formatAmount(totalPaid)}"))
        }

        return builder
    }

    /**
     * Sends an admin summary with collection statistics.
     */
    private fun sendAdminSummary(result: TaxCollectionResult) {
        val message = MessageFormatter.info("=== Tax Collection Summary ===")
            .insert(Message.raw("\n"))
            .insert(MessageFormatter.info("Total collected: ${economyService.formatAmount(result.totalCollected)}"))
            .insert(Message.raw("\n"))

        if (result.failedTownUpkeepCount > 0) {
            message.insert(MessageFormatter.error("Towns failed upkeep: ${result.failedTownUpkeepCount}"))
                .insert(Message.raw("\n"))
        }

        if (result.failedNationUpkeepCount > 0) {
            message.insert(MessageFormatter.error("Nations failed upkeep: ${result.failedNationUpkeepCount}"))
                .insert(Message.raw("\n"))
        }

        if (result.lostPlotCount > 0) {
            message.insert(MessageFormatter.warning("Plots lost due to unpaid taxes: ${result.lostPlotCount}"))
                .insert(Message.raw("\n"))
        }

        // Log to console
        TownyLog.info("TaxCollection Total collected: ${economyService.formatAmount(result.totalCollected)}")
        TownyLog.info("TaxCollection Towns failed upkeep: ${result.failedTownUpkeepCount}")
        TownyLog.info("TaxCollection Nations failed upkeep: ${result.failedNationUpkeepCount}")
        TownyLog.info("TaxCollection Plots lost: ${result.lostPlotCount}")
    }

    /**
     * Sends a message to a resident if they are online.
     */
    private suspend fun sendMessageToResident(resident: Resident, message: Message) {
        try {
            // Log notification (server broadcast not yet implemented)
            TownyLog.info("To ${resident.name}: $message")
        } catch (e: Exception) {
            TownyLog.error("Failed to send message to ${resident.name}: ${e.message}", e)
        }
    }

    /**
     * Sealed class representing different types of tax summary items.
     */
    private sealed class TaxSummaryItem {
        data class TownTax(val result: TownTaxResult) : TaxSummaryItem()
        data class PlotTax(val result: PlotTaxResult) : TaxSummaryItem()
    }
}
