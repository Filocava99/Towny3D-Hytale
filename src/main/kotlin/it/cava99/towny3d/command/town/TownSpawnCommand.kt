package it.cava99.towny3d.command.town

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
 * /town spawn - Teleport to town spawn
 */
class TownSpawnCommand : AbstractWorldCommand {
    constructor() : super("spawn", "Teleport to town spawn") {
        requirePermission("towny3d.town.spawn")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can teleport to town spawn"))
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

                val homeLocation = town.homeLocation
                if (homeLocation == null) {
                    context.sendMessage(MessageFormatter.error("Town spawn has not been set"))
                    return@launch
                }

                // Schedule teleport on WorldThread for Store access
                world.execute {
                    try {
                        // Get TransformComponent
                        val transformComponent = store.getComponent(
                            playerRef,
                            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType()
                        ) as? com.hypixel.hytale.server.core.modules.entity.component.TransformComponent

                        if (transformComponent == null) {
                            context.sendMessage(MessageFormatter.error("Failed to get player transform"))
                            return@execute
                        }

                        // Create position vector
                        val position = org.joml.Vector3d(
                            homeLocation.x.toDouble(),
                            homeLocation.y.toDouble(),
                            homeLocation.z.toDouble()
                        )

                        // Create Teleport component with proper rotation
                        val teleport = com.hypixel.hytale.server.core.modules.entity.teleport.Teleport(
                            position,
                            com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f)  // No rotation change
                        )

                        // Add the Teleport component to trigger teleportation
                        store.addComponent(playerRef, com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), teleport)
                        context.sendMessage(MessageFormatter.success("Teleported to ${town.name} spawn"))
                    } catch (e: Exception) {
                        TownyLog.error("Failed to teleport: ${e.message}", e)
                        context.sendMessage(MessageFormatter.error("Failed to teleport: ${e.message}"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to teleport to spawn: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to teleport: ${e.message}"))
            }
        }
    }
}
