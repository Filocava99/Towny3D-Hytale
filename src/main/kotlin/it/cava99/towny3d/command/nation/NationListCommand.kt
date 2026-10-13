package it.cava99.towny3d.command.nation

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
 * /nation list - List all nations
 */
class NationListCommand : AbstractWorldCommand {

    constructor() : super("list", "List all nations") {
        requirePermission("towny3d.nation.list")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                val nations = plugin.nationService.getAllNations()

                if (nations.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("No nations exist yet."))
                    return@launch
                }

                context.sendMessage(Message.raw(MessageFormatter.formatHeader("Nation List (${nations.size})")))
                for (nation in nations.sortedBy { it.name }) {
                    val townCount = nation.townIds.size
                    val line = "- ${nation.name} ($townCount towns)\n"
                    context.sendMessage(Message.raw(line))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to list nations", e)
                context.sendMessage(MessageFormatter.error("Failed to list nations: ${e.message}"))
            }
        }
    }
}
