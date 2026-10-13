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
 * /nation delete - Delete your nation
 */
class NationDeleteCommand : AbstractWorldCommand {

    constructor() : super("delete", "Delete your nation") {
        requirePermission("towny3d.nation.delete")
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

                // Check if player is the king
                if (nation.kingId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the king can delete the nation"))
                    return@launch
                }

                // Nation balance is tracked through the capital town
                // No refund needed as nations don't have separate balance

                // Remove all towns from the nation
                for (townId in nation.townIds) {
                    plugin.townService.setNation(townId, null)
                }

                // Delete the nation
                plugin.nationService.deleteNation(nation.id)

                context.sendMessage(MessageFormatter.success("Nation '${nation.name}' has been deleted"))

            } catch (e: Exception) {
                TownyLog.error("Failed to delete nation", e)
                context.sendMessage(MessageFormatter.error("Failed to delete nation: ${e.message}"))
            }
        }
    }
}
