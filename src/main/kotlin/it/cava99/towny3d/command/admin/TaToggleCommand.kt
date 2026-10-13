package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /ta toggle - Toggle admin settings
 */
class TaToggleCommand : AbstractCommandCollection("toggle", "Toggle admin settings") {
    init {
        requirePermission("towny3d.admin")
        this.addSubCommand(AdminToggleUpkeepCommand())
    }
}

/**
 * /ta toggle upkeep - Enable/disable upkeep system
 */
class AdminToggleUpkeepCommand : AbstractWorldCommand {
    constructor() : super("upkeep", "Enable/disable upkeep system") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                val config = plugin.getConfig()

                // Toggle both town and nation upkeep
                val newState = !config.enableTownUpkeep
                val newConfig = config.copy(
                    tax = config.tax.copy(
                        enableTownUpkeep = newState,
                        enableNationUpkeep = newState
                    )
                )

                plugin.setConfig(newConfig)

                val status = if (newState) "enabled" else "disabled"
                context.sendMessage(
                    MessageFormatter.success("Upkeep system $status")
                )

                if (newState) {
                    context.sendMessage(
                        MessageFormatter.info("Town upkeep: ${plugin.economyService.formatAmount(config.townUpkeep)}/day")
                    )
                    context.sendMessage(
                        MessageFormatter.info("Nation upkeep: ${plugin.economyService.formatAmount(config.nationUpkeep)}/day")
                    )
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to toggle upkeep: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to toggle upkeep: ${e.message}"))
            }
        }
    }
}
