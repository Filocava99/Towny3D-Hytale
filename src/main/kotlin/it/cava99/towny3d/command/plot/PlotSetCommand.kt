package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection

/**
 * /plot set - Plot settings submenu
 */
class PlotSetCommand : AbstractCommandCollection("set", "Plot settings") {
    init {
        requirePermission("towny3d.plot.set")
        this.addSubCommand(PlotSetEmbassyCommand())
        this.addSubCommand(PlotSetPermCommand())
        this.addSubCommand(PlotSetResetCommand())
    }
}
