package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.admin.TaChatCommand
import it.cava99.towny3d.command.admin.TaDataCommand
import it.cava99.towny3d.command.admin.TaReloadCommand
import it.cava99.towny3d.command.admin.TaResetCommand
import it.cava99.towny3d.command.admin.TaSaveCommand
import it.cava99.towny3d.command.admin.TaStatsCommand
import it.cava99.towny3d.command.admin.TaTaxCommand
import it.cava99.towny3d.command.admin.TaToggleCommand
import it.cava99.towny3d.command.admin.TaUnclaimCommand

/**
 * Root admin command collection for Towny3D.
 * Provides access to all admin-level commands for managing towns, nations, and server settings.
 *
 * Usage: /ta [subcommand]
 * Permission: towny3d.admin
 */
class AdminCommand : AbstractCommandCollection("ta", "Town admin commands") {

    init {
        requirePermission("towny3d.admin")

        // Core admin commands
        this.addSubCommand(TaReloadCommand())
        this.addSubCommand(TaSaveCommand())
        this.addSubCommand(TaStatsCommand())
        this.addSubCommand(TaUnclaimCommand())
        this.addSubCommand(TaResetCommand())
        this.addSubCommand(TaChatCommand())
        this.addSubCommand(TaSetLangCommand())

        // Tax and upkeep commands
        this.addSubCommand(TaTaxCommand())
        this.addSubCommand(TaToggleCommand())

        // Data management commands
        this.addSubCommand(TaDataCommand())
    }
}
