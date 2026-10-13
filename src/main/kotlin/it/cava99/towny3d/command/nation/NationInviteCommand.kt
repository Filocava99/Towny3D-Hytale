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
 * /nation invite <town> - Invite a town to your nation
 */
class NationInviteCommand : AbstractWorldCommand {

    private val townArg = this.withRequiredArg("town", "Town name", ArgTypes.STRING)

    constructor() : super("invite", "Invite a town to your nation") {
        requirePermission("towny3d.nation.invite")
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
                    context.sendMessage(MessageFormatter.error("Only the king can invite towns"))
                    return@launch
                }

                // Find target town by name
                val targetTown = plugin.townService.getTownByName(townName!!)
                if (targetTown == null) {
                    context.sendMessage(MessageFormatter.error("Town '$townName' not found"))
                    return@launch
                }

                // Check if target town is already in a nation
                if (targetTown.hasNation) {
                    context.sendMessage(MessageFormatter.error("Town '$townName' is already in a nation"))
                    return@launch
                }

                // Create invite
                val inviteId = plugin.invitationService.inviteTownToNation(
                    nationId = nation.id,
                    inviterId = resident.id,
                    targetTownId = targetTown.id
                )

                val inviteMsg = "Invited town '${targetTown.name}' to join '${nation.name}' (ID: $inviteId)"
                context.sendMessage(MessageFormatter.success(inviteMsg))
                context.sendMessage(MessageFormatter.info("The town mayor can accept with /accept $inviteId"))

            } catch (e: Exception) {
                TownyLog.error("Failed to invite town", e)
                context.sendMessage(MessageFormatter.error("Failed to invite town: ${e.message}"))
            }
        }
    }
}
