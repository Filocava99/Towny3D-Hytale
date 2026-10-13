package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.command.claim.ClaimGcsCommand
import javax.annotation.Nonnull

/**
 * /town mayor - Mayor management submenu
 */
class TownMayorCommand : AbstractCommandCollection("mayor", "Mayor management commands") {
    init {
        requirePermission("towny3d.town")
        this.addSubCommand(TownMayorHelpCommand())
        this.addSubCommand(TownMayorGcsCommand())
        // Note: addAssistant and removeAssistant are top-level /town commands in Towny3D
        // but we also support them under /town mayor for completeness
    }
}

/**
 * /town mayor help - Show mayor command help
 */
class TownMayorHelpCommand : AbstractWorldCommand {
    constructor() : super("help", "Show mayor command help")

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        context.sendMessage(Message.raw("=== Mayor Commands ===").color("#FFAA00").bold(true))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/town mayor gcs - Get claim stick to select plot points").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("Left click with the stick to set the first corner").color("#AAAAAA"))
        context.sendMessage(Message.raw("Press F with the stick to set the second corner").color("#AAAAAA"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("The claim stick allows 3D region selection for claiming land").color("#AAAAAA"))
    }
}

/**
 * /town mayor gcs - Get claim stick
 */
class TownMayorGcsCommand : AbstractWorldCommand {
    constructor() : super("gcs", "Get claim stick")

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        // Delegate to /claim gcs command
        ClaimGcsCommand().execute(context, world, store)
    }
}
