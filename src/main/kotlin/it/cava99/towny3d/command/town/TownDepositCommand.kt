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
 * /town deposit <amount> - Deposit money to town bank
 */
class TownDepositCommand : AbstractWorldCommand {

    private val amountArg = this.withRequiredArg("amount", "Amount to deposit", ArgTypes.STRING)

    constructor() : super("deposit", "Deposit to town bank") {
        requirePermission("towny3d.town.deposit")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val amountStr = amountArg.get(context) as? String
        if (amountStr.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town deposit <amount>"))
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            context.sendMessage(MessageFormatter.error("Amount must be a positive number"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can deposit to town bank"))
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
                // Get resident (with fallback to load/create if not in cache)
                val resident = CommandUtils.getOrCreateResident(plugin, playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Failed to load resident data. Please try again."))
                    return@launch
                }

                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You are not a member of any town"))
                    return@launch
                }

                if (!plugin.economyService.hasBalance(resident.id, amount)) {
                    context.sendMessage(MessageFormatter.error("You don't have enough money"))
                    return@launch
                }

                plugin.economyService.withdraw(resident.id, amount)
                plugin.townService.deposit(resident.townId!!, amount)

                val town = plugin.townService.getTown(resident.townId!!)
                context.sendMessage(
                    MessageFormatter.success("Deposited ")
                        .insert(Message.raw(plugin.economyService.formatAmount(amount)).color("#FFAA00").bold(true))
                        .insert(Message.raw(" to town bank"))
                )
                context.sendMessage(
                    MessageFormatter.info("New town balance: ")
                        .insert(Message.raw(plugin.economyService.formatAmount(town?.balance ?: 0.0)).color("#FFAA00").bold(true))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to deposit: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to deposit: ${e.message}"))
            }
        }
    }
}
