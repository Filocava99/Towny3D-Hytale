package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /ta save - Force save all data
 */
class TaSaveCommand : AbstractWorldCommand {

    constructor() : super("save", "Force save all data") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                // Save all data by flushing the batch writer
                plugin.saveAllData()

                context.sendMessage(MessageFormatter.success("All data saved successfully"))

            } catch (e: Exception) {
                TownyLog.error("Failed to save data: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to save data: ${e.message}"))
            }
        }
    }
}
