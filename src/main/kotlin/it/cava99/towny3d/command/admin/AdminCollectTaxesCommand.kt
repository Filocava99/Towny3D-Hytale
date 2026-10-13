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
 * /ta tax collecttaxes - Force immediate tax collection
 */
class AdminCollectTaxesCommand : AbstractWorldCommand {
    constructor() : super("collecttaxes", "Force immediate tax collection") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        // TODO(2026-02-22): Implement when TaxCollectionService is available
        // val result = plugin.taxCollectionService.collectTaxes()

        plugin.pluginScope.launch {
            try {
                context.sendMessage(MessageFormatter.info("Tax collection manually triggered"))
                context.sendMessage(MessageFormatter.info("This feature will be fully implemented once TaxCollectionService is integrated"))

                // Show current tax configuration
                val config = plugin.getConfig()
                context.sendMessage(
                    MessageFormatter.info("Tax collection interval: ${config.taxCollectionIntervalHours} hours")
                )
                context.sendMessage(
                    MessageFormatter.info("Tax collection time: ${config.upkeepTimeOfDay}")
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to trigger tax collection: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to trigger tax collection: ${e.message}"))
            }
        }
    }
}
