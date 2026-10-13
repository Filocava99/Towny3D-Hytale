package it.cava99.towny3d.command.toggle

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
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
 * /toggle open - Toggle town open status (for joining)
 * This is similar to /toggle public but specifically for town joining
 */
class ToggleOpenCommand : AbstractWorldCommand {

    constructor() : super("open", "Toggle town open status") {
        requirePermission("towny3d.toggle.open")
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
                // Get resident
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town"))
                    return@launch
                }

                // Get player's town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can toggle open status"))
                    return@launch
                }

                // Toggle town open status
                val newOpenState = !town.isOpen
                plugin.townService.setOpen(town.id, newOpenState)

                val stateText = if (newOpenState) "open" else "closed"
                val infoText = if (newOpenState) {
                    "Players can now join without an invite"
                } else {
                    "Players now need an invite to join"
                }
                context.sendMessage(MessageFormatter.success("Town is now $stateText. $infoText."))

            } catch (e: Exception) {
                TownyLog.error("Failed to toggle open status: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to toggle open status: ${e.message}"))
            }
        }
    }
}
