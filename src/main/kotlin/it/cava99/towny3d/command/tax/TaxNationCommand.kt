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
 * /tax nation <name> - View nation tax info
 */
class TaxNationCommand : AbstractWorldCommand {

    private val nationNameArg = this.withRequiredArg("name", "Nation name", ArgTypes.STRING)

    constructor() : super("nation", "View nation tax info") {
        requirePermission("towny3d.tax.nation")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val nationName = nationNameArg.get(context) as? String
        if (nationName.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /tax nation <name>"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val nation = plugin.nationService.getNationByName(nationName)
                if (nation == null) {
                    context.sendMessage(MessageFormatter.error("Nation '$nationName' not found"))
                    return@launch
                }

                // Build nation tax information
                val taxInfo = buildString {
                    append("${MessageFormatter.formatHeader("Nation Tax Information: ${nation.name}")}\n\n")
                    append("King: ${plugin.residentService.getResident(nation.kingId)?.name ?: "Unknown"}\n")
                    append("Capital: ${plugin.townService.getTown(nation.capitalId)?.name ?: "Unknown"}\n")
                    append("Towns: ${nation.townCount}\n\n")

                    append("Town Tax:\n")
                    if (nation.taxToTowns.enabled) {
                        append("  Status: Active\n")
                        append("  Type: ${nation.taxToTowns.type}\n")
                        when (nation.taxToTowns.type) {
                            it.cava99.towny3d.model.TaxType.FLAT -> {
                                append("  Amount: ${plugin.economyService.formatAmount(nation.taxToTowns.amount)} per town\n")
                                val potentialRevenue = nation.taxToTowns.amount * nation.townCount
                                append("  Potential Revenue: ${plugin.economyService.formatAmount(potentialRevenue)}\n")
                            }
                            it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                                append("  Rate: ${String.format("%.2f", nation.taxToTowns.amount)}% of town balance\n")
                            }
                        }
                    } else {
                        append("  Status: Disabled\n")
                    }

                    // Nation balance
                    append("\nNation Balance: ${plugin.economyService.formatAmount(nation.balance)}\n")
                }

                context.sendMessage(Message.raw(taxInfo))

            } catch (e: Exception) {
                TownyLog.error("Failed to get nation tax info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get nation tax info: ${e.message}"))
            }
        }
    }
}
