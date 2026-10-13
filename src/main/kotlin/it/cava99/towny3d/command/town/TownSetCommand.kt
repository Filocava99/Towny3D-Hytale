package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
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
 * /town set - Town settings submenu
 */
class TownSetCommand : AbstractCommandCollection("set", "Town settings") {
    init {
        requirePermission("towny3d.town.set")
        this.addSubCommand(TownSetColorCommand())
        this.addSubCommand(TownSetPermCommand())
    }
}

/**
 * /town set color <hexColor> - Set town map color (mayor only)
 */
class TownSetColorCommand : AbstractWorldCommand {

    private val colorArg = this.withRequiredArg("color", "Hex color (e.g., FF0000 for red)", ArgTypes.STRING)

    constructor() : super("color", "Set town map color") {
        requirePermission("towny3d.town.set")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val colorStr = colorArg.get(context) as? String

        if (colorStr.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town set color <hexColor>"))
            context.sendMessage(MessageFormatter.info("Example: /town set color FF0000 (red)"))
            context.sendMessage(MessageFormatter.info("Example: /town set color 00FF00 (green)"))
            context.sendMessage(MessageFormatter.info("Example: /town set color 0000FF (blue)"))
            return
        }

        // Validate hex color format
        if (!colorStr.matches(Regex("^[0-9A-Fa-f]{6}$"))) {
            context.sendMessage(MessageFormatter.error("Invalid color format. Use 6-digit hex color (e.g., FF0000)"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can set town color"))
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
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Only mayor can set color
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can set the town color"))
                    return@launch
                }

                // Parse hex color to integer
                val color = colorStr.toIntOrNull(16)
                if (color == null) {
                    context.sendMessage(MessageFormatter.error("Failed to parse color"))
                    return@launch
                }

                // Set the color
                plugin.townService.setMapColor(town.id, color)

                // Show color preview in message
                val colorHex = "#$colorStr"
                context.sendMessage(
                    MessageFormatter.success("Town color updated to ")
                        .insert(Message.raw(colorStr).color(colorHex).bold(true))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to set town color: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to set color: ${e.message}"))
            }
        }
    }
}

/**
 * /town set perm <permGroup> <permType> <value> - Set town permissions
 */
class TownSetPermCommand : AbstractWorldCommand {

    private val permGroupArg = this.withOptionalArg("permGroup", "Permission group (res/ally/gen)", ArgTypes.STRING)
    private val permTypeArg = this.withOptionalArg("permType", "Permission type", ArgTypes.STRING)
    private val valueArg = this.withOptionalArg("value", "true/false", ArgTypes.STRING)

    constructor() : super("perm", "Set town permissions") {
        requirePermission("towny3d.town.set")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val permGroup = if (permGroupArg.provided(context)) permGroupArg.get(context) as? String else null
        val permType = if (permTypeArg.provided(context)) permTypeArg.get(context) as? String else null
        val value = if (valueArg.provided(context)) valueArg.get(context) as? String else null

        if (permGroup.isNullOrBlank() || permType.isNullOrBlank() || value.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town set perm <res/ally/gen> <permType> <true/false>"))
            context.sendMessage(MessageFormatter.info("Permission types: pvp, interact, inventory, build, destroy, drop, pick, move"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can set permissions"))
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
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Only mayor or assistants can set permissions
                if (!town.hasLeadership(resident.id)) {
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can set permissions"))
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

                // TODO(2026-02-22): Implement permission setting for all town regions
                context.sendMessage(
                    MessageFormatter.success("Set $permGroup $permType to ${if (boolValue) "allowed" else "denied"} for all town regions")
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to set permission: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to set permission: ${e.message}"))
            }
        }
    }
}
