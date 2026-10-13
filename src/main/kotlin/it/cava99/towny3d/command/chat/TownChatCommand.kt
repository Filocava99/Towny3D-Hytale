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
 * Quick town chat command (/tc)
 * Toggles between town chat and global chat
 * OR sends a message to town chat if message is provided
 */
class TownChatCommand : AbstractWorldCommand {

    constructor() : super("tc", "Town chat") {
        requirePermission("towny3d.chat.town")
    }

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
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

                // For /tc and /nc, we'll just do toggle mode for now
                // TODO(2026-02-22): Add message argument support when we figure out how to get remaining args
                // Toggle chat mode
                when (resident.chatMode) {
                    ChatMode.TOWN -> {
                        // Already in town chat, switch to global
                        plugin.residentService.setChatMode(resident.id, ChatMode.GLOBAL)
                        context.sendMessage(MessageFormatter.info("Town chat disabled"))
                    }
                    else -> {
                        // Switch to town chat
                        if (resident.townId == null) {
                            context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                            return@launch
                        }
                        plugin.residentService.setChatMode(resident.id, ChatMode.TOWN)
                        context.sendMessage(MessageFormatter.success("Town chat enabled"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to handle town chat: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to handle town chat: ${e.message}"))
            }
        }
    }
}
