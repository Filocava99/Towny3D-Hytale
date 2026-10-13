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
 * /town here - Show info for town you're standing in
 */
class TownHereCommand : AbstractWorldCommand {
    constructor() : super("here", "Show town info at current location") {
        requirePermission("towny3d.town.here")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
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
                val chunkPos = it.cava99.towny3d.model.ChunkPos(blockX shr 4, blockZ shr 4)

                val regions = plugin.regionService.getRegionsAt(chunkPos)

                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("You are in the wilderness (unclaimed land)"))
                    return@launch
                }

                val region = regions.first()
                val town = plugin.townService.getTown(region.townId)

                if (town == null) {
                    context.sendMessage(MessageFormatter.error("This land is claimed but town data is missing"))
                    return@launch
                }

                // Show town info
                val mayorName = plugin.residentService.getResident(town.mayorId)?.name ?: "Unknown"

                context.sendMessage(Message.raw(MessageFormatter.formatHeader("${town.name}")))
                context.sendMessage(Message.raw("This territory belongs to ").insert(MessageFormatter.formatTown(town.name, town.isPublic)))
                context.sendMessage(Message.raw("Mayor: ").insert(Message.raw(mayorName).color("#AAFFAA")))
                context.sendMessage(Message.raw("Residents: ").insert(Message.raw("${town.residentIds.size}").color("#AAAAFF")))

                if (!town.isOpen) {
                    context.sendMessage(MessageFormatter.info("This town is closed (invite only)"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to get town info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get town info: ${e.message}"))
            }
        }
    }
}
