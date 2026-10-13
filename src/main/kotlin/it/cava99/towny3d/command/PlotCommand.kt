package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.plot.*

/**
 * Plot management command collection.
 * Plot commands are for managing individual plots within a town's territory.
 */
class PlotCommand : AbstractCommandCollection("plot", "Plot management commands") {

    init {
        requirePermission("towny3d.plot")
        registerPlotSubcommands(this)
    }
}

/**
 * Registers all plot subcommands to the given collection.
 * This function can be reused by other command collections that need
 * to register the same plot subcommands (e.g., aliases).
 */
fun registerPlotSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(PlotHelpCommand())
    collection.addSubCommand(PlotClaimCommand())
    collection.addSubCommand(PlotUnclaimCommand())
    collection.addSubCommand(PlotForSaleCommand())
    collection.addSubCommand(PlotNotForSaleCommand())
    collection.addSubCommand(PlotInfoCommand())
    collection.addSubCommand(PlotSetCommand())
    collection.addSubCommand(PlotToggleCommand())
    collection.addSubCommand(PlotNameCommand())
    val showBordersCmd = PlotShowBordersCommand()
    showBordersCmd.addAliases("sb")
    collection.addSubCommand(showBordersCmd)
    val hideBordersCmd = PlotHideBordersCommand()
    hideBordersCmd.addAliases("hb")
    collection.addSubCommand(hideBordersCmd)
    // Plot tax commands
    collection.addSubCommand(PlotTaxCommand())
}
