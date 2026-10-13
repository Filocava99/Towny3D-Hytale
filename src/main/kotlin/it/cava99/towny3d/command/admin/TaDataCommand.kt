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
 * /ta data - Data management commands
 */
class TaDataCommand : AbstractCommandCollection("data", "Data management commands") {
    init {
        requirePermission("towny3d.admin")
        this.addSubCommand(AdminReconcileCommand())
    }
}

/**
 * /ta reconcile - Fix town membership inconsistencies
 */
class AdminReconcileCommand : AbstractWorldCommand {
    constructor() : super("reconcile", "Fix town membership inconsistencies") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                context.sendMessage(MessageFormatter.info("Reconciling town membership data..."))

                val result = plugin.residentService.reconcileTownMembership()

                context.sendMessage(
                    MessageFormatter.success("Reconciliation complete!")
                )

                if (result.fixedResidents > 0) {
                    context.sendMessage(
                        MessageFormatter.info("Fixed ${result.fixedResidents} resident(s)")
                    )
                }

                if (result.fixedTowns > 0) {
                    context.sendMessage(
                        MessageFormatter.info("Fixed ${result.fixedTowns} town(s)")
                    )
                }

                if (result.errors.isNotEmpty()) {
                    context.sendMessage(
                        MessageFormatter.warning("Found ${result.errors.size} issue(s) that require manual attention:")
                    )
                    result.errors.take(5).forEach { error ->
                        context.sendMessage(MessageFormatter.error("  - $error"))
                    }
                    if (result.errors.size > 5) {
                        context.sendMessage(
                            MessageFormatter.info("  ... and ${result.errors.size - 5} more (check console)")
                        )
                    }
                }

                if (result.fixedResidents == 0 && result.fixedTowns == 0 && result.errors.isEmpty()) {
                    context.sendMessage(
                        MessageFormatter.success("No inconsistencies found!")
                    )
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to reconcile: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to reconcile: ${e.message}"))
            }
        }
    }
}
