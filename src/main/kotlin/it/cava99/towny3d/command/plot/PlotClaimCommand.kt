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
 * /plot claim - Claim plot for yourself
 */
class PlotClaimCommand : AbstractWorldCommand {

    constructor() : super("claim", "Claim plot for yourself") {
        requirePermission("towny3d.plot.claim")
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

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to claim plots"))
                    return@launch
                }

                // Get current location
                val config = plugin.getConfig()
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

                // Check if plot is already claimed
                if (region.ownerId != null) {
                    val ownerResident = plugin.residentService.getResident(region.ownerId)
                    val ownerName = ownerResident?.name ?: "Unknown"
                    context.sendMessage(MessageFormatter.error("This plot is already owned by $ownerName"))
                    return@launch
                }

                // Claim the plot
                plugin.regionService.setOwner(region.id, resident.id)

                context.sendMessage(MessageFormatter.success("You have claimed this plot"))

            } catch (e: Exception) {
                TownyLog.error("Failed to claim plot", e)
                context.sendMessage(MessageFormatter.error("Failed to claim plot: ${e.message}"))
            }
        }
    }
}
