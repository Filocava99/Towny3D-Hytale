package it.cava99.towny3d.command.town

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
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
 * /town tax <flat|percent> <amount> - Set resident tax (mayor only)
 */
class TownTaxCommand : AbstractWorldCommand {

    private val typeArg = this.withRequiredArg("type", "Tax type (flat/percent)", ArgTypes.STRING)
    private val amountArg = this.withRequiredArg("amount", "Tax amount", ArgTypes.STRING)

    constructor() : super("tax", "Set resident tax") {
        requirePermission("towny3d.town.tax")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val taxType = typeArg.get(context) as? String
        val amountStr = amountArg.get(context) as? String

        if (taxType.isNullOrBlank() || amountStr.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /town tax <flat|percent> <amount>"))
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount < 0) {
            context.sendMessage(MessageFormatter.error("Amount must be a non-negative number"))
            return
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can set taxes"))
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

                // Only mayor can set taxes
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can set taxes"))
                    return@launch
                }

                val taxConfig = when (taxType.lowercase()) {
                    "flat" -> it.cava99.towny3d.model.TaxConfig.flat(amount)
                    "percent", "percentage" -> {
                        if (amount > 100) {
                            context.sendMessage(MessageFormatter.error("Percentage tax cannot exceed 100%"))
                            return@launch
                        }
                        it.cava99.towny3d.model.TaxConfig.percentage(amount)
                    }
                    else -> {
                        context.sendMessage(MessageFormatter.error("Invalid tax type. Use: flat or percent"))
                        return@launch
                    }
                }

                plugin.townService.setTaxToResidents(town.id, taxConfig)

                val formattedAmount = plugin.economyService.formatAmount(amount)
                val typeText = if (taxConfig.type == it.cava99.towny3d.model.TaxType.FLAT) {
                    "flat tax of $formattedAmount"
                } else {
                    "${amount}% tax"
                }

                context.sendMessage(
                    MessageFormatter.success("Resident tax set to ")
                        .insert(Message.raw(typeText).color("#FFAA00").bold(true))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to set tax: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to set tax: ${e.message}"))
            }
        }
    }
}

/**
 * /town tax clear - Clear resident tax (mayor only)
 */
class TownTaxClearCommand : AbstractWorldCommand {

    constructor() : super("clear", "Clear resident tax") {
        requirePermission("towny3d.town.tax")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can clear taxes"))
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

                // Only mayor can clear taxes
                if (town.mayorId != resident.id) {
                    context.sendMessage(MessageFormatter.error("Only the mayor can clear taxes"))
                    return@launch
                }

                plugin.townService.setTaxToResidents(town.id, it.cava99.towny3d.model.TaxConfig.disabled())

                context.sendMessage(MessageFormatter.success("Resident tax cleared"))

            } catch (e: Exception) {
                TownyLog.error("Failed to clear tax: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to clear tax: ${e.message}"))
            }
        }
    }
}

/**
 * /town tax info - View current tax settings
 */
class TownTaxInfoCommand : AbstractWorldCommand {

    constructor() : super("info", "View tax settings") {
        requirePermission("towny3d.town.tax")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can view tax info"))
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

                context.sendMessage(Message.raw(MessageFormatter.formatHeader("${town.name} Tax Info")))

                val tax = town.taxToResidents
                if (tax.enabled) {
                    val taxText = if (tax.type == it.cava99.towny3d.model.TaxType.FLAT) {
                        plugin.economyService.formatAmount(tax.amount)
                    } else {
                        "${tax.amount}%"
                    }
                    context.sendMessage(
                        Message.raw("Resident Tax: ")
                            .insert(Message.raw(taxText).color("#FFAA00").bold(true))
                    )
                } else {
                    context.sendMessage(
                        Message.raw("Resident Tax: ")
                            .insert(Message.raw("Disabled").color("#AAAAAA"))
                    )
                }

                context.sendMessage(
                    Message.raw("Nation Debt: ")
                        .insert(Message.raw(plugin.economyService.formatAmount(town.nationDebt)).color("#FF5555").bold(true))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to get tax info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get tax info: ${e.message}"))
            }
        }
    }
}

/**
 * /town tax - Tax management submenu
 */
class TownTaxCommandCollection : AbstractCommandCollection("tax", "Tax management commands") {
    init {
        requirePermission("towny3d.town.tax")
        this.addSubCommand(TownTaxCommand())
        this.addSubCommand(TownTaxClearCommand())
        this.addSubCommand(TownTaxInfoCommand())
    }
}
