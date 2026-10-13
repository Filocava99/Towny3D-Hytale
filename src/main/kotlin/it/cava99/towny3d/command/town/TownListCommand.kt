package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /town list - List all towns
 */
class TownListCommand : AbstractWorldCommand {

    constructor() : super("list", "List all towns") {
        requirePermission("towny3d.town.list")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                val towns = plugin.townService.getAllTowns()

                if (towns.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("No towns exist yet."))
                    return@launch
                }

                context.sendMessage(Message.raw(MessageFormatter.formatHeader("Town List (${towns.size})")))
                for (town in towns.sortedBy { it.name }) {
                    val residentCount = town.residentIds.size
                    context.sendMessage(
                        Message.raw("- ")
                            .insert(MessageFormatter.formatTown(town.name, town.isPublic))
                            .insert(Message.raw(" ($residentCount residents)"))
                    )
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to list towns: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to list towns: ${e.message}"))
            }
        }
    }
}
