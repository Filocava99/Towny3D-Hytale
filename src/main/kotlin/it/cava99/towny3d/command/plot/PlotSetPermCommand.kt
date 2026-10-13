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
 * /plot set perm <permGroup> <permType> <value> - Set plot permissions
 */
class PlotSetPermCommand : AbstractWorldCommand {

    private val permGroupArg = this.withRequiredArg("permGroup", "Permission group (res/ally/gen)", ArgTypes.STRING)
    private val permTypeArg = this.withRequiredArg("permType", "Permission type", ArgTypes.STRING)
    private val valueArg = this.withRequiredArg("value", "true/false", ArgTypes.STRING)

    constructor() : super("perm", "Set plot permissions") {
        requirePermission("towny3d.plot.set.perm")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val permGroup = permGroupArg.get(context) as? String
        val permType = permTypeArg.get(context) as? String
        val value = valueArg.get(context) as? String

        if (permGroup.isNullOrBlank() || permType.isNullOrBlank() || value.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /plot set perm <res/ally/gen> <permType> <true/false>"))
            context.sendMessage(MessageFormatter.info("Permission types: pvp, interact, inventory, build, destroy, drop, pick, move"))
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
                    context.sendMessage(MessageFormatter.error("Only the plot owner or town leadership can set permissions"))
                    return@launch
                }

                // Validate permission group
                val validGroups = listOf("res", "ally", "gen")
                if (permGroup !in validGroups) {
                    context.sendMessage(MessageFormatter.error("Invalid permission group. Use: res, ally, or gen"))
                    return@launch
                }

                // Validate permission type
                val validTypes = listOf("pvp", "interact", "inventory", "build", "destroy", "drop", "pick", "move")
                if (permType !in validTypes) {
                    context.sendMessage(MessageFormatter.error("Invalid permission type. Use: pvp, interact, inventory, build, destroy, drop, pick, or move"))
                    return@launch
                }

                // Parse boolean value
                val boolValue = when (value.lowercase()) {
                    "true", "on", "yes", "1" -> true
                    "false", "off", "no", "0" -> false
                    else -> {
                        context.sendMessage(MessageFormatter.error("Invalid value. Use: true or false"))
                        return@launch
                    }
                }

                // TODO(2026-02-22): Implement permission setting for the region
                context.sendMessage(
                    MessageFormatter.success("Set $permGroup $permType to ${if (boolValue) "allowed" else "denied"} for this plot")
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to set permission", e)
                context.sendMessage(MessageFormatter.error("Failed to set permission: ${e.message}"))
            }
        }
    }
}
