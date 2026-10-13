package it.cava99.towny3d.command.invite

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
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
 * /invite list - List pending invitations
 */
class InviteListCommand : AbstractWorldCommand {

    constructor() : super("list", "List pending invitations") {
        requirePermission("towny3d.invite.list")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use this command"))
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

                // Get pending town invitations
                val townInvites = plugin.invitationService.getTownInvitesForPlayer(resident.name)

                // Get pending nation invitations (if player's town is invited)
                val nationInvites = if (resident.townId != null) {
                    plugin.invitationService.getNationInvitesForTown(resident.townId!!)
                } else {
                    emptyList()
                }

                if (townInvites.isEmpty() && nationInvites.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("You have no pending invitations"))
                    return@launch
                }

                context.sendMessage(Message.raw(MessageFormatter.formatHeader("Pending Invitations (${townInvites.size + nationInvites.size})")))

                // Show town invites
                for (invite in townInvites) {
                    val town = plugin.townService.getTown(invite.townId)
                    val townName = town?.name ?: "Unknown"
                    val isPublic = town?.isPublic ?: false
                    context.sendMessage(
                        Message.raw("- Town invite from: ")
                            .insert(MessageFormatter.formatTown(townName, isPublic))
                            .insert(Message.raw(" (ID: ${invite.inviteId})"))
                    )
                }

                // Show nation invites
                for (invite in nationInvites) {
                    val nation = plugin.nationService.getNation(invite.nationId)
                    val inviteInfo = "Nation invite from: ${nation?.name ?: "Unknown"} (ID: ${invite.inviteId})"
                    context.sendMessage(Message.raw("- $inviteInfo"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to list invitations: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to list invitations: ${e.message}"))
            }
        }
    }
}
