package it.cava99.towny3d.command.invite

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
 * /decline [inviteId] - Decline an invitation
 */
class DeclineCommand : AbstractWorldCommand {

    private val inviteIdArg = this.withOptionalArg("inviteId", "Invitation ID", ArgTypes.STRING)

    constructor() : super("decline", "Decline invitation") {
        requirePermission("towny3d.invite.decline")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val inviteIdInput = if (inviteIdArg.provided(context)) {
            inviteIdArg.get(context) as? String
        } else {
            null
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can decline invitations"))
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

                // Get invites - if no ID specified, decline the most recent
                val townInvites = plugin.invitationService.getTownInvitesForPlayer(resident.name)

                val inviteIdToDecline = if (inviteIdInput != null) {
                    try {
                        java.util.UUID.fromString(inviteIdInput)
                    } catch (e: Exception) {
                        context.sendMessage(MessageFormatter.error("Invalid invitation ID format"))
                        return@launch
                    }
                } else {
                    if (townInvites.isEmpty()) {
                        context.sendMessage(MessageFormatter.error("You have no pending invitations"))
                        return@launch
                    }
                    townInvites.first().inviteId
                }

                // Decline the invite
                val result = plugin.invitationService.rejectTownInvite(resident.id, inviteIdToDecline)

                if (result) {
                    context.sendMessage(MessageFormatter.success("Invitation declined"))
                } else {
                    context.sendMessage(MessageFormatter.error("Failed to decline invitation - not found or not for you"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to decline invitation: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to decline invitation: ${e.message}"))
            }
        }
    }
}
