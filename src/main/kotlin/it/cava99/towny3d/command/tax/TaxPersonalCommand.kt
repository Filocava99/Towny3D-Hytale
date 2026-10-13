package it.cava99.towny3d.command.tax

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /tax - Show your personal tax burden
 */
class TaxPersonalCommand : AbstractWorldCommand {

    constructor() : super("", "Show your personal tax burden") {
        requirePermission("towny3d.tax")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use this command"))
            return
        }

        val playerRef = context.senderAsPlayerRef()
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference"))
            return
        }

        val playerUuid = CommandUtils.getPlayerUuid(playerRef, store)
        if (playerUuid == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player UUID"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                // Build personal tax information
                val taxInfo = buildString {
                    append("${MessageFormatter.formatHeader("Your Tax Information")}\n\n")

                    if (resident.townId == null) {
                        append("You are not in a town. No taxes to pay.\n")
                    } else {
                        val town = plugin.townService.getTown(resident.townId!!)
                        if (town == null) {
                            append("Town not found.\n")
                        } else {
                            // Resident tax to town
                            append("Town: ${town.name}\n")
                            if (town.taxToResidents.enabled) {
                                when (town.taxToResidents.type) {
                                    it.cava99.towny3d.model.TaxType.FLAT -> {
                                        append("Resident Tax: ${plugin.economyService.formatAmount(town.taxToResidents.amount)}\n")
                                    }
                                    it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                                        append("Resident Tax: ${String.format("%.2f", town.taxToResidents.amount)}% of balance\n")
                                    }
                                }
                            } else {
                                append("Resident Tax: None\n")
                            }

                            // Calculate resident tax
                            val residentTax = if (town.taxToResidents.enabled) {
                                when (town.taxToResidents.type) {
                                    it.cava99.towny3d.model.TaxType.FLAT -> town.taxToResidents.amount
                                    it.cava99.towny3d.model.TaxType.PERCENTAGE -> resident.balance * town.taxToResidents.amount / 100.0
                                }
                            } else {
                                0.0
                            }

                            // Plot taxes
                            val ownedPlots = plugin.regionService.getRegionsByOwner(resident.id)
                            if (ownedPlots.isNotEmpty()) {
                                append("\nYour Plots:\n")
                                var totalPlotTax = 0.0

                                for (plot in ownedPlots) {
                                    if (plot.plotTax.enabled) {
                                        val plotTax = when (plot.plotTax.type) {
                                            it.cava99.towny3d.model.TaxType.FLAT -> plot.plotTax.amount
                                            it.cava99.towny3d.model.TaxType.PERCENTAGE -> resident.balance * plot.plotTax.amount / 100.0
                                        }
                                        totalPlotTax += plotTax

                                        val plotName = plot.name ?: "Plot at ${plot.chunkPos.x}, ${plot.chunkPos.z}"
                                        when (plot.plotTax.type) {
                                            it.cava99.towny3d.model.TaxType.FLAT -> {
                                                append("  - $plotName: ${plugin.economyService.formatAmount(plotTax)}\n")
                                            }
                                            it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                                                append("  - $plotName: ${String.format("%.2f", plot.plotTax.amount)}% (${plugin.economyService.formatAmount(plotTax)})\n")
                                            }
                                        }
                                    }
                                }

                                if (totalPlotTax > 0) {
                                    append("Total Plot Tax: ${plugin.economyService.formatAmount(totalPlotTax)}\n")
                                } else {
                                    append("Total Plot Tax: None\n")
                                }
                            }

                            // Nation tax
                            if (town.nationId != null) {
                                val nation = plugin.nationService.getNation(town.nationId!!)
                                if (nation != null && nation.taxToTowns.enabled) {
                                    append("\nNation: ${nation.name}\n")
                                    when (nation.taxToTowns.type) {
                                        it.cava99.towny3d.model.TaxType.FLAT -> {
                                            append("Nation Tax (paid by town): ${plugin.economyService.formatAmount(nation.taxToTowns.amount)}\n")
                                        }
                                        it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                                            append("Nation Tax (paid by town): ${String.format("%.2f", nation.taxToTowns.amount)}% of town balance\n")
                                        }
                                    }
                                }
                            }

                            // Total tax burden
                            val totalPlotTaxSum = ownedPlots.filter { it.plotTax.enabled }.sumOf { plot ->
                                when (plot.plotTax.type) {
                                    it.cava99.towny3d.model.TaxType.FLAT -> plot.plotTax.amount
                                    it.cava99.towny3d.model.TaxType.PERCENTAGE -> resident.balance * plot.plotTax.amount / 100.0
                                }
                            }

                            val totalBurden = residentTax + totalPlotTaxSum
                            append("\n${MessageFormatter.formatHeader("Total Tax Burden")}\n")
                            append("Personal Tax: ${plugin.economyService.formatAmount(residentTax)}\n")
                            append("Plot Tax: ${plugin.economyService.formatAmount(totalPlotTaxSum)}\n")
                            append("Total: ${plugin.economyService.formatAmount(totalBurden)}\n")
                            append("Your Balance: ${plugin.economyService.formatAmount(resident.balance)}\n")
                        }
                    }
                }

                context.sendMessage(Message.raw(taxInfo))

            } catch (e: Exception) {
                TownyLog.error("Failed to get personal tax info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get personal tax info: ${e.message}"))
            }
        }
    }
}
