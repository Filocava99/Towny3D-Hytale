package it.cava99.towny3d.command.town

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
 * /town withdraw <amount> - Withdraw money from town bank
 */
class TownWithdrawCommand : AbstractWorldCommand {

    private val amountArg = this.withRequiredArg("amount", "Amount to withdraw", ArgTypes.STRING)

    constructor() : super("withdraw", "Withdraw from town bank") {
        requirePermission("towny3d.town.withdraw")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val amountStr = amountArg.get(context) as? String
        if (amountStr.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town withdraw <amount>"))
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            context.sendMessage(MessageFormatter.error("Amount must be a positive number"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can withdraw from town bank"))
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

                // Only mayor can withdraw
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can withdraw from town bank"))
                    return@launch
                }

                if (town.balance < amount) {
                    context.sendMessage(MessageFormatter.error("Town doesn't have enough money"))
                    return@launch
                }

                plugin.townService.withdraw(town.id, amount)
                plugin.economyService.deposit(resident.id, amount)

                context.sendMessage(
                    MessageFormatter.success("Withdrew ")
                        .insert(Message.raw(plugin.economyService.formatAmount(amount)).color("#FFAA00").bold(true))
                        .insert(Message.raw(" from town bank"))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to withdraw: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to withdraw: ${e.message}"))
            }
        }
    }
}
