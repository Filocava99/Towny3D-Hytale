package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.tax.*

/**
 * Registers all tax subcommands to a command collection.
 */
fun registerTaxSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(TaxPersonalCommand())
    collection.addSubCommand(TaxTownCommand())
    collection.addSubCommand(TaxNationCommand())
}

/**
 * Tax command collection.
 * Tax commands allow residents to view their personal tax burden and tax information for towns and nations.
 *
 * All subcommand classes have been extracted to separate files in the
 * it.cava99.towny3d.command.tax package for better organization and maintainability.
 */
class TaxCommand : AbstractCommandCollection("tax", "Tax information commands") {

    init {
        requirePermission("towny3d.tax")
        registerTaxSubcommands(this)
    }
}
