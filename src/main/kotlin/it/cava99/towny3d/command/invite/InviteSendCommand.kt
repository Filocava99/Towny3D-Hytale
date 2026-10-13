package it.cava99.towny3d.command.invite

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /invite send [player] - Send invitation to a player
 */
class InviteSendCommand : AbstractWorldCommand {

    private val playerArg = this.withOptionalArg("player", "Player name", ArgTypes.STRING)

    constructor() : super("send", "Send invitation") {
        requirePermission("towny3d.invite.send")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val targetPlayerName = if (playerArg.provided(context)) {
            playerArg.get(context) as? String
        } else {
            context.sendMessage(MessageFormatter.error("Usage: /invite send <player>"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can send invitations"))
            return
        }

        val playerRef = context.senderAsPlayerRef()
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference"))
            return
        }

        val playerUuid = CommandUtils.getPlayerUuid(playerRef, store)
        if (playerUuid == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return
        }

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
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to send invitations"))
                    return@launch
                }

                // Get player's town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can send invitations"))
                    return@launch
                }

                // Create invite
                val inviteId = plugin.invitationService.inviteToTown(
                    townId = town.id,
                    inviterId = resident.id,
                    targetName = targetPlayerName!!
                )

                context.sendMessage(
                    MessageFormatter.success("Invited '")
                        .insert(Message.raw(targetPlayerName))
                        .insert(Message.raw("' to join '"))
                        .insert(MessageFormatter.formatTown(town.name, town.isPublic))
                        .insert(Message.raw("' (ID: $inviteId)"))
                )
                context.sendMessage(MessageFormatter.info("They can accept with /accept $inviteId"))

            } catch (e: Exception) {
                TownyLog.error("Failed to send invite: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to send invite: ${e.message}"))
            }
        }
    }
}
