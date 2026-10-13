package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
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
 * /plot tax info - View plot tax
 */
class PlotTaxInfoCommand : AbstractWorldCommand {
    constructor() : super("info", "View plot tax") {
        requirePermission("towny3d.plot.tax.info")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerPosition = playerContext.position

        plugin.pluginScope.launch {
            try {
                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                val regions = plugin.regionService.getRegionsAt(chunkPos)
                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("This land is wilderness (not claimed)"))
                    return@launch
                }

                val region = regions.first()
                val taxConfig = region.plotTax

                if (!taxConfig.enabled) {
                    context.sendMessage(MessageFormatter.info("This plot has no tax set"))
                } else {
                    val taxInfo = buildString {
                        append("${MessageFormatter.formatHeader("Plot Tax Information")}\n\n")
                        append("Status: Active\n")
                        append("Type: ${taxConfig.type}\n")
                        when (taxConfig.type) {
                            it.cava99.towny3d.model.TaxType.FLAT -> {
                                append("Amount: ${plugin.economyService.formatAmount(taxConfig.amount)}\n")
                            }
                            it.cava99.towny3d.model.TaxType.PERCENTAGE -> {
                                append("Rate: ${String.format("%.2f", taxConfig.amount)}%\n")
                            }
                        }
                    }
                    context.sendMessage(Message.raw(taxInfo))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to get plot tax info", e)
                context.sendMessage(MessageFormatter.error("Failed to get plot tax info: ${e.message}"))
            }
        }
    }
}
