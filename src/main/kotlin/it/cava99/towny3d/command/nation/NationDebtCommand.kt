package it.cava99.towny3d.command.nation

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /nation debt [town] - View town debts to nation
 */
class NationDebtCommand : AbstractWorldCommand {

    private val townArg = this.withOptionalArg("town", "Town name", ArgTypes.STRING)

    constructor() : super("debt", "View town debts") {
        requirePermission("towny3d.nation.debt")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val townName = if (townArg.provided(context)) {
            townArg.get(context) as? String
        } else {
            null
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can view debts"))
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
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }

                if (!town.hasNation || town.nationId == null) {
                    context.sendMessage(MessageFormatter.error("Your town is not in a nation"))
                    return@launch
                }

                val nation = plugin.nationService.getNation(town.nationId!!)
                if (nation == null) {
                    context.sendMessage(MessageFormatter.error("Nation not found"))
                    return@launch
                }

                // If specific town requested
                if (!townName.isNullOrBlank()) {
                    val targetTown = plugin.townService.getTownByName(townName)
                    if (targetTown == null) {
                        context.sendMessage(MessageFormatter.error("Town '$townName' not found"))
                        return@launch
                    }

                    if (targetTown.nationId != nation.id) {
                        context.sendMessage(MessageFormatter.error("Town '$townName' is not in your nation"))
                        return@launch
                    }

                    val debt = targetTown.nationDebt
                    context.sendMessage(
                        Message.raw("'$townName' debt to nation: ")
                            .insert(Message.raw(plugin.economyService.formatAmount(debt)).color("#FF5555").bold(true))
                    )
                } else {
                    // Show all town debts
                    context.sendMessage(Message.raw(MessageFormatter.formatHeader("${nation.name} Town Debts")))

                    for (townId in nation.townIds) {
                        val memberTown = plugin.townService.getTown(townId)
                        if (memberTown != null) {
                            val debt = memberTown.nationDebt
                            context.sendMessage(
                                Message.raw("- ${memberTown.name}: ")
                                    .insert(Message.raw(plugin.economyService.formatAmount(debt)).color("#FF5555").bold(true))
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to get debts", e)
                context.sendMessage(MessageFormatter.error("Failed to get debts: ${e.message}"))
            }
        }
    }
}
