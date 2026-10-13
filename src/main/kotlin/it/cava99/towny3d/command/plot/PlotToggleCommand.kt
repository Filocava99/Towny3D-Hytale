package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandValidation
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /plot toggle <flag> <value?> - Toggle plot flags
 */
class PlotToggleCommand : AbstractWorldCommand {

    private val flagArg = this.withOptionalArg("flag", "Flag to toggle", ArgTypes.STRING)
    private val valueArg = this.withOptionalArg("value", "on/off (optional)", ArgTypes.STRING)

    constructor() : super("toggle", "Toggle plot flags") {
        requirePermission("towny3d.plot.toggle")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val flag = if (flagArg.provided(context)) flagArg.get(context) as? String else null
        val value = if (valueArg.provided(context)) valueArg.get(context) as? String else null

        if (flag.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /plot toggle <flag> [on/off]"))
            context.sendMessage(MessageFormatter.info("Implemented flags: fire, explosion"))
            context.sendMessage(MessageFormatter.info("Coming soon: friendly_mobs_spawn, hostile_mobs_spawn"))
            return
        }

        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerUuid = playerContext.uuid
        val playerPosition = playerContext.position

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

                val blockX = playerPosition.x.toInt()
                val blockZ = playerPosition.z.toInt()
                val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(blockX, blockZ)

                val regions = plugin.regionService.getRegionsAt(chunkPos)
                if (regions.isEmpty()) {
                    context.sendMessage(MessageFormatter.error("This land is not claimed"))
                    return@launch
                }

                val region = regions.first()

                // Check permissions - owner or mayor/assistant of town
                val isOwner = region.ownerId == resident.id
                val isLeader = town.hasLeadership(resident.id)

                if (!isOwner && !isLeader) {
                    context.sendMessage(MessageFormatter.error("Only the plot owner or town leadership can toggle flags"))
                    return@launch
                }

                // Parse the value
                val newValue = when (value?.lowercase()) {
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
                    "fire" -> {
                        val newFireState = newValue ?: !region.toggles.fire
                        plugin.regionService.setToggleFlags(region.id, fire = newFireState)
                        context.sendMessage(
                            MessageFormatter.success("Fire ${if (newFireState) "enabled" else "disabled"} for this plot")
                        )
                    }
                    "explosion", "explosions" -> {
                        val newExplosionState = newValue ?: !region.toggles.explosion
                        plugin.regionService.setToggleFlags(region.id, explosion = newExplosionState)
                        context.sendMessage(
                            MessageFormatter.success("Explosions ${if (newExplosionState) "enabled" else "disabled"} for this plot")
                        )
                    }
                    "friendly_mobs_spawn", "fms" -> {
                        // TODO(2026-02-22): Implement friendly mobs spawn flag
                        context.sendMessage(MessageFormatter.error("Friendly mob spawning flag not yet implemented"))
                    }
                    "hostile_mobs_spawn", "hms" -> {
                        // TODO(2026-02-22): Implement hostile mobs spawn flag
                        context.sendMessage(MessageFormatter.error("Hostile mob spawning flag not yet implemented"))
                    }
                    else -> {
                        context.sendMessage(MessageFormatter.error("Unknown flag. Available: fire, explosion, friendly_mobs_spawn, hostile_mobs_spawn"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to toggle flag", e)
                context.sendMessage(MessageFormatter.error("Failed to toggle flag: ${e.message}"))
            }
        }
    }
}
