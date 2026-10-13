package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import java.time.Instant
import javax.annotation.Nonnull

/**
 * /ta tax ruinate <town|nation> <name> - Force entity into ruined state
 */
class AdminRuinateCommand : AbstractWorldCommand {
    private val typeArg = this.withRequiredArg("type", "Entity type (town/nation)", ArgTypes.STRING)
    private val nameArg = this.withRequiredArg("name", "Entity name", ArgTypes.STRING)

    constructor() : super("ruinate", "Force entity into ruined state") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val type = typeArg.get(context) as? String
        val name = nameArg.get(context) as? String

        if (type.isNullOrBlank() || name.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /ta tax ruinate <town|nation> <name>"))
            return
        }

        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                when (type.lowercase()) {
                    "town" -> {
                        val town = plugin.townService.getTownByName(name)
                        if (town == null) {
                            context.sendMessage(MessageFormatter.error("Town '$name' not found"))
                            return@launch
                        }

                        if (town.isRuined) {
                            context.sendMessage(MessageFormatter.error("Town '$name' is already ruined"))
                            return@launch
                        }

                        // Mark as ruined
                        plugin.townService.setRuined(town.id, true, Instant.now())

                        context.sendMessage(
                            MessageFormatter.success("Town '$name' has been marked as ruined")
                        )
                        context.sendMessage(
                            MessageFormatter.info("The town will be deleted in ${plugin.getConfig().ruinedDeletionDays} days if not reclaimed")
                        )
                    }

                    "nation" -> {
                        val nation = plugin.nationService.getNationByName(name)
                        if (nation == null) {
                            context.sendMessage(MessageFormatter.error("Nation '$name' not found"))
                            return@launch
                        }

                        if (nation.isRuined) {
                            context.sendMessage(MessageFormatter.error("Nation '$name' is already ruined"))
                            return@launch
                        }

                        // Mark as ruined
                        plugin.nationService.setRuined(nation.id, true, Instant.now())

                        context.sendMessage(
                            MessageFormatter.success("Nation '$name' has been marked as ruined")
                        )
                        context.sendMessage(
                            MessageFormatter.info("The nation will be deleted in ${plugin.getConfig().ruinedDeletionDays} days if not reclaimed")
                        )
                    }

                    else -> {
                        context.sendMessage(MessageFormatter.error("Invalid type. Use 'town' or 'nation'"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to ruinate entity: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to ruinate entity: ${e.message}"))
            }
        }
    }
}
