package it.cava99.towny3d.command.chat

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import javax.annotation.Nonnull

/**
 * Quick global chat command (/g)
 */
class GlobalChatCommand : AbstractWorldCommand {

    constructor() : super("g", "Global chat") {
        requirePermission("towny3d.chat.global")
    }

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        // Delegate to the chat global command
        ChatGlobalCommand().execute(context, world, store)
    }
}
