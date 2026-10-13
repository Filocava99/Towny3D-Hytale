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
import javax.annotation.Nonnull

/**
 * /plot hideBorders or /plot hb - Hide plot borders
 */
class PlotHideBordersCommand : AbstractWorldCommand {
    constructor() : super("hideBorders", "Hide plot borders") {
        requirePermission("towny3d.plot.hide_borders")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayerBasic(context, store) ?: return
        val playerUuid = playerContext.uuid

        // Hide plot borders
        plugin.plotBorderRenderer.hidePlotBorders(playerUuid)
        context.sendMessage(MessageFormatter.success("Plot borders disabled"))
    }
}
