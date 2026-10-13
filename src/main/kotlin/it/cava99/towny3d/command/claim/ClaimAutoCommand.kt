package it.cava99.towny3d.command.claim

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
 * /claim auto [on|off] - Toggle auto-claiming
 */
class ClaimAutoCommand : AbstractWorldCommand {

    private val stateArg = this.withOptionalArg("state", "on|off", ArgTypes.STRING)

    constructor() : super("auto", "Toggle auto-claiming") {
        requirePermission("towny3d.claim.auto")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val state = if (stateArg.provided(context)) {
            (stateArg.get(context) as? String)?.lowercase()
        } else {
            null
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

        val playerUuid = CommandUtils.getPlayerUuid(playerRef, store)
        if (playerUuid == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return
        }

        plugin.pluginScope.launch {
            try {
                // Get resident (with fallback to load/create if not in cache)
                val resident = CommandUtils.getOrCreateResident(plugin, playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Failed to load resident data. Please try again."))
                    return@launch
                }

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to use auto-claim"))
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
                    context.sendMessage(MessageFormatter.error("Only the mayor can use auto-claim"))
                    return@launch
                }

                when (state) {
                    "on", "true", "yes" -> {
                        context.sendMessage(MessageFormatter.info("Auto-claim will automatically claim chunks as you walk."))
                        context.sendMessage(MessageFormatter.info("Use: /claim auto [radius]"))
                        context.sendMessage(MessageFormatter.error("Note: Auto-claim is not yet functional - use /claim land instead"))
                    }
                    "off", "false", "no" -> {
                        context.sendMessage(MessageFormatter.info("Auto-claim disabled"))
                    }
                    null -> {
                        context.sendMessage(MessageFormatter.info("Auto-claim status: Not implemented"))
                        context.sendMessage(MessageFormatter.info("Use /claim land to claim the current chunk"))
                    }
                    else -> {
                        context.sendMessage(MessageFormatter.error("Usage: /claim auto [on|off]"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to toggle auto-claim: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to toggle auto-claim: ${e.message}"))
            }
        }
    }
}
