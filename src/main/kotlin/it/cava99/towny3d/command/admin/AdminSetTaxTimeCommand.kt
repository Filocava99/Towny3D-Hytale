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
import javax.annotation.Nonnull

/**
 * /ta tax settaxtime <HH:mm> - Change tax collection time
 */
class AdminSetTaxTimeCommand : AbstractWorldCommand {
    private val timeArg = this.withRequiredArg("time", "Time in HH:mm format", ArgTypes.STRING)

    constructor() : super("settaxtime", "Change tax collection time") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val timeStr = timeArg.get(context) as? String

        if (timeStr.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /ta tax settaxtime <HH:mm>"))
            context.sendMessage(MessageFormatter.info("Example: /ta tax settaxtime 18:00"))
            return
        }

        // Validate time format
        if (!timeStr.matches(Regex("^([01]\\d|2[0-3]):([0-5]\\d)$"))) {
            context.sendMessage(MessageFormatter.error("Invalid time format. Use HH:mm (24-hour format)"))
            context.sendMessage(MessageFormatter.info("Example: 18:00 for 6 PM, 00:00 for midnight"))
            return
        }

        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                // Get current config and update with new time
                val currentConfig = plugin.getConfig()
                val newConfig = currentConfig.copy(
                    tax = currentConfig.tax.copy(
                        upkeepTimeOfDay = timeStr
                    )
                )

                plugin.setConfig(newConfig)

                context.sendMessage(
                    MessageFormatter.success("Tax collection time updated to $timeStr")
                )
                context.sendMessage(
                    MessageFormatter.info("Taxes will now be collected daily at $timeStr")
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to set tax time: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to set tax time: ${e.message}"))
            }
        }
    }
}
