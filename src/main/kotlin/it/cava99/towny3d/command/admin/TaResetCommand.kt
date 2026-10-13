package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /ta reset <player> - Reset player's town data
 */
class TaResetCommand : AbstractWorldCommand {

    private val playerArg = this.withOptionalArg("player", "Player name", ArgTypes.STRING)

    constructor() : super("reset", "Reset player's town data") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val targetPlayerName = if (playerArg.provided(context)) {
            playerArg.get(context) as? String
        } else {
            context.sendMessage(MessageFormatter.error("Usage: /ta reset <player>"))
            context.sendMessage(MessageFormatter.info("Example: /ta reset PlayerName"))
            return
        }

        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                // Find target resident by name
                val targetResident = plugin.residentService.getResidentByName(targetPlayerName!!)
                if (targetResident == null) {
                    context.sendMessage(MessageFormatter.error("Player '$targetPlayerName' not found"))
                    return@launch
                }

                // Check if player has a town
                if (targetResident.townId != null) {
                    val town = plugin.townService.getTown(targetResident.townId!!)
                    if (town != null) {
                        // Check if player is mayor
                        if (town.mayorId == targetResident.id) {
                            // Delete the town
                            for (residentId in town.residentIds) {
                                plugin.residentService.setTown(residentId, null)
                            }
                            plugin.townService.deleteTown(town.id)
                            context.sendMessage(MessageFormatter.info("Deleted town '${town.name}'"))
                        } else {
                            // Just remove from town
                            plugin.townService.removeResident(town.id, targetResident.id)
                            context.sendMessage(MessageFormatter.info("Removed '$targetPlayerName' from town '${town.name}'"))
                        }
                    }
                }

                // Reset resident's town
                plugin.residentService.setTown(targetResident.id, null)

                context.sendMessage(MessageFormatter.success("Reset '$targetPlayerName's town data"))

            } catch (e: Exception) {
                TownyLog.error("Failed to reset player: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to reset player: ${e.message}"))
            }
        }
    }
}
