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
 * Quick nation chat command (/nc)
 * Toggles between nation chat and global chat
 * OR sends a message to nation chat if message is provided
 */
class NationChatCommand : AbstractWorldCommand {

    constructor() : super("nc", "Nation chat") {
        requirePermission("towny3d.chat.nation")
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

                // For /nc, just toggle mode for now
                // TODO(2026-02-22): Add message argument support
                // Toggle chat mode
                when (resident.chatMode) {
                    ChatMode.NATION -> {
                        // Already in nation chat, switch to global
                        plugin.residentService.setChatMode(resident.id, ChatMode.GLOBAL)
                        context.sendMessage(MessageFormatter.info("Nation chat disabled"))
                    }
                    else -> {
                        // Switch to nation chat
                        if (resident.townId == null) {
                            context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                            return@launch
                        }

                        val town = plugin.townService.getTown(resident.townId!!)
                        if (town == null) {
                            context.sendMessage(MessageFormatter.error("Town not found"))
                            return@launch
                        }

                        if (!town.hasNation || town.nationId == null) {
                            context.sendMessage(MessageFormatter.error("Your town is not a member of any nation"))
                            return@launch
                        }

                        plugin.residentService.setChatMode(resident.id, ChatMode.NATION)
                        context.sendMessage(MessageFormatter.success("Nation chat enabled"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to handle nation chat: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to handle nation chat: ${e.message}"))
            }
        }
    }
}
