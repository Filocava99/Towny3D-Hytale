package it.cava99.towny3d.command.nation

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandValidation
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.TaxType
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /nation tax <flat|percent> <amount> - Set tax on member towns (king only)
 */
class NationTaxCommand : AbstractWorldCommand {

    private val typeArg = this.withRequiredArg("type", "Tax type (flat/percent)", ArgTypes.STRING)
    private val amountArg = this.withRequiredArg("amount", "Tax amount", ArgTypes.STRING)

    constructor() : super("tax", "Set tax on member towns") {
        requirePermission("towny3d.nation.tax")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val taxType = typeArg.get(context) as? String
        val amountStr = amountArg.get(context) as? String

        if (taxType.isNullOrBlank() || amountStr.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /nation tax <flat|percent> <amount>"))
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount < 0) {
            context.sendMessage(MessageFormatter.error("Amount must be a non-negative number"))
            return
        }

        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayerBasic(context, store) ?: return
        val playerUuid = playerContext.uuid

        plugin.pluginScope.launch {
            try {
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                if (!town.hasNation || town.nationId == null) {
                    context.sendMessage(MessageFormatter.error("Your town is not in a nation"))
                    return@launch
                }

                val nation = plugin.nationService.getNation(town.nationId!!)
                if (nation == null) {
                    context.sendMessage(MessageFormatter.error("Nation not found"))
                    return@launch
                }

                // Only king can set taxes
                if (nation.kingId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the king can set taxes"))
                    return@launch
                }

                val taxConfig = when (taxType.lowercase()) {
                    "flat" -> TaxConfig.flat(amount)
                    "percent", "percentage" -> {
                        if (amount > 100) {
                            context.sendMessage(MessageFormatter.error("Percentage tax cannot exceed 100%"))
                            return@launch
                        }
                        TaxConfig.percentage(amount)
                    }
                    else -> {
                        context.sendMessage(MessageFormatter.error("Invalid tax type. Use: flat or percent"))
                        return@launch
                    }
                }

                plugin.nationService.setTaxToTowns(nation.id, taxConfig)

                val formattedAmount = plugin.economyService.formatAmount(amount)
                val typeText = if (taxConfig.type == TaxType.FLAT) {
                    "flat tax of $formattedAmount"
                } else {
                    "${amount}% tax"
                }

                context.sendMessage(
                    MessageFormatter.success("Town tax set to ")
                        .insert(Message.raw(typeText).color("#FFAA00").bold(true))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to set tax", e)
                context.sendMessage(MessageFormatter.error("Failed to set tax: ${e.message}"))
            }
        }
    }
}
