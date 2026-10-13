package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection

/**
 * Registers all admin tax subcommands to a command collection.
 */
fun registerTaTaxSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(AdminCollectTaxesCommand())
    collection.addSubCommand(AdminNextTaxCommand())
    collection.addSubCommand(AdminReclaimCommand())
    collection.addSubCommand(AdminRuinateCommand())
    collection.addSubCommand(AdminSetTaxTimeCommand())
}

/**
 * /ta tax - Tax collection commands
 *
 * All subcommand classes have been extracted to separate files in the
 * it.cava99.towny3d.command.admin package for better organization and maintainability.
 */
class TaTaxCommand : AbstractCommandCollection("tax", "Tax collection commands") {
    init {
        requirePermission("towny3d.admin")
        registerTaTaxSubcommands(this)
    }
}
