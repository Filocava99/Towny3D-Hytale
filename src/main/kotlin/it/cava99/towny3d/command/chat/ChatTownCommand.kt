package it.cava99.towny3d.command.chat

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import it.cava99.towny3d.model.ChatMode
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /chat town - Switch to town chat
 */
class ChatTownCommand : AbstractWorldCommand {

    constructor() : super("town", "Switch to town chat") {
        requirePermission("towny3d.chat.town")
    }

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use chat commands"))
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

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to use town chat"))
                    return@launch
                }

                // Set chat mode
                plugin.residentService.setChatMode(resident.id, ChatMode.TOWN)

                context.sendMessage(MessageFormatter.success("Switched to town chat"))

            } catch (e: Exception) {
                TownyLog.error("Failed to switch chat mode: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to switch chat mode: ${e.message}"))
            }
        }
    }
}
