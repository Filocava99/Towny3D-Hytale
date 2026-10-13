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
 * /plot info - Show plot information
 */
class PlotInfoCommand : AbstractWorldCommand {

    constructor() : super("info", "Show plot information") {
        requirePermission("towny3d.plot.info")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerPosition = playerContext.position

        plugin.pluginScope.launch {
            try {
                // Get current location
                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                // Check if location is in a region
                val regions = plugin.regionService.getRegionsAt(chunkPos)

                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("This land is wilderness (not claimed)"))
                    return@launch
                }

                val region = regions.first()

                // Build plot info
                val info = buildString {
                    append("${MessageFormatter.formatHeader("Plot Information")}\n\n")
                    append("Type: ${region.type}\n")

                    // Get town name
                    val town = plugin.townService.getTown(region.townId)
                    if (town != null) {
                        append("Town: ${town.name}\n")
                    }

                    // Get owner name
                    if (region.ownerId != null) {
                        val owner = plugin.residentService.getResident(region.ownerId)
                        append("Owner: ${owner?.name ?: "Unknown"}\n")
                    } else {
                        append("Owner: None (town owned)\n")
                    }

                    // Check if for sale
                    if (region.forSale) {
                        val priceText = if (region.salePrice > 0) {
                            plugin.economyService.formatAmount(region.salePrice)
                        } else {
                            "FREE"
                        }
                        append("Price: $priceText\n")
                        append("Status: For Sale\n")
                    } else {
                        append("Status: Not for sale\n")
                    }

                    // Permissions
                    append("\nPermissions:\n")
                    append("  PvP: ${if (region.toggles.pvp) "Allowed" else "Denied"}\n")
                    append("  Explosions: ${if (region.toggles.explosion) "Allowed" else "Denied"}\n")
                    append("  Fire: ${if (region.toggles.fire) "Allowed" else "Denied"}\n")
                }

                context.sendMessage(Message.raw(info))

            } catch (e: Exception) {
                TownyLog.error("Failed to get plot info", e)
                context.sendMessage(MessageFormatter.error("Failed to get plot info: ${e.message}"))
            }
        }
    }
}
