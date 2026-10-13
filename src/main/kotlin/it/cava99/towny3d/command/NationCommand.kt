package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.nation.*

/**
 * Main /nation command collection.
 * Root command that registers all nation subcommands.
 */
class NationCommand : AbstractCommandCollection("nation", "Nation management commands") {

    init {
        requirePermission("towny3d.nation")
        registerNationSubcommands(this)
    }
}

/**
 * Registers all nation subcommands to the given collection.
 * This function can be reused by other command collections that need
 * to register the same nation subcommands (e.g., aliases).
 */
fun registerNationSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(NationHelpCommand())
    collection.addSubCommand(NationCreateCommand())
    collection.addSubCommand(NationDeleteCommand())
    collection.addSubCommand(NationJoinCommand())
    collection.addSubCommand(NationLeaveCommand())
    collection.addSubCommand(NationListCommand())
    collection.addSubCommand(NationInfoCommand())
    collection.addSubCommand(NationInviteCommand())
    collection.addSubCommand(NationKickCommand())
    collection.addSubCommand(NationTaxCommandCollection())
    collection.addSubCommand(NationDebtCommand())
    collection.addSubCommand(NationBalanceCommand())
}
