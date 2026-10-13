package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
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
 * /town kick <player> - Kick a resident from town
 */
class TownKickCommand : AbstractWorldCommand {

    private val playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING)

    constructor() : super("kick", "Kick resident from town") {
        requirePermission("towny3d.town.kick")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val targetPlayerName = playerArg.get(context) as? String
        if (targetPlayerName.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town kick <player>"))
            return
        }

        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayerBasic(context, store) ?: return
        val playerUuid = playerContext.uuid

        plugin.pluginScope.launch {
            try {
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor or assistant
                if (!town.hasLeadership(resident.id)) {
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can kick residents"))
                    return@launch
                }

                // Find target resident
                val targetResident = plugin.residentService.getResidentByName(targetPlayerName)
                if (targetResident == null) {
                    context.sendMessage(MessageFormatter.error("Player '$targetPlayerName' not found"))
                    return@launch
                }

                // Check if target is in the same town
                if (targetResident.townId != town.id) {
                    context.sendMessage(MessageFormatter.error("'$targetPlayerName' is not a member of your town"))
                    return@launch
                }

                // Can't kick the mayor
                if (town.mayorId == targetResident.id) {
                    context.sendMessage(MessageFormatter.error("You cannot kick the mayor"))
                    return@launch
                }

                // Remove from town
                plugin.townService.removeResident(town.id, targetResident.id)
                plugin.residentService.setTown(targetResident.id, null)

                context.sendMessage(
                    MessageFormatter.success("Kicked '")
                        .insert(Message.raw(targetPlayerName))
                        .insert(Message.raw("' from the town"))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to kick resident: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to kick resident: ${e.message}"))
            }
        }
    }
}
