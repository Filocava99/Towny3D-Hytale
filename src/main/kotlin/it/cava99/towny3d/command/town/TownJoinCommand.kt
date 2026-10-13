package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandValidation
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /town join [town] - Join a town
 */
class TownJoinCommand : AbstractWorldCommand {

    private val townArg = this.withRequiredArg("town", "Town name", ArgTypes.STRING)

    constructor() : super("join", "Join a town") {
        requirePermission("towny3d.town.join")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val townName = townArg.get(context) as? String ?: run {
            context.sendMessage(MessageFormatter.error("Usage: /town join <town>"))
            return
        }

        val plugin = Towny3D.instance

        val playerContext = CommandValidation.validatePlayerBasic(context, store) ?: return
        val playerUuid = playerContext.uuid

        plugin.pluginScope.launch {
            try {
                // Get resident
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                // Check if player already has a town
                if (resident.townId != null) {
                    context.sendMessage(MessageFormatter.error("You are already a member of a town. Leave it first."))
                    return@launch
                }

                // Find town by name
                val town = plugin.townService.getTownByName(townName!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town '$townName' not found"))
                    return@launch
                }

                // Check if town is open
                if (!town.isOpen) {
                    context.sendMessage(MessageFormatter.error("Town '$townName' is not open. You need an invite."))
                    return@launch
                }

                // Add player to town
                plugin.townService.addResident(town.id, resident.id)
                plugin.residentService.setTown(resident.id, town.id)

                context.sendMessage(
                    MessageFormatter.success("You have joined town '")
                        .insert(MessageFormatter.formatTown(town.name, town.isPublic))
                        .insert(Message.raw("'!"))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to join town: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to join town: ${e.message}"))
            }
        }
    }
}
