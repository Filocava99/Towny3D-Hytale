package it.cava99.towny3d.command.admin

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
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
 * /ta stats - Show server statistics
 */
class TaStatsCommand : AbstractWorldCommand {

    constructor() : super("stats", "Show server statistics") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                val towns = plugin.townService.getAllTowns()
                val nations = plugin.nationService.getAllNations()
                val residents = plugin.residentService.getAllResidents()
                val regions = plugin.regionService.getAllRegions()

                val stats = buildString {
                    append("${MessageFormatter.formatHeader("Server Statistics")}\n\n")
                    append("Towns: ${towns.size}\n")
                    append("Nations: ${nations.size}\n")
                    append("Residents: ${residents.size}\n")
                    append("Regions: ${regions.size}\n")
                    append("\nTotal Residents in Towns: ${towns.sumOf { it.residentIds.size }}\n")
                    append("Total Town Balance: ${plugin.economyService.formatAmount(towns.sumOf { it.balance })}\n")
                }

                context.sendMessage(Message.raw(stats))

            } catch (e: Exception) {
                TownyLog.error("Failed to get stats: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get stats: ${e.message}"))
            }
        }
    }
}
