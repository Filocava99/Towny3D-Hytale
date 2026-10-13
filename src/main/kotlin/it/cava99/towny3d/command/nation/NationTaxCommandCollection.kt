package it.cava99.towny3d.command.nation

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection

/**
 * /nation tax - Tax management submenu
 */
class NationTaxCommandCollection : AbstractCommandCollection("tax", "Tax management commands") {
    init {
        requirePermission("towny3d.nation.tax")
        this.addSubCommand(NationTaxCommand())
        this.addSubCommand(NationTaxClearCommand())
        this.addSubCommand(NationTaxInfoCommand())
    }
}
