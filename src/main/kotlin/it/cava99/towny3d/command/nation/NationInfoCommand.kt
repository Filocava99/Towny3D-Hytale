package it.cava99.towny3d.command.nation

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
 * /nation info [nation] - View nation information
 * Default: show your own nation info
 */
class NationInfoCommand : AbstractWorldCommand {

    private val nationArg = this.withOptionalArg("nation", "Nation name", ArgTypes.STRING)

    constructor() : super("info", "View nation information") {
        requirePermission("towny3d.nation.info")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val nationName = if (nationArg.provided(context)) {
            nationArg.get(context) as? String
        } else {
            null
        }

        val plugin = Towny3D.instance

        // Get player UUID BEFORE entering coroutine (must be on main thread)
        val playerUuid = if (nationName.isNullOrBlank() && context.isPlayer()) {
            val playerRef = context.senderAsPlayerRef()
            if (playerRef != null && playerRef.isValid()) {
                CommandUtils.getPlayerUuid(playerRef, store)
            } else null
        } else null

        plugin.pluginScope.launch {
            try {
                val nation = when {
                    !nationName.isNullOrBlank() -> {
                        // Show specific nation info
                        plugin.nationService.getNationByName(nationName)
                    }
                    playerUuid != null -> {
                        // Show player's nation info
                        val resident = plugin.residentService.getResidentByUuid(playerUuid)
                        if (resident?.townId != null) {
                            val town = plugin.townService.getTown(resident.townId!!)
                            town?.nationId?.let { plugin.nationService.getNation(it) }
                        } else null
                    }
                    else -> null
                }

                if (nation == null) {
                    val errorMsg = if (!nationName.isNullOrBlank()) {
                        "Nation '$nationName' not found"
                    } else {
                        "You are not a member of any nation"
                    }
                    context.sendMessage(MessageFormatter.error(errorMsg))
                    return@launch
                }

                // Fetch all data first to avoid suspend function issues
                // King is the mayor of the capital town
                val capitalTown = nation.capitalId?.let { plugin.townService.getTown(it) }
                val capitalName = capitalTown?.name ?: "None"
                val kingName = capitalTown?.let { town ->
                    plugin.residentService.getResident(town.mayorId)?.name
                } ?: "Unknown"
                val townCount = nation.townIds.size
                val isPublic = if (nation.isPublic) "Yes" else "No"

                // Send individual messages for better formatting and to avoid thread issues
                context.sendMessage(Message.raw(MessageFormatter.formatHeader("${nation.name}")))
                context.sendMessage(Message.raw("King: ").insert(Message.raw(kingName).color("#AAFFAA")))
                context.sendMessage(Message.raw("Towns: ").insert(Message.raw("$townCount").color("#AAAAFF")))
                context.sendMessage(Message.raw("Capital: ").insert(Message.raw(capitalName).color("#FFAAFF")))
                context.sendMessage(Message.raw("Public: ").insert(Message.raw(isPublic).color("#AAAAFF")))

            } catch (e: Exception) {
                TownyLog.error("Failed to get nation info", e)
                context.sendMessage(MessageFormatter.error("Failed to get nation info: ${e.message}"))
            }
        }
    }
}
