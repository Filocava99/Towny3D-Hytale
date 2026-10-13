package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection

/**
 * /plot tax - Plot tax command collection
 */
class PlotTaxCommand : AbstractCommandCollection("tax", "Plot tax management") {
    init {
        requirePermission("towny3d.plot.tax")
        this.addSubCommand(PlotTaxInfoCommand())
        this.addSubCommand(PlotTaxClearCommand())
        this.addSubCommand(PlotTaxSetCommand())
    }
}
