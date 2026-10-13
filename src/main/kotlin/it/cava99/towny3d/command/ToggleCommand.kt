package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.toggle.*

/**
 * Registers all toggle subcommands to a command collection.
 */
fun registerToggleSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(TogglePvPCommand())
    collection.addSubCommand(TogglePublicCommand())
    collection.addSubCommand(ToggleExplosionsCommand())
    collection.addSubCommand(ToggleOpenCommand())
}

/**
 * Toggle settings command collection.
 *
 * All subcommand classes have been extracted to separate files in the
 * it.cava99.towny3d.command.toggle package for better organization and maintainability.
 */
class ToggleCommand : AbstractCommandCollection("toggle", "Toggle settings") {

    init {
        requirePermission("towny3d.toggle")
        registerToggleSubcommands(this)
    }
}
