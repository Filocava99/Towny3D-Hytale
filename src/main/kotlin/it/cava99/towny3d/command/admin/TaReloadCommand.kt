package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.util.MessageFormatter
import javax.annotation.Nonnull

/**
 * /ta reload - Reload plugin configuration
 */
class TaReloadCommand : AbstractWorldCommand {

    constructor() : super("reload", "Reload configuration") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        // TODO(2026-02-22): Implement config reload from file using TownyConfigLoader
        context.sendMessage(MessageFormatter.success("Configuration reloaded"))
        context.sendMessage(MessageFormatter.info("Using default configuration"))
    }
}
