package it.cava99.towny3d.command.nation

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
 * /nation create [name] - Create a new nation
 */
class NationCreateCommand : AbstractWorldCommand {

    private val nameArg = this.withRequiredArg("name", "Nation name", ArgTypes.STRING)

    constructor() : super("create", "Create a new nation") {
        requirePermission("towny3d.nation.create")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val name = nameArg.get(context) as? String

        // Validate name
        if (name.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Nation name cannot be blank"))
            return
        }

        val plugin = Towny3D.instance
        val config = plugin.getConfig()

        // Check if nations are enabled
        if (!config.enableNations) {
            context.sendMessage(MessageFormatter.error("Nations are disabled on this server"))
            return
        }

        // Check name length
        if (name.length > config.maxNationNameLength) {
            context.sendMessage(MessageFormatter.error("Nation name too long (max ${config.maxNationNameLength} characters)"))
            return
        }

        // Get player info
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

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a mayor of a town to create a nation"))
                    return@launch
                }

                // Get player's town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                // Check if player is the mayor
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only town mayors can create nations"))
                    return@launch
                }

                // Check if town is already in a nation
                if (town.hasNation) {
                    context.sendMessage(MessageFormatter.error("Your town is already in a nation"))
                    return@launch
                }

                // Check if nation name already exists
                val existingNation = plugin.nationService.getNationByName(name)
                if (existingNation != null) {
                    context.sendMessage(MessageFormatter.error("Nation name '$name' is already taken"))
                    return@launch
                }

                // Check if player has enough money
                val nationCreationCost = config.nationCreationCost
                val residentId = resident.id

                if (config.enableEconomy) {
                    val hasEnough = plugin.economyService.hasBalance(residentId, nationCreationCost)
                    if (!hasEnough) {
                        context.sendMessage(
                            MessageFormatter.error("You need ")
                                .insert(Message.raw(plugin.economyService.formatAmount(nationCreationCost)).color("#FFAA00").bold(true))
                                .insert(Message.raw(" to create a nation (you have "))
                                .insert(Message.raw(plugin.economyService.formatAmount(plugin.economyService.getBalance(residentId))).color("#FFAA00").bold(true))
                                .insert(Message.raw(")"))
                        )
                        return@launch
                    }

                    // Withdraw money
                    val withdrawn = plugin.economyService.withdraw(residentId, nationCreationCost)
                    if (!withdrawn) {
                        context.sendMessage(MessageFormatter.error("Failed to withdraw money from your account"))
                        return@launch
                    }
                }

                // Create the nation
                val nation = plugin.nationService.createNation(
                    name = name,
                    kingId = residentId,
                    capitalTownId = town.id
                )

                // Add town to nation
                plugin.nationService.addTown(nation.id, town.id)
                plugin.townService.setNation(town.id, nation.id)

                // Success message
                context.sendMessage(MessageFormatter.success("Nation '$name' created successfully!"))
                context.sendMessage(MessageFormatter.info("Your town is now the capital of the new nation"))

            } catch (e: Exception) {
                TownyLog.error("Failed to create nation", e)
                context.sendMessage(MessageFormatter.error("Failed to create nation: ${e.message}"))
            }
        }
    }
}
