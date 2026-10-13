package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Duration
import javax.annotation.Nonnull

/**
 * /ta tax nexttax - Show when next tax collection occurs
 */
class AdminNextTaxCommand : AbstractWorldCommand {
    constructor() : super("nexttax", "Show when next collection occurs") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                val config = plugin.getConfig()
                val now = Instant.now()

                // Get last collection time from a random town as reference
                val towns = plugin.townService.getAllTowns()
                val lastCollection = if (towns.isNotEmpty()) {
                    towns.minOfOrNull { it.lastTaxCollection } ?: Instant.EPOCH
                } else {
                    Instant.EPOCH
                }

                val intervalHours = config.taxCollectionIntervalHours.toLong()
                val intervalMillis = intervalHours * 3600 * 1000
                val nextCollection = lastCollection.plusMillis(intervalMillis)
                val timeUntil = Duration.between(now, nextCollection)

                val hours = timeUntil.toHours()
                val minutes = timeUntil.toMinutes() % 60

                context.sendMessage(MessageFormatter.info("Tax collection scheduled for: ${config.upkeepTimeOfDay}"))
                context.sendMessage(
                    MessageFormatter.info("Next collection in: ${hours}h ${minutes}m")
                )
                context.sendMessage(
                    MessageFormatter.info("Collection interval: ${intervalHours} hours")
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to get next tax info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get next tax info: ${e.message}"))
            }
        }
    }
}
