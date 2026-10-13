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
 * /plot set reset - Reset plot to default
 */
class PlotSetResetCommand : AbstractWorldCommand {
    constructor() : super("reset", "Reset plot to default") {
        requirePermission("towny3d.plot.set.reset")
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

                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                val regions = plugin.regionService.getRegionsAt(chunkPos)
                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.error("This land is not claimed"))
                    return@launch
                }

                val region = regions.first()

                // Check permissions - owner or mayor/assistant of town
                val isOwner = region.ownerId == resident.id
                val isLeader = town.hasLeadership(resident.id)

                if (!isOwner && !isLeader) {
                    context.sendMessage(MessageFormatter.error("Only the plot owner or town leadership can reset the plot"))
                    return@launch
                }

                // Reset plot to default settings
                if (region.type == it.cava99.towny3d.model.RegionType.EMBASSY) {
                    plugin.regionService.setRegionType(region.id, it.cava99.towny3d.model.RegionType.NORMAL)
                }

                // Remove owner, for sale status, etc.
                plugin.regionService.removeOwner(region.id)
                plugin.regionService.setForSale(region.id, false, 0.0)

                context.sendMessage(MessageFormatter.success("Plot reset to default settings"))

            } catch (e: Exception) {
                TownyLog.error("Failed to reset plot", e)
                context.sendMessage(MessageFormatter.error("Failed to reset plot: ${e.message}"))
            }
        }
    }
}
