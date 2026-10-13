package it.cava99.towny3d.command.toggle

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
 * /toggle explosions - Toggle explosions
 */
class ToggleExplosionsCommand : AbstractWorldCommand {

    constructor() : super("explosions", "Toggle explosions") {
        requirePermission("towny3d.toggle.explosions")
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

        val playerUuid = CommandUtils.getPlayerUuid(playerRef, store)
        if (playerUuid == null) {
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
                    context.sendMessage(MessageFormatter.error("Only the mayor can toggle explosions"))
                    return@launch
                }

                // Toggle explosions for all town regions
                val regions = plugin.regionService.getRegionsByTown(town.id)
                val newExplosionsState = if (regions.isNotEmpty()) !regions.first().toggles.explosion else true

                for (region in regions) {
                    plugin.regionService.setToggleFlags(region.id, explosion = newExplosionsState)
                }

                val stateText = if (newExplosionsState) "enabled" else "disabled"
                context.sendMessage(MessageFormatter.success("Explosions $stateText for town '${town.name}'"))

            } catch (e: Exception) {
                TownyLog.error("Failed to toggle explosions: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to toggle explosions: ${e.message}"))
            }
        }
    }
}
