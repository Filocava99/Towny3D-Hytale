package it.cava99.towny3d.command.claim

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /claim gcs - Get claim stick
 * Gives an item that allows selecting plot boundaries for 3D claiming
 */
class ClaimGcsCommand : AbstractWorldCommand {
    constructor() : super("gcs", "Get claim stick") {
        requirePermission("towny3d.claim.stick")
    }

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can get the claim stick"))
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
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to use the claim stick"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is mayor or assistant
                if (!town.hasLeadership(resident.id)) {
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can use the claim stick"))
                    return@launch
                }

                // The playerRef from context is actually the entity Ref, not PlayerRef component
                // We can use it directly to access the store
                val entityRef = playerRef

                // Check if valid
                if (!entityRef.isValid()) {
                    context.sendMessage(MessageFormatter.error("Failed to get player entity"))
                    return@launch
                }

                // Get player's world for thread-safe operations
                val playerWorld = try {
                    val entityStore = entityRef.getStore().getExternalData() as? com.hypixel.hytale.server.core.universe.world.storage.EntityStore
                    entityStore?.getWorld()
                } catch (e: Exception) {
                    TownyLog.error("Failed to get player world: ${e.message}", e)
                    null
                }

                if (playerWorld == null) {
                    context.sendMessage(MessageFormatter.error("Failed to get player world"))
                    return@launch
                }

                // Give claim stick item
                playerWorld.execute {
                    try {
                        // Get Player component directly from the store
                        val playerComponent = try {
                            entityRef.getStore().getComponent(
                                entityRef,
                                com.hypixel.hytale.server.core.entity.entities.Player.getComponentType()
                            ) as? com.hypixel.hytale.server.core.entity.entities.Player
                        } catch (e: Exception) {
                            TownyLog.error("Failed to get player component: ${e.message}")
                            null
                        }

                        if (playerComponent == null) {
                            context.sendMessage(MessageFormatter.error("Failed to get player component"))
                            return@execute
                        }

                        // Create a stick item stack (using Ingredient_Stick)
                        // Note: Custom display names via metadata don't work in Hytale
                        // The item will show as "Stick" but the event system checks by itemId
                        val itemStack = com.hypixel.hytale.server.core.inventory.ItemStack(
                            "Ingredient_Stick",
                            1,
                            null  // No metadata - display names come from translation keys, not metadata
                        )

                        // Add item to player's inventory
                        val container = playerComponent.getInventory().getCombinedHotbarFirst()
                        if (container == null) {
                            context.sendMessage(MessageFormatter.error("Failed to access inventory"))
                            return@execute
                        }
                        val transaction = container.addItemStack(itemStack)

                        val remainder = transaction.getRemainder()
                        if (remainder != null && !remainder.isEmpty()) {
                            context.sendMessage(MessageFormatter.error("Inventory is full!"))
                        } else {
                            context.sendMessage(MessageFormatter.success("Claim stick received!"))
                            context.sendMessage(MessageFormatter.info("Left click to set the first corner"))
                            context.sendMessage(MessageFormatter.info("Press F to set the second corner"))
                            context.sendMessage(MessageFormatter.info("Use /claim land to claim the selected region"))
                        }

                    } catch (e: Exception) {
                        TownyLog.error("Failed to give claim stick: ${e.message}", e)
                        context.sendMessage(MessageFormatter.error("Failed to give claim stick: ${e.message}"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to give claim stick: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to give claim stick: ${e.message}"))
            }
        }
    }
}
