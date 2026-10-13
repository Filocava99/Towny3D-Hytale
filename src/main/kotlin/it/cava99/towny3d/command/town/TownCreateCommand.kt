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
 * /town create [name] - Create a new town
 */
class TownCreateCommand : AbstractWorldCommand {

    private val nameArg = this.withRequiredArg("name", "Town name", ArgTypes.STRING)

    constructor() : super("create", "Create a new town") {
        requirePermission("towny3d.town.create")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val name = nameArg.get(context) as? String

        // Validate name
        if (name.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Town name cannot be blank"))
            return
        }

        val plugin = Towny3D.instance
        val config = plugin.getConfig()

        // Check name length
        if (name.length > config.maxTownNameLength) {
            context.sendMessage(MessageFormatter.error("Town name too long (max ${config.maxTownNameLength} characters)"))
            return
        }

        // Get player info
        val playerContext = CommandValidation.validatePlayer(context, store) ?: return
        val playerUuid = playerContext.uuid
        val playerPosition = playerContext.position
        val playerName = getPlayerName(playerContext.ref, store)

        // Launch coroutine to handle async operations
        plugin.pluginScope.launch {
            try {
                // Load or create resident
                val resident = plugin.residentService.loadOrCreate(playerUuid, playerName ?: "Unknown")

                // Check if player already has a town
                if (resident.townId != null) {
                    context.sendMessage(MessageFormatter.error("You are already a member of a town"))
                    return@launch
                }

                // Check if town name already exists
                val existingTown = plugin.townService.getTownByName(name)
                if (existingTown != null) {
                    context.sendMessage(MessageFormatter.error("Town name '$name' is already taken"))
                    return@launch
                }

                // Get current location as BlockLocation
                val blockX = playerPosition.x.toInt()
                val blockY = playerPosition.y.toInt()
                val blockZ = playerPosition.z.toInt()
                val worldName = config.defaultWorldName
                val blockLocation = it.cava99.towny3d.model.BlockLocation(worldName, blockX, blockY, blockZ)
                val chunkPos = it.cava99.towny3d.model.ChunkPos(blockX shr 4, blockZ shr 4)

                // Check if location is already claimed
                if (plugin.regionService.isClaimed(blockLocation)) {
                    context.sendMessage(MessageFormatter.error("You cannot create a town here - this area is already claimed"))
                    return@launch
                }

                // Check if player has enough money
                val townCreationCost = config.calculateTownCreationCost()
                val residentId = it.cava99.towny3d.model.ResidentId.fromPlayerUuid(playerUuid)

                if (config.enableEconomy) {
                    val hasEnough = plugin.economyService.hasBalance(residentId, townCreationCost)
                    if (!hasEnough) {
                        context.sendMessage(
                            MessageFormatter.error("You need ")
                                .insert(Message.raw(plugin.economyService.formatAmount(townCreationCost)).color("#FFAA00").bold(true))
                                .insert(Message.raw(" to create a town (you have "))
                                .insert(Message.raw(plugin.economyService.formatAmount(plugin.economyService.getBalance(residentId))).color("#FFAA00").bold(true))
                                .insert(Message.raw(")"))
                        )
                        return@launch
                    }

                    // Withdraw money
                    val withdrawn = plugin.economyService.withdraw(residentId, townCreationCost)
                    if (!withdrawn) {
                        context.sendMessage(MessageFormatter.error("Failed to withdraw money from your account"))
                        return@launch
                    }
                }

                // Calculate initial layers based on config
                val initialLayers = config.homeRegionHeight
                val initialBalance = config.defaultTownBalance

                // Create the town
                val town = plugin.townService.createTown(
                    name = name,
                    mayorId = residentId,
                    homeLocation = blockLocation,
                    initialBalance = initialBalance,
                    initialLayers = initialLayers
                )

                // Create home region
                plugin.regionService.createHomeRegion(
                    townId = town.id,
                    chunkPos = chunkPos,
                    y1 = -64,
                    y2 = 320
                )

                // Update resident's town
                plugin.residentService.setTown(residentId, town.id)

                // Success message
                context.sendMessage(MessageFormatter.success("Town '$name' created successfully!"))
                context.sendMessage(MessageFormatter.info("Use /town for town commands"))

            } catch (e: Exception) {
                TownyLog.error("Failed to create town: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to create town: ${e.message}"))
            }
        }
    }
}

/**
 * Helper extension to get player name from Ref
 * Note: This may not be available in all contexts, so we return null
 */
private fun getPlayerName(ref: com.hypixel.hytale.component.Ref<EntityStore>, store: Store<EntityStore>): String? {
    // Try to get the name from Player component
    // In many cases, we can't get the player name directly from the component
    // So we return null and let the caller handle it
    return null
}
