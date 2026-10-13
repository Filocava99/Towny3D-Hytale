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
 * /ta tax reclaim <town|nation> <name> - Reclaim a ruined entity
 */
class AdminReclaimCommand : AbstractWorldCommand {
    private val typeArg = this.withRequiredArg("type", "Entity type (town/nation)", ArgTypes.STRING)
    private val nameArg = this.withRequiredArg("name", "Entity name", ArgTypes.STRING)

    constructor() : super("reclaim", "Reclaim a ruined entity") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val type = typeArg.get(context) as? String
        val name = nameArg.get(context) as? String

        if (type.isNullOrBlank() || name.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /ta tax reclaim <town|nation> <name>"))
            return
        }

        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                val config = plugin.getConfig()

                when (type.lowercase()) {
                    "town" -> {
                        val town = plugin.townService.getTownByName(name)
                        if (town == null) {
                            context.sendMessage(MessageFormatter.error("Town '$name' not found"))
                            return@launch
                        }

                        if (!town.isRuined) {
                            context.sendMessage(MessageFormatter.error("Town '$name' is not ruined"))
                            return@launch
                        }

                        // Calculate reclaim fee
                        val reclaimFee = config.townUpkeep * config.reclaimFeeMultiplier

                        context.sendMessage(
                            MessageFormatter.info("Reclaiming ruined town '$name'")
                        )
                        context.sendMessage(
                            MessageFormatter.info("Reclaim fee: ${plugin.economyService.formatAmount(reclaimFee)}")
                        )

                        // TODO(2026-02-22): Implement proper reclaim with TaxCollectionService
                        // val result = plugin.taxCollectionService.reclaimTown(town.id)

                        // For now, just mark as not ruined
                        plugin.townService.setRuined(town.id, false, null)

                        context.sendMessage(
                            MessageFormatter.success("Town '$name' has been reclaimed")
                        )
                    }

                    "nation" -> {
                        val nation = plugin.nationService.getNationByName(name)
                        if (nation == null) {
                            context.sendMessage(MessageFormatter.error("Nation '$name' not found"))
                            return@launch
                        }

                        if (!nation.isRuined) {
                            context.sendMessage(MessageFormatter.error("Nation '$name' is not ruined"))
                            return@launch
                        }

                        // Calculate reclaim fee
                        val reclaimFee = config.nationUpkeep * config.reclaimFeeMultiplier

                        context.sendMessage(
                            MessageFormatter.info("Reclaiming ruined nation '$name'")
                        )
                        context.sendMessage(
                            MessageFormatter.info("Reclaim fee: ${plugin.economyService.formatAmount(reclaimFee)}")
                        )

                        // TODO(2026-02-22): Implement proper reclaim with TaxCollectionService
                        // val result = plugin.taxCollectionService.reclaimNation(nation.id)

                        // For now, just mark as not ruined
                        plugin.nationService.setRuined(nation.id, false, null)

                        context.sendMessage(
                            MessageFormatter.success("Nation '$name' has been reclaimed")
                        )
                    }

                    else -> {
                        context.sendMessage(MessageFormatter.error("Invalid type. Use 'town' or 'nation'"))
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to reclaim entity: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to reclaim entity: ${e.message}"))
            }
        }
    }
}
