package it.cava99.towny3d.command.nation

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import javax.annotation.Nonnull

/**
 * /nation help - Show nation command help
 */
class NationHelpCommand : AbstractWorldCommand {

    constructor() : super("help", "Show nation command help") {
        requirePermission("towny3d.nation")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        context.sendMessage(Message.raw("=== Nation Commands ===").color("#FFAA00").bold(true))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/nation create <name> - Create a new nation").color("#55FFFF"))
        context.sendMessage(Message.raw("/nation delete - Delete your nation").color("#55FFFF"))
        context.sendMessage(Message.raw("/nation join <nation> - Join a nation").color("#55FFFF"))
        context.sendMessage(Message.raw("/nation leave - Leave your nation").color("#55FFFF"))
        context.sendMessage(Message.raw("/nation list - List all nations").color("#55FFFF"))
        context.sendMessage(Message.raw("/nation info [nation] - View nation information").color("#55FFFF"))
        context.sendMessage(Message.raw("/nation invite <town> - Invite a town to your nation").color("#55FFFF"))
        context.sendMessage(Message.raw("/nation kick <town> - Kick a town from your nation").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("Use /nation <subcommand> for more details on each command.").color("#AAAAAA"))
    }
}
