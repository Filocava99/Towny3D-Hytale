package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import it.cava99.towny3d.command.claim.*

/**
 * Registers all claim subcommands to a command collection.
 */
fun registerClaimSubcommands(collection: AbstractCommandCollection) {
    collection.addSubCommand(ClaimHelpCommand())
    collection.addSubCommand(ClaimGcsCommand())
    collection.addSubCommand(ClaimLandCommand())
    collection.addSubCommand(ClaimAutoCommand())
    collection.addSubCommand(ClaimUnclaimCommand())
}

/**
 * Land claiming command collection.
 *
 * All subcommand classes have been extracted to separate files in the
 * it.cava99.towny3d.command.claim package for better organization and maintainability.
 */
class ClaimCommand : AbstractCommandCollection("claim", "Land claiming commands") {

    init {
        requirePermission("towny3d.claim")
        registerClaimSubcommands(this)
    }
}
