package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.RegionType
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /ta unclaim [radius] - Unclaim land by radius
 */
class TaUnclaimCommand : AbstractWorldCommand {

    private val radiusArg = this.withOptionalArg("radius", "Radius", ArgTypes.INTEGER)

    constructor() : super("unclaim", "Unclaim land by radius") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val radius = if (radiusArg.provided(context)) {
            radiusArg.get(context) as? Int ?: 1
        } else {
            1
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use this command"))
            return
        }

        val playerRef = context.senderAsPlayerRef()
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference"))
            return
        }

        val playerPosition = CommandUtils.getPlayerPosition(playerRef, store)
        if (playerPosition == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player position"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val centerChunkX = blockX shr 4
                val centerChunkZ = blockZ shr 4

                var unclaimedCount = 0

                // Unclaim all regions in radius
                for (dx in -radius..radius) {
                    for (dz in -radius..radius) {
                        val chunkPos = ChunkPos(centerChunkX + dx, centerChunkZ + dz)
                        val regions = plugin.regionService.getRegionsAt(chunkPos)

                        for (region in regions) {
                            // Don't unclaim home regions through this command
                            if (region.type != RegionType.HOME) {
                                plugin.regionService.deleteRegion(region.id)
                                unclaimedCount++
                            }
                        }
                    }
                }

                context.sendMessage(MessageFormatter.success("Unclaimed $unclaimedCount region(s)"))

            } catch (e: Exception) {
                TownyLog.error("Failed to unclaim: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to unclaim: ${e.message}"))
            }
        }
    }
}
