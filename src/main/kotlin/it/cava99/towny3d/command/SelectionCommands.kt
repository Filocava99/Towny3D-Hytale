package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /p1 - Set primary selection corner (like WorldEdit's pos1)
 */
class Pos1Command : AbstractWorldCommand {
    constructor() : super("p1", "Set primary selection corner")

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
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
        val playerPosition = CommandUtils.getPlayerPosition(playerRef, store)

        if (playerUuid == null || playerPosition == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val config = plugin.getConfig()
                val location = BlockLocation(
                    config.defaultWorldName,
                    playerPosition.x.toInt(),
                    playerPosition.y.toInt(),
                    playerPosition.z.toInt()
                )

                plugin.selectionManager.setPrimary(playerUuid, location)

                val (primary, secondary) = plugin.selectionManager.getSelection(playerUuid)

                if (secondary != null) {
                    val volume = plugin.selectionManager.getSelectedVolume(playerUuid)
                    if (volume != null) {
                        val sizeX = volume.maxX - volume.minX + 1
                        val sizeY = volume.maxY - volume.minY + 1
                        val sizeZ = volume.maxZ - volume.minZ + 1
                        context.sendMessage(
                            MessageFormatter.success("First position set to ")
                                .insert(Message.raw("(${location.x}, ${location.y}, ${location.z})").color("#FFAA00"))
                        )
                        context.sendMessage(
                            MessageFormatter.info("Selection size: ")
                                .insert(Message.raw("${sizeX}x${sizeY}x${sizeZ}").color("#55FFFF").bold(true))
                        )
                    } else {
                        context.sendMessage(
                            MessageFormatter.success("First position set to ")
                                .insert(Message.raw("(${location.x}, ${location.y}, ${location.z})").color("#FFAA00"))
                        )
                    }
                } else {
                    context.sendMessage(
                        MessageFormatter.success("First position set to ")
                            .insert(Message.raw("(${location.x}, ${location.y}, ${location.z})").color("#FFAA00"))
                    )
                    context.sendMessage(MessageFormatter.info("Use /p2 to set the second position"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to set position 1: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to set position 1: ${e.message}"))
            }
        }
    }
}

/**
 * /p2 - Set secondary selection corner (like WorldEdit's pos2)
 */
class Pos2Command : AbstractWorldCommand {
    constructor() : super("p2", "Set secondary selection corner")

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
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
        val playerPosition = CommandUtils.getPlayerPosition(playerRef, store)

        if (playerUuid == null || playerPosition == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return
        }

        plugin.pluginScope.launch {
            try {
                val config = plugin.getConfig()
                val location = BlockLocation(
                    config.defaultWorldName,
                    playerPosition.x.toInt(),
                    playerPosition.y.toInt(),
                    playerPosition.z.toInt()
                )

                plugin.selectionManager.setSecondary(playerUuid, location)

                // Send success message first
                context.sendMessage(
                    MessageFormatter.success("Second position set to ")
                        .insert(Message.raw("(${location.x}, ${location.y}, ${location.z})").color("#FFAA00"))
                )

                val volume = plugin.selectionManager.getSelectedVolume(playerUuid)
                if (volume != null) {
                    val sizeX = volume.maxX - volume.minX + 1
                    val sizeY = volume.maxY - volume.minY + 1
                    val sizeZ = volume.maxZ - volume.minZ + 1

                    // Calculate cost safely
                    val estimatedCost = try {
                        plugin.selectionManager.calculateClaimCost(playerUuid, plugin.getConfig().layerCost)
                    } catch (e: Exception) {
                        TownyLog.warn("Failed to calculate claim cost: ${e.message}")
                        0.0
                    }

                    context.sendMessage(
                        MessageFormatter.info("Selection: ")
                            .insert(Message.raw("(${volume.minX}, ${volume.minY}, ${volume.minZ})").color("#AAFFFF"))
                            .insert(Message.raw(" to ").color("#AAAAAA"))
                            .insert(Message.raw("(${volume.maxX}, ${volume.maxY}, ${volume.maxZ})").color("#AAFFFF"))
                    )
                    context.sendMessage(
                        MessageFormatter.info("Size: ")
                            .insert(Message.raw("${sizeX}x${sizeY}x${sizeZ}").color("#55FFFF").bold(true))
                    )

                    // Format cost safely
                    try {
                        val costStr = plugin.economyService.formatAmount(estimatedCost)
                        context.sendMessage(
                            MessageFormatter.info("Estimated claim cost: ")
                                .insert(Message.raw(costStr).color("#FFAA00").bold(true))
                        )
                    } catch (e: Exception) {
                        TownyLog.warn("Failed to format cost: ${e.message}")
                        context.sendMessage(
                            MessageFormatter.info("Estimated claim cost: ")
                                .insert(Message.raw("$estimatedCost").color("#FFAA00").bold(true))
                        )
                    }

                    context.sendMessage(MessageFormatter.info("Use /claim land to claim this region"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to set position 2: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to set position 2: ${e.message}"))
            }
        }
    }
}
