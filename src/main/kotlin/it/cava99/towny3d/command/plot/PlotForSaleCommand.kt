package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.Message
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
 * /plot forsale [price] - Put plot up for sale
 */
class PlotForSaleCommand : AbstractWorldCommand {

    private val priceArg = this.withOptionalArg("price", "Price", ArgTypes.INTEGER)

    constructor() : super("forsale", "Put plot up for sale") {
        requirePermission("towny3d.plot.forsale")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val price = if (priceArg.provided(context)) {
            (priceArg.get(context) as? Int)?.coerceAtLeast(0) ?: 0
        } else {
            0
        }

        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerUuid = playerContext.uuid
        val playerPosition = playerContext.position

        plugin.pluginScope.launch {
            try {
                // Get resident (with fallback to load/create if not in cache)
                val resident = CommandUtils.getOrCreateResident(plugin, playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Failed to load resident data. Please try again."))
                    return@launch
                }

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to sell plots"))
                    return@launch
                }

                // Get player's town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can put town plots up for sale"))
                    return@launch
                }

                // Get current location
                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                // Check if location is in a region
                val regions = plugin.regionService.getRegionsAt(chunkPos)
                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.error("This land is not claimed"))
                    return@launch
                }

                val region = regions.first()

                // Check if region belongs to player's town
                if (region.townId != resident.townId) {
                    context.sendMessage(MessageFormatter.error("This plot does not belong to your town"))
                    return@launch
                }

                // Check if plot is already owned by someone else
                if (region.ownerId != null && region.ownerId != resident.id) {
                    context.sendMessage(MessageFormatter.error("This plot is owned by someone else"))
                    return@launch
                }

                // Set plot for sale
                plugin.regionService.setForSale(region.id, true, price.toDouble())

                if (price > 0) {
                    context.sendMessage(
                        MessageFormatter.success("Plot put up for sale: ")
                            .insert(Message.raw(plugin.economyService.formatAmount(price.toDouble())).color("#FFAA00").bold(true))
                    )
                } else {
                    context.sendMessage(MessageFormatter.success("Plot put up for sale: FREE"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to set plot for sale", e)
                context.sendMessage(MessageFormatter.error("Failed to set plot for sale: ${e.message}"))
            }
        }
    }
}
