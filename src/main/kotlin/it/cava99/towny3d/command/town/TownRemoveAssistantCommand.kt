package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /town removeAssistant <player> - Remove an assistant
 */
class TownRemoveAssistantCommand : AbstractWorldCommand {

    private val playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING)

    constructor() : super("removeAssistant", "Remove town assistant") {
        requirePermission("towny3d.town.remove_assistant")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val targetPlayerName = playerArg.get(context) as? String
        if (targetPlayerName.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town removeAssistant <player>"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can remove assistants"))
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

                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Only mayor can remove assistants
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can remove assistants"))
                    return@launch
                }

                // Find target resident
                val targetResident = plugin.residentService.getResidentByName(targetPlayerName)
                if (targetResident == null) {
                    context.sendMessage(MessageFormatter.error("Player '$targetPlayerName' not found"))
                    return@launch
                }

                // Check if is an assistant
                if (!town.isAssistant(targetResident.id)) {
                    context.sendMessage(MessageFormatter.error("'$targetPlayerName' is not an assistant"))
                    return@launch
                }

                // Remove assistant
                plugin.townService.removeAssistant(town.id, targetResident.id)

                context.sendMessage(
                    MessageFormatter.success("Demoted '")
                        .insert(Message.raw(targetPlayerName))
                        .insert(Message.raw("' from assistant"))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to remove assistant: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to remove assistant: ${e.message}"))
            }
        }
    }
}
