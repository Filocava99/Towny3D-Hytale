package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandValidation
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /plot tax <flat|percent> <amount> - Set tax for this plot (mayor/assistant only)
 */
class PlotTaxSetCommand : AbstractWorldCommand {
    private val taxTypeArg = this.withRequiredArg("type", "Tax type (flat/percent)", ArgTypes.STRING)
    private val amountArg = this.withRequiredArg("amount", "Tax amount", ArgTypes.DOUBLE)

    constructor() : super("", "Set tax for this plot") {
        requirePermission("towny3d.plot.tax")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerUuid = playerContext.uuid
        val playerPosition = playerContext.position

        val taxTypeStr = taxTypeArg.get(context) as? String
        val amount = amountArg.get(context) as? Double

        if (taxTypeStr.isNullOrBlank() || amount == null) {
            context.sendMessage(MessageFormatter.error("Usage: /plot tax <flat|percent> <amount>"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                if (!town.hasLeadership(resident.id)) {
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can set plot taxes"))
                    return@launch
                }

                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                val regions = plugin.regionService.getRegionsAt(chunkPos)
                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.error("This land is not claimed"))
                    return@launch
                }

                val region = regions.first()

                if (region.townId != town.id) {
                    context.sendMessage(MessageFormatter.error("This plot does not belong to your town"))
                    return@launch
                }

                val taxType = when (taxTypeStr.lowercase()) {
                    "flat", "f" -> it.cava99.towny3d.model.TaxType.FLAT
                    "percent", "percentage", "p" -> it.cava99.towny3d.model.TaxType.PERCENTAGE
                    else -> {
                        context.sendMessage(MessageFormatter.error("Invalid tax type. Use: flat or percent"))
                        return@launch
                    }
                }

                if (amount < 0) {
                    context.sendMessage(MessageFormatter.error("Tax amount cannot be negative"))
                    return@launch
                }

                if (taxType == it.cava99.towny3d.model.TaxType.PERCENTAGE && amount > 100) {
                    context.sendMessage(MessageFormatter.error("Percentage tax cannot exceed 100%"))
                    return@launch
                }

                val taxConfig = it.cava99.towny3d.model.TaxConfig(
                    enabled = true,
                    type = taxType,
                    amount = amount
                )
                plugin.regionService.setPlotTax(region.id, taxConfig)

                val successMsg = when (taxType) {
                    it.cava99.towny3d.model.TaxType.FLAT -> {
                        "Plot tax set to ${plugin.economyService.formatAmount(amount)}"
                    }
                    it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                        "Plot tax set to ${String.format("%.2f", amount)}%"
                    }
                }
                context.sendMessage(MessageFormatter.success(successMsg))

            } catch (e: Exception) {
                TownyLog.error("Failed to set plot tax", e)
                context.sendMessage(MessageFormatter.error("Failed to set plot tax: ${e.message}"))
            }
        }
    }
}
