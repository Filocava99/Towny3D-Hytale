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
 * /plot set embassy - Mark plot as embassy
 */
class PlotSetEmbassyCommand : AbstractWorldCommand {
    constructor() : super("embassy", "Mark plot as embassy") {
        requirePermission("towny3d.plot.set")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerUuid = playerContext.uuid
        val playerPosition = playerContext.position

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
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can mark embassies"))
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

                if (region.type == it.cava99.towny3d.model.RegionType.HOME) {
                    context.sendMessage(MessageFormatter.error("Cannot mark home region as embassy"))
                    return@launch
                }

                // Set plot type to embassy
                plugin.regionService.setRegionType(region.id, it.cava99.towny3d.model.RegionType.EMBASSY)

                context.sendMessage(MessageFormatter.success("Plot marked as embassy"))
                context.sendMessage(MessageFormatter.info("Non-residents can now purchase this plot"))

            } catch (e: Exception) {
                TownyLog.error("Failed to set embassy", e)
                context.sendMessage(MessageFormatter.error("Failed to set embassy: ${e.message}"))
            }
        }
    }
}
