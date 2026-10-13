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
 * /town leave - Leave your town
 */
class TownLeaveCommand : AbstractWorldCommand {

    constructor() : super("leave", "Leave your town") {
        requirePermission("towny3d.town.leave")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

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
                if (town.mayorId == resident.id) {
                    context.sendMessage(MessageFormatter.error("The mayor cannot leave the town. Delete it or transfer mayorship first."))
                    return@launch
                }

                // Remove player from town
                plugin.townService.removeResident(town.id, resident.id)
                plugin.residentService.setTown(resident.id, null)

                context.sendMessage(
                    MessageFormatter.success("You have left town '")
                        .insert(MessageFormatter.formatTown(town.name, town.isPublic))
                        .insert(Message.raw("'"))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to leave town: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to leave town: ${e.message}"))
            }
        }
    }
}
