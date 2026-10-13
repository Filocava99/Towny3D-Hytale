package it.cava99.towny3d.command.tax

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /tax town <name> - View town tax info
 */
class TaxTownCommand : AbstractWorldCommand {

    private val townNameArg = this.withRequiredArg("name", "Town name", ArgTypes.STRING)

    constructor() : super("town", "View town tax info") {
        requirePermission("towny3d.tax.town")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val townName = townNameArg.get(context) as? String
        if (townName.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /tax town <name>"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val town = plugin.townService.getTownByName(townName)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town '$townName' not found"))
                    return@launch
                }

                // Build town tax information
                val taxInfo = buildString {
                    append("${MessageFormatter.formatHeader("Town Tax Information: ${town.name}")}\n\n")
                    append("Mayor: ${plugin.residentService.getResident(town.mayorId)?.name ?: "Unknown"}\n")
                    append("Residents: ${town.residentCount}\n\n")

                    append("Resident Tax:\n")
                    if (town.taxToResidents.enabled) {
                        append("  Status: Active\n")
                        append("  Type: ${town.taxToResidents.type}\n")
                        when (town.taxToResidents.type) {
                            it.cava99.towny3d.model.TaxType.FLAT -> {
                                append("  Amount: ${plugin.economyService.formatAmount(town.taxToResidents.amount)} per resident\n")
                            }
                            it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                                append("  Rate: ${String.format("%.2f", town.taxToResidents.amount)}% of resident balance\n")
                            }
                        }
                    } else {
                        append("  Status: Disabled\n")
                    }

                    // Nation tax
                    if (town.nationId != null) {
                        val nation = plugin.nationService.getNation(town.nationId)
                        if (nation != null) {
                            append("\nNation Tax (${nation.name}):\n")
                            if (nation.taxToTowns.enabled) {
                                append("  Status: Active\n")
                                append("  Type: ${nation.taxToTowns.type}\n")
                                when (nation.taxToTowns.type) {
                                    it.cava99.towny3d.model.TaxType.FLAT -> {
                                        append("  Amount: ${plugin.economyService.formatAmount(nation.taxToTowns.amount)}\n")
                                    }
                                    it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                                        append("  Rate: ${String.format("%.2f", nation.taxToTowns.amount)}% of town balance\n")
                                    }
                                }
                            } else {
                                append("  Status: Disabled\n")
                            }
                        }
                    }

                    // Town balance
                    append("\nTown Balance: ${plugin.economyService.formatAmount(town.balance)}\n")
                }

                context.sendMessage(Message.raw(taxInfo))

            } catch (e: Exception) {
                TownyLog.error("Failed to get town tax info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get town tax info: ${e.message}"))
            }
        }
    }
}
