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
 * /town addAssistant <player> - Add an assistant
 */
class TownAddAssistantCommand : AbstractWorldCommand {

    private val playerArg = this.withRequiredArg("player", "Player name", ArgTypes.STRING)

    constructor() : super("addAssistant", "Add town assistant") {
        requirePermission("towny3d.town.add_assistant")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val targetPlayerName = playerArg.get(context) as? String
        if (targetPlayerName.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town addAssistant <player>"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can add assistants"))
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

                // Only mayor can add assistants
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can add assistants"))
                    return@launch
                }

                // Find target resident
                val targetResident = plugin.residentService.getResidentByName(targetPlayerName)
                if (targetResident == null) {
                    context.sendMessage(MessageFormatter.error("Player '$targetPlayerName' not found"))
                    return@launch
                }

                // Check if target is in the same town
                if (targetResident.townId != town.id) {
                    context.sendMessage(MessageFormatter.error("'$targetPlayerName' is not a member of your town"))
                    return@launch
                }

                // Check if already an assistant
                if (town.isAssistant(targetResident.id)) {
                    context.sendMessage(MessageFormatter.error("'$targetPlayerName' is already an assistant"))
                    return@launch
                }

                // Add assistant
                plugin.townService.addAssistant(town.id, targetResident.id)

                context.sendMessage(
                    MessageFormatter.success("Promoted '")
                        .insert(Message.raw(targetPlayerName))
                        .insert(Message.raw("' to assistant"))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to add assistant: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to add assistant: ${e.message}"))
            }
        }
    }
}
