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
 * /accept [inviteId] - Accept an invitation
 */
class AcceptCommand : AbstractWorldCommand {

    private val inviteIdArg = this.withOptionalArg("inviteId", "Invitation ID", ArgTypes.STRING)

    constructor() : super("accept", "Accept invitation") {
        requirePermission("towny3d.invite.accept")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val inviteIdInput = if (inviteIdArg.provided(context)) {
            inviteIdArg.get(context) as? String
        } else {
            null
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can accept invitations"))
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

                // Check if player is already in a town
                if (resident.townId != null) {
                    context.sendMessage(MessageFormatter.error("You are already in a town. Leave it first."))
                    return@launch
                }

                // Get invites - if no ID specified, try to accept the most recent
                val townInvites = plugin.invitationService.getTownInvitesForPlayer(resident.name)

                val inviteIdToAccept = if (inviteIdInput != null) {
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

                // Accept the invite
                val result = plugin.invitationService.acceptTownInvite(resident.id, inviteIdToAccept)

                when (result) {
                    it.cava99.towny3d.service.InvitationService.AcceptResult.SUCCESS -> {
                        // Get the invite to find town info
                        val invite = plugin.invitationService.getTownInvite(inviteIdToAccept)
                        if (invite != null) {
                            val town = plugin.townService.getTown(invite.townId)
                            context.sendMessage(MessageFormatter.success("You have joined town '${town?.name ?: "Unknown"}'!"))
                        } else {
                            context.sendMessage(MessageFormatter.success("Invitation accepted"))
                        }
                    }
                    it.cava99.towny3d.service.InvitationService.AcceptResult.NOT_FOUND -> {
                        context.sendMessage(MessageFormatter.error("Invitation not found or has expired"))
                    }
                    it.cava99.towny3d.service.InvitationService.AcceptResult.EXPIRED -> {
                        context.sendMessage(MessageFormatter.error("Invitation has expired"))
                    }
                    it.cava99.towny3d.service.InvitationService.AcceptResult.ALREADY_IN_TOWN -> {
                        context.sendMessage(MessageFormatter.error("You are already in a town"))
                    }
                    else -> {
                        context.sendMessage(MessageFormatter.error("Failed to accept invitation"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to accept invitation: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to accept invitation: ${e.message}"))
            }
        }
    }
}
