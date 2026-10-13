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
import it.cava99.towny3d.gui.TownInfoGui
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /town info [town] - View town information
 * Default: show your own town info
 */
class TownInfoCommand : AbstractWorldCommand {

    private val townArg = this.withOptionalArg("town", "Town name", ArgTypes.STRING)

    constructor() : super("info", "View town information") {
        requirePermission("towny3d.town.info")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val townName = if (townArg.provided(context)) {
            townArg.get(context) as? String
        } else {
            null
        }

        val plugin = Towny3D.instance

        // Get player Ref and UUID BEFORE entering coroutine (must be on main thread)
        val playerRef = if (context.isPlayer()) context.senderAsPlayerRef() else null
        val playerUuid = if (townName.isNullOrBlank() && playerRef != null && playerRef.isValid()) {
            CommandUtils.getPlayerUuid(playerRef, store)
        } else null

        // Get PlayerRef component BEFORE entering coroutine (must be on WorldThread)
        val actualPlayerRef = if (playerRef != null && playerRef.isValid()) {
            store.getComponent(playerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType())
        } else null

        plugin.pluginScope.launch {
            try {
                val town = when {
                    !townName.isNullOrBlank() -> {
                        // Show specific town info
                        plugin.townService.getTownByName(townName)
                    }
                    playerUuid != null -> {
                        // Show player's town info
                        val resident = plugin.residentService.getResidentByUuid(playerUuid)
                        resident?.townId?.let { plugin.townService.getTown(it) }
                    }
                    else -> null
                }

                if (town == null) {
                    val errorMsg = if (!townName.isNullOrBlank()) {
                        "Town '$townName' not found"
                    } else {
                        "You are not a member of any town"
                    }
                    context.sendMessage(MessageFormatter.error(errorMsg))
                    return@launch
                }

                // Fetch all data first using synchronous cache methods
                val mayorName = plugin.residentCache.getSync(town.mayorId)?.name ?: "Unknown"
                val nationName = if (town.hasNation && town.nationId != null) {
                    plugin.nationCache.getSync(town.nationId!!)?.name
                } else null

                // Send individual messages for better formatting and to avoid thread issues
                context.sendMessage(Message.raw(MessageFormatter.formatHeader("${town.name}")))
                context.sendMessage(Message.raw("Mayor: ").insert(Message.raw(mayorName).color("#AAFFAA")))
                context.sendMessage(Message.raw("Residents: ").insert(Message.raw("${town.residentIds.size}").color("#AAAAFF")))
                context.sendMessage(Message.raw("Balance: ").insert(Message.raw(plugin.economyService.formatAmount(town.balance)).color("#FFAA00").bold(true)))
                context.sendMessage(Message.raw("Layers: ").insert(Message.raw("${town.layers} / ${town.maxLayers}").color("#FFAAAA")))

                if (nationName != null) {
                    context.sendMessage(Message.raw("Nation: ").insert(Message.raw(nationName).color("#FFAAFF")))
                }

                context.sendMessage(Message.raw("Open: ").insert(Message.raw(if (town.isOpen) "Yes" else "No").color("#AAAAFF")))

                if (town.board.isNotBlank()) {
                    context.sendMessage(Message.raw(""))
                    context.sendMessage(Message.raw("Board: ").insert(Message.raw(town.board).color("#FFFFAA")))
                }

                // Open the GUI for players
                if (actualPlayerRef != null && playerRef != null) {
                    val residentId = playerUuid?.let { uuid -> ResidentId.fromPlayerUuid(uuid) }
                    try {
                        val sender = context.sender()
                        if (sender is com.hypixel.hytale.server.core.entity.entities.Player) {
                            sender.getPageManager().openCustomPage(
                                playerRef,
                                store,
                                TownInfoGui(actualPlayerRef, town, residentId)
                            )
                        }
                    } catch (e: Exception) {
                        TownyLog.error("Failed to open TownInfoGui: ${e.message}", e)
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to get town info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get town info: ${e.message}"))
            }
        }
    }
}
