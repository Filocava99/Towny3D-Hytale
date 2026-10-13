package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /ta chat - Admin chat commands
 */
class TaChatCommand : AbstractCommandCollection("chat", "Admin chat commands") {
    init {
        requirePermission("towny3d.admin")
        this.addSubCommand(TaChatSpyCommand())
    }
}

/**
 * /ta chat spy - Toggle chat spying
 */
class TaChatSpyCommand : AbstractWorldCommand {
    constructor() : super("spy", "Toggle chat spying") {
        requirePermission("towny3d.admin")
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
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                // Toggle spy mode
                val newSpyState = !resident.isSpy
                plugin.residentService.setSpy(resident.id, newSpyState)

                if (newSpyState) {
                    context.sendMessage(
                        MessageFormatter.success("Chat spy mode enabled")
                    )
                    context.sendMessage(MessageFormatter.info("You will now see all town and nation chat messages"))
                } else {
                    context.sendMessage(
                        MessageFormatter.success("Chat spy mode disabled")
                    )
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to toggle spy mode: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to toggle spy mode: ${e.message}"))
            }
        }
    }
}
