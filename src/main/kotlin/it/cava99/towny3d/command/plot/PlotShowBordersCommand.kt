package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandValidation
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /plot showBorders or /plot sb - Show plot borders
 */
class PlotShowBordersCommand : AbstractWorldCommand {
    constructor() : super("showBorders", "Show plot borders") {
        requirePermission("towny3d.plot.show_borders")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerUuid = playerContext.uuid
        val playerPosition = playerContext.position

        // Get username from PlayerRef component
        val playerRefComponent = store.getComponent(playerContext.ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType()) as? com.hypixel.hytale.server.core.universe.PlayerRef
        if (playerRefComponent == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference component"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                val regions = plugin.regionService.getRegionsAt(chunkPos)

                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("No plot at current location"))
                    return@launch
                }

                // Start showing plot borders
                plugin.plotBorderRenderer.showPlotBorders(playerUuid, playerRefComponent.username)
                context.sendMessage(MessageFormatter.success("Plot borders enabled! The borders will update as you move."))

            } catch (e: Exception) {
                TownyLog.error("Failed to show borders", e)
                context.sendMessage(MessageFormatter.error("Failed to show borders: ${e.message}"))
            }
        }
    }
}
