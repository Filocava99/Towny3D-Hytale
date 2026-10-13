package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.town.*

/**
 * Registers all town subcommands to a command collection.
 * Used by both TownCommand and TownAliasCommand to avoid duplication.
 */
fun registerTownSubcommands(collection: AbstractCommandCollection) {
    // Register all subcommands
    collection.addSubCommand(TownHelpCommand())
    collection.addSubCommand(TownCreateCommand())
    collection.addSubCommand(TownDeleteCommand()) // Also works as "disband"
    collection.addSubCommand(TownJoinCommand())
    collection.addSubCommand(TownLeaveCommand())
    collection.addSubCommand(TownListCommand())
    collection.addSubCommand(TownInfoCommand())

    // Mayor submenu
    collection.addSubCommand(TownMayorCommand())

    // Bank management
    collection.addSubCommand(TownDepositCommand())
    collection.addSubCommand(TownWithdrawCommand())

    // Spawn and location
    collection.addSubCommand(TownSetHomeCommand())
    collection.addSubCommand(TownSpawnCommand())
    collection.addSubCommand(TownOnlineCommand())
    collection.addSubCommand(TownHereCommand())

    // Player management
    collection.addSubCommand(TownKickCommand())
    collection.addSubCommand(TownInviteCommand()) // Also as "add"
    collection.addSubCommand(TownAddAssistantCommand())
    collection.addSubCommand(TownRemoveAssistantCommand())

    // Land management
    collection.addSubCommand(TownClaimCommand())
    collection.addSubCommand(TownUnclaimCommand())

    // Permissions and toggles (submenus)
    collection.addSubCommand(TownSetCommand())
    collection.addSubCommand(TownToggleCommand())

    // Tax management
    collection.addSubCommand(TownBalanceCommand())
    collection.addSubCommand(TownTaxCommandCollection())
}

/**
 * Main /town command collection.
 * Root command that registers all town subcommands.
 *
 * All subcommand classes have been extracted to separate files in the
 * it.cava99.towny3d.command.town package for better organization and maintainability.
 */
class TownCommand : AbstractCommandCollection("town", "Town management commands") {
    init {
        requirePermission("towny3d.town")
        registerTownSubcommands(this)
    }
}

/**
 * /t - Alias for /town command
 * Note: This is also defined in TownAliasCommand.kt in the town package.
 * This definition is kept for backward compatibility.
 */
class TownAliasCommand : AbstractCommandCollection("t", "Alias for /town") {
    init {
        requirePermission("towny3d.town")
        registerTownSubcommands(this)
    }
}
