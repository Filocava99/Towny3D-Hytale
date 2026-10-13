package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
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
 * /town setHome - Set town spawn point
 */
class TownSetHomeCommand : AbstractWorldCommand {
    constructor() : super("setHome", "Set town spawn point") {
        requirePermission("towny3d.town.set_home")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can set town spawn"))
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
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor or assistant
                if (!town.hasLeadership(resident.id)) {
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can set town spawn"))
                    return@launch
                }

                // Check if player is in town territory
                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos(blockX shr 4, blockZ shr 4)
                val regions = plugin.regionService.getRegionsAt(chunkPos)

                if (regions.isEmpty() || regions.first().townId != town.id) {
                    context.sendMessage(MessageFormatter.error("You must be in your town's territory to set spawn"))
                    return@launch
                }

                // Set the new home location
                val newHomeLocation = it.cava99.towny3d.model.BlockLocation(
                    plugin.getConfig().defaultWorldName,
                    blockX,
                    playerPosition.y.toInt(),
                    blockZ
                )

                plugin.townService.setHomeLocation(town.id, newHomeLocation)

                context.sendMessage(
                    MessageFormatter.success("Town spawn set to your current location")
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to set home: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to set home: ${e.message}"))
            }
        }
    }
}
