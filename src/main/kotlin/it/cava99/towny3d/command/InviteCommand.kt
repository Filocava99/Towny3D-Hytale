package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.invite.*

/**
 * Registers all invite subcommands to a command collection.
 */
fun registerInviteSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(InviteListCommand())
    collection.addSubCommand(InviteSendCommand())
    collection.addSubCommand(AcceptCommand())
    collection.addSubCommand(DeclineCommand())
}

/**
 * Invitation management command collection.
 *
 * All subcommand classes have been extracted to separate files in the
 * it.cava99.towny3d.command.invite package for better organization and maintainability.
 */
class InviteCommand : AbstractCommandCollection("invite", "Manage invitations") {

    init {
        requirePermission("towny3d.invite")
        registerInviteSubcommands(this)
    }
}
