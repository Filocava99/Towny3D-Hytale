package it.cava99.towny3d.command.nation

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
 * /nation leave - Leave your nation
 */
class NationLeaveCommand : AbstractWorldCommand {

    constructor() : super("leave", "Leave your nation") {
        requirePermission("towny3d.nation.leave")
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

                // Get player's town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is the mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only town mayors can leave nations"))
                    return@launch
                }

                // Check if town is in a nation
                if (!town.hasNation || town.nationId == null) {
                    context.sendMessage(MessageFormatter.error("Your town is not in a nation"))
                    return@launch
                }

                // Get nation
                val nation = plugin.nationService.getNation(town.nationId!!)
                if (nation == null) {
                    context.sendMessage(MessageFormatter.error("Nation not found"))
                    return@launch
                }

                // Check if town is the capital
                if (nation.capitalId == town.id) {
                    context.sendMessage(MessageFormatter.error("The capital cannot leave the nation. Transfer capital or delete the nation first."))
                    return@launch
                }

                // Remove town from nation
                plugin.nationService.removeTown(nation.id, town.id)
                plugin.townService.setNation(town.id, null)

                context.sendMessage(MessageFormatter.success("Your town has left nation '${nation.name}'"))

            } catch (e: Exception) {
                TownyLog.error("Failed to leave nation", e)
                context.sendMessage(MessageFormatter.error("Failed to leave nation: ${e.message}"))
            }
        }
    }
}
