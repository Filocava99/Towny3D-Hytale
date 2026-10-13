package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.command.claim.ClaimLandCommand
import javax.annotation.Nonnull

/**
 * /town claim [chunk] - Claim land for town
 */
class TownClaimCommand : AbstractWorldCommand {

    private val chunkArg = this.withOptionalArg("chunk", "Claim entire chunk", ArgTypes.STRING)

    constructor() : super("claim", "Claim land for town") {
        requirePermission("towny3d.town.claim")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        // Delegate to /claim land command
        ClaimLandCommand().execute(context, world, store)
    }
}
