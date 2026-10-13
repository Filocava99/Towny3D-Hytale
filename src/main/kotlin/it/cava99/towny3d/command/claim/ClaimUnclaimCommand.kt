package it.cava99.towny3d.command.claim

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /claim unclaim - Unclaim current chunk
 */
class ClaimUnclaimCommand : AbstractWorldCommand {

    constructor() : super("unclaim", "Unclaim current chunk") {
        requirePermission("towny3d.claim.unclaim")
    }

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can unclaim land"))
            return
        }

        val playerRef = context.senderAsPlayerRef()
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference"))
            return
        }

        val playerUuid = CommandUtils.getPlayerUuid(playerRef, store)
        val playerPosition = CommandUtils.getPlayerPosition(playerRef, store)

        if (playerUuid == null || playerPosition == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return
        }

        plugin.pluginScope.launch {
            try {
                // Get resident
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town"))
                    return@launch
                }

                // Get player's town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can unclaim town land"))
                    return@launch
                }

                // Get current location
                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos(blockX shr 5, blockZ shr 5)

                // Check if location is in a region
                val regions = plugin.regionService.getRegionsAt(chunkPos)
                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.error("This land is not claimed"))
                    return@launch
                }

                val region = regions.first()

                // Check if region belongs to player's town
                if (region.townId != resident.townId) {
                    context.sendMessage(MessageFormatter.error("This land does not belong to your town"))
                    return@launch
                }

                // Check if it's a home region (cannot unclaim home region)
                if (region.type == it.cava99.towny3d.model.RegionType.HOME) {
                    context.sendMessage(MessageFormatter.error("Cannot unclaim home region. Use /town delete to delete the town."))
                    return@launch
                }

                // Delete the region
                plugin.regionService.deleteRegion(region.id)

                context.sendMessage(MessageFormatter.success("Unclaimed this chunk"))

            } catch (e: Exception) {
                TownyLog.error("Failed to unclaim: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to unclaim: ${e.message}"))
            }
        }
    }
}
