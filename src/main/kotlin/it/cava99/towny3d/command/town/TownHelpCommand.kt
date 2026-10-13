package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.util.MessageFormatter
import javax.annotation.Nonnull

/**
 * /town help - Show town command help
 */
class TownHelpCommand : AbstractWorldCommand {

    constructor() : super("help", "Show town command help") {
        requirePermission("towny3d.town")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        context.sendMessage(Message.raw("=== Town Commands ===").color("#FFAA00").bold(true))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/town create <name> - Create a new town").color("#55FFFF"))
        context.sendMessage(Message.raw("/town delete - Delete your town").color("#55FFFF"))
        context.sendMessage(Message.raw("/town join <town> - Join a town").color("#55FFFF"))
        context.sendMessage(Message.raw("/town leave - Leave your town").color("#55FFFF"))
        context.sendMessage(Message.raw("/town list - List all towns").color("#55FFFF"))
        context.sendMessage(Message.raw("/town info [town] - View town information").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("=== Town Concepts ===").color("#FFAA00").bold(true))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("Layers: Volume measurement for 3D claiming").color("#55FFFF"))
        context.sendMessage(Message.raw("  - 1 layer = chunk width x chunk width x 1 block height").color("#AAAAAA"))
        context.sendMessage(Message.raw("  - With 32x32 chunks: 1 layer = 1,024 blocks (32x32x1)").color("#AAAAAA"))
        context.sendMessage(Message.raw("  - Distribute layers across chunks and heights freely").color("#AAAAAA"))
        context.sendMessage(Message.raw("  - Example: 100 layers could be 1 chunk x 100 height OR 2 chunks x 50 height").color("#AAAAAA"))
        context.sendMessage(Message.raw("  - More layers = more total blocks your town can claim").color("#AAAAAA"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("Use /town <subcommand> for more details on each command.").color("#AAAAAA"))
    }
}
