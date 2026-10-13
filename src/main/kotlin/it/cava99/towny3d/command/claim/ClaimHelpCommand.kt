package it.cava99.towny3d.command.claim

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import javax.annotation.Nonnull

/**
 * /claim help - Show claiming command help
 */
class ClaimHelpCommand : AbstractWorldCommand {

    constructor() : super("help", "Show claiming command help") {
        requirePermission("towny3d.claim")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        context.sendMessage(Message.raw("=== Claim Commands ===").color("#FFAA00").bold(true))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/claim gcs - Get claim stick for 3D selection").color("#55FFFF"))
        context.sendMessage(Message.raw("/claim land - Claim selected region or current chunk").color("#55FFFF"))
        context.sendMessage(Message.raw("/claim auto [on|off] - Toggle auto-claim (not yet implemented)").color("#55FFFF"))
        context.sendMessage(Message.raw("/claim unclaim - Unclaim selected region").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("=== Selection Commands ===").color("#FFAA00").bold(true))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/p1 - Set selection point 1").color("#55FFFF"))
        context.sendMessage(Message.raw("/p2 - Set selection point 2").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("Plot selection allows claiming specific Y-ranges and multi-chunk areas").color("#AAAAAA"))
        context.sendMessage(Message.raw("Use /p1 and /p2 to define corners, then /claim land to claim the region").color("#AAAAAA"))
    }
}
