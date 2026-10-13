package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
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
 * /town toggle <flag> <value?> - Toggle town flags
 */
class TownToggleCommand : AbstractWorldCommand {

    private val flagArg = this.withOptionalArg("flag", "Flag to toggle", ArgTypes.STRING)
    private val valueArg = this.withOptionalArg("value", "on/off (optional)", ArgTypes.STRING)

    constructor() : super("toggle", "Toggle town settings") {
        requirePermission("towny3d.town.toggle")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val flag = if (flagArg.provided(context)) flagArg.get(context) as? String else null
        val value = if (valueArg.provided(context)) valueArg.get(context) as? String else null

        if (flag.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town toggle <flag> [on/off]"))
            context.sendMessage(MessageFormatter.info("Available flags: pvp, explosions, fire, mobs"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can toggle settings"))
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
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can toggle town settings"))
                    return@launch
                }

                // Determine the new state
                val newState = when (value?.lowercase()) {
                    "on", "true", "yes", "1" -> true
                    "off", "false", "no", "0" -> false
                    null -> null // Toggle current state
                    else -> {
                        context.sendMessage(MessageFormatter.error("Invalid value. Use: on or off"))
                        return@launch
                    }
                }

                // Apply the toggle based on flag type
                when (flag.lowercase()) {
                    "pvp" -> {
                        // Toggle PvP for all town regions (stored in region toggles)
                        val regions = plugin.regionService.getRegionsByTown(town.id)
                        val newPvPState = if (newState != null) {
                            newState
                        } else {
                            if (regions.isNotEmpty()) !regions.first().toggles.pvp else true
                        }

                        for (region in regions) {
                            plugin.regionService.setToggleFlags(region.id, pvp = newPvPState)
                        }

                        context.sendMessage(
                            MessageFormatter.success("PvP ${if (newPvPState) "enabled" else "disabled"} for town '${town.name}'")
                        )
                    }
                    "explosions", "explosion" -> {
                        // Toggle explosions for all town regions
                        val regions = plugin.regionService.getRegionsByTown(town.id)
                        val newExplosionsState = if (newState != null) {
                            newState
                        } else {
                            if (regions.isNotEmpty()) !regions.first().toggles.explosion else true
                        }

                        for (region in regions) {
                            plugin.regionService.setToggleFlags(region.id, explosion = newExplosionsState)
                        }

                        context.sendMessage(
                            MessageFormatter.success("Explosions ${if (newExplosionsState) "enabled" else "disabled"} for town '${town.name}'")
                        )
                    }
                    "public" -> {
                        // Toggle publicity (stored on town)
                        val newPublicState = if (newState != null) newState else !town.isPublic
                        plugin.townService.setPublic(town.id, newPublicState)

                        val stateText = if (newPublicState) "public" else "private"
                        context.sendMessage(
                            MessageFormatter.success("Town is now $stateText")
                        )
                    }
                    "open" -> {
                        // Toggle open status (stored on town)
                        val newOpenState = if (newState != null) newState else !town.isOpen
                        plugin.townService.setOpen(town.id, newOpenState)

                        val stateText = if (newOpenState) "open" else "closed"
                        val infoText = if (newOpenState) {
                            "Players can now join without an invite"
                        } else {
                            "Players now need an invite to join"
                        }
                        context.sendMessage(
                            MessageFormatter.success("Town is now $stateText. $infoText.")
                        )
                    }
                    "fire", "mobs" -> {
                        context.sendMessage(MessageFormatter.error("Plot-specific flags (fire, mobs) must be set via /plot toggle"))
                    }
                    else -> {
                        context.sendMessage(MessageFormatter.error("Unknown flag. Available: pvp, explosions, public, open"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to toggle: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to toggle: ${e.message}"))
            }
        }
    }
}
