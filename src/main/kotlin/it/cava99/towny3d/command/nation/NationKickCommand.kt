package it.cava99.towny3d.command.nation

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
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
 * /nation kick <town> - Kick a town from your nation
 */
class NationKickCommand : AbstractWorldCommand {

    private val townArg = this.withRequiredArg("town", "Town name", ArgTypes.STRING)

    constructor() : super("kick", "Kick a town from your nation") {
        requirePermission("towny3d.nation.kick")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val townName = townArg.get(context) as? String

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
                    context.sendMessage(MessageFormatter.error("Only the king can kick towns"))
                    return@launch
                }

                // Find target town by name
                val targetTown = plugin.townService.getTownByName(townName!!)
                if (targetTown == null) {
                    context.sendMessage(MessageFormatter.error("Town '$townName' not found"))
                    return@launch
                }

                // Check if target town is in this nation
                if (!targetTown.hasNation || targetTown.nationId != nation.id) {
                    context.sendMessage(MessageFormatter.error("Town '$townName' is not in your nation"))
                    return@launch
                }

                // Check if trying to kick the capital
                if (nation.capitalId == targetTown.id) {
                    context.sendMessage(MessageFormatter.error("Cannot kick the capital from the nation"))
                    return@launch
                }

                // Remove town from nation
                plugin.nationService.removeTown(nation.id, targetTown.id)
                plugin.townService.setNation(targetTown.id, null)

                context.sendMessage(MessageFormatter.success("Town '$townName' has been kicked from '${nation.name}'"))

            } catch (e: Exception) {
                TownyLog.error("Failed to kick town", e)
                context.sendMessage(MessageFormatter.error("Failed to kick town: ${e.message}"))
            }
        }
    }
}
