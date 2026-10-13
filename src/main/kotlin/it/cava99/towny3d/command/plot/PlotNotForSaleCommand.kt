package it.cava99.towny3d.command.plot

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
 * /plot notforsale - Remove plot from sale
 */
class PlotNotForSaleCommand : AbstractWorldCommand {

    constructor() : super("notforsale", "Remove plot from sale") {
        requirePermission("towny3d.plot.notforsale")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerUuid = playerContext.uuid
        val playerPosition = playerContext.position

        plugin.pluginScope.launch {
            try {
                // Get resident
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
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

                // Check if player owns this plot or is mayor of the town
                val isOwner = region.ownerId == resident.id
                val isMayor = if (resident.townId != null) {
                    val town = plugin.townService.getTown(resident.townId!!)
                    town?.mayorId == resident.id
                } else false

                if (!isOwner && !isMayor) {
                    context.sendMessage(MessageFormatter.error("You don't own this plot and are not the mayor"))
                    return@launch
                }

                // Remove from sale
                plugin.regionService.setForSale(region.id, false, 0.0)

                context.sendMessage(MessageFormatter.success("Plot removed from sale"))

            } catch (e: Exception) {
                TownyLog.error("Failed to remove plot from sale", e)
                context.sendMessage(MessageFormatter.error("Failed to remove plot from sale: ${e.message}"))
            }
        }
    }
}
