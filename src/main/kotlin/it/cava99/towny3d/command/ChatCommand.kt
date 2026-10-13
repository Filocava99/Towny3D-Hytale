package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.chat.*

/**
 * Registers all chat subcommands to a command collection.
 */
fun registerChatSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(ChatTownCommand())
    collection.addSubCommand(ChatNationCommand())
    collection.addSubCommand(ChatGlobalCommand())
    collection.addSubCommand(TownChatCommand())
    collection.addSubCommand(NationChatCommand())
    collection.addSubCommand(GlobalChatCommand())
}

/**
 * Chat mode switching command collection.
 *
 * All subcommand classes have been extracted to separate files in the
 * it.cava99.towny3d.command.chat package for better organization and maintainability.
 */
class ChatCommand : AbstractCommandCollection("chat", "Switch chat mode") {

    init {
        requirePermission("towny3d.chat")
        registerChatSubcommands(this)
    }
}
