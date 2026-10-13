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
 * /town invite <player> or /town add <player> - Invite a player to town
 */
class TownInviteCommand : AbstractWorldCommand {

    private val playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING)

    constructor() : super("invite", "Invite player to town") {
        requirePermission("towny3d.town.invite")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val targetPlayerName = playerArg.get(context) as? String
        if (targetPlayerName.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town invite <player>"))
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
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to send invitations"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor or assistant
                if (!town.hasLeadership(resident.id)) {
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can send invitations"))
                    return@launch
                }

                // Create invite
                val inviteId = plugin.invitationService.inviteToTown(
                    townId = town.id,
                    inviterId = resident.id,
                    targetName = targetPlayerName
                )

                context.sendMessage(
                    MessageFormatter.success("Invited '")
                        .insert(Message.raw(targetPlayerName))
                        .insert(Message.raw("' to join '"))
                        .insert(MessageFormatter.formatTown(town.name, town.isPublic))
                        .insert(Message.raw("'"))
                )
                context.sendMessage(MessageFormatter.info("They can accept with /accept $inviteId"))

                // Notify the target player if they're online
                try {
                    val universe = com.hypixel.hytale.server.core.universe.Universe.get()
                    val allPlayers = universe.getPlayers()

                    for (player in allPlayers) {
                        if (player.username.equals(targetPlayerName, ignoreCase = true)) {
                            try {
                                player.sendMessage(
                                    MessageFormatter.success("You have been invited to join '")
                                        .insert(MessageFormatter.formatTown(town.name, town.isPublic))
                                        .insert(Message.raw("'"))
                                )
                                player.sendMessage(
                                    MessageFormatter.info("Accept with: ")
                                        .insert(Message.raw("/accept $inviteId").color("#55FFFF").bold(true))
                                )
                            } catch (e: Exception) {
                                TownyLog.warn("Failed to send invite notification to player: ${e.message}")
                            }
                            break
                        }
                    }
                } catch (e: Exception) {
                    TownyLog.warn("Failed to get players for invite notification: ${e.message}")
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to send invite: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to send invite: ${e.message}"))
            }
        }
    }
}
