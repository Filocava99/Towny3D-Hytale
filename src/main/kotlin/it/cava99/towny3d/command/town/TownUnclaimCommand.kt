package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.command.claim.ClaimUnclaimCommand
import javax.annotation.Nonnull

/**
 * /town unclaim - Unclaim town land
 */
class TownUnclaimCommand : AbstractWorldCommand {
    constructor() : super("unclaim", "Unclaim town land") {
        requirePermission("towny3d.town.unclaim")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        // Delegate to /claim unclaim command
        ClaimUnclaimCommand().execute(context, world, store)
    }
}
