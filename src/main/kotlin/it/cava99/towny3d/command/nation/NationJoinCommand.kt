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
 * /nation join [nation] - Join a nation (as a town)
 * Note: Towns join nations, not individual players
 */
class NationJoinCommand : AbstractWorldCommand {

    private val nationArg = this.withRequiredArg("nation", "Nation name", ArgTypes.STRING)

    constructor() : super("join", "Join a nation") {
        requirePermission("towny3d.nation.join")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val nationName = nationArg.get(context) as? String

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
                    context.sendMessage(MessageFormatter.error("You must be a mayor of a town to join a nation"))
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
                    context.sendMessage(MessageFormatter.error("Only town mayors can join nations"))
                    return@launch
                }

                // Check if town is already in a nation
                if (town.hasNation) {
                    context.sendMessage(MessageFormatter.error("Your town is already in a nation"))
                    return@launch
                }

                // Find nation by name
                val nation = plugin.nationService.getNationByName(nationName!!)
                if (nation == null) {
                    context.sendMessage(MessageFormatter.error("Nation '$nationName' not found"))
                    return@launch
                }

                // Nations don't have an "open" setting - any town can join if invited
                // Skip this check

                // Add town to nation
                plugin.nationService.addTown(nation.id, town.id)
                plugin.townService.setNation(town.id, nation.id)

                context.sendMessage(MessageFormatter.success("Your town has joined nation '${nation.name}'!"))

            } catch (e: Exception) {
                TownyLog.error("Failed to join nation", e)
                context.sendMessage(MessageFormatter.error("Failed to join nation: ${e.message}"))
            }
        }
    }
}
