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
 * /town online - Show online town residents
 */
class TownOnlineCommand : AbstractWorldCommand {
    constructor() : super("online", "Show online residents") {
        requirePermission("towny3d.town.online")
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

                // Get all residents and check who's online
                val onlineResidents = mutableListOf<String>()
                val offlineResidents = mutableListOf<String>()

                // Get all online player UUIDs
                val onlinePlayerUuids = try {
                    val universe = com.hypixel.hytale.server.core.universe.Universe.get()
                    universe.getPlayers().map { it.uuid }.toSet()
                } catch (e: Exception) {
                    TownyLog.warn("Failed to get online players: ${e.message}")
                    emptySet()
                }

                for (residentId in town.residentIds) {
                    val res = plugin.residentService.getResident(residentId)
                    if (res != null) {
                        if (res.id.value in onlinePlayerUuids) {
                            onlineResidents.add(res.name)
                        } else {
                            offlineResidents.add(res.name)
                        }
                    }
                }

                context.sendMessage(Message.raw(MessageFormatter.formatHeader("Online Residents (${onlineResidents.size}/${town.residentIds.size})")))

                if (onlineResidents.isNotEmpty()) {
                    for (name in onlineResidents) {
                        context.sendMessage(Message.raw("- ").insert(Message.raw(name).color("#AAFFAA")))
                    }
                } else {
                    context.sendMessage(MessageFormatter.info("No residents currently online"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to get online residents: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get online residents: ${e.message}"))
            }
        }
    }
}
