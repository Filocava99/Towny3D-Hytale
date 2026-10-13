package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandValidation
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /town delete - Delete your town
 */
class TownDeleteCommand : AbstractWorldCommand {

    constructor() : super("delete", "Delete your town") {
        requirePermission("towny3d.town.delete")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance
        val config = plugin.getConfig()

        val playerContext = CommandValidation.validatePlayerBasic(context, store) ?: return
        val playerUuid = playerContext.uuid

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
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                // Get town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is the mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can delete the town"))
                    return@launch
                }

                // Refund town balance to mayor
                if (config.enableEconomy && town.balance > 0) {
                    plugin.economyService.deposit(resident.id, town.balance)
                    context.sendMessage(
                        MessageFormatter.info("Town balance of ")
                            .insert(Message.raw(plugin.economyService.formatAmount(town.balance)).color("#FFAA00").bold(true))
                            .insert(Message.raw(" refunded to you"))
                    )
                }

                // Remove all residents from the town
                for (residentId in town.residentIds) {
                    plugin.residentService.setTown(residentId, null)
                }

                // Delete all regions owned by this town
                val regions = plugin.regionService.getRegionsByTown(town.id)
                for (region in regions) {
                    plugin.regionService.deleteRegion(region.id)
                }
                TownyLog.info("Deleted ${regions.size} regions for town '${town.name}'")

                // Delete the town
                plugin.townService.deleteTown(town.id)

                context.sendMessage(
                    MessageFormatter.success("Town '")
                        .insert(MessageFormatter.formatTown(town.name, town.isPublic))
                        .insert(Message.raw("' has been deleted"))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to delete town: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to delete town: ${e.message}"))
            }
        }
    }
}
