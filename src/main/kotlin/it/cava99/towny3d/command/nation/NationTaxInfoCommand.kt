package it.cava99.towny3d.command.nation

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.model.TaxType
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /nation tax info - View current tax settings
 */
class NationTaxInfoCommand : AbstractWorldCommand {

    constructor() : super("info", "View tax settings") {
        requirePermission("towny3d.nation.tax")
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

                if (!town.hasNation || town.nationId == null) {
                    context.sendMessage(MessageFormatter.error("Your town is not in a nation"))
                    return@launch
                }

                val nation = plugin.nationService.getNation(town.nationId!!)
                if (nation == null) {
                    context.sendMessage(MessageFormatter.error("Nation not found"))
                    return@launch
                }

                context.sendMessage(Message.raw(MessageFormatter.formatHeader("${nation.name} Tax Info")))

                val tax = nation.taxToTowns
                if (tax.enabled) {
                    val taxText = if (tax.type == TaxType.FLAT) {
                        plugin.economyService.formatAmount(tax.amount)
                    } else {
                        "${tax.amount}%"
                    }
                    context.sendMessage(
                        Message.raw("Town Tax: ")
                            .insert(Message.raw(taxText).color("#FFAA00").bold(true))
                    )
                } else {
                    context.sendMessage(
                        Message.raw("Town Tax: ")
                            .insert(Message.raw("Disabled").color("#AAAAAA"))
                    )
                }

                context.sendMessage(
                    Message.raw("Nation Balance: ")
                        .insert(Message.raw(plugin.economyService.formatAmount(nation.balance)).color("#FFAA00").bold(true))
                )

            } catch (e: Exception) {
                TownyLog.error("Failed to get tax info", e)
                context.sendMessage(MessageFormatter.error("Failed to get tax info: ${e.message}"))
            }
        }
    }
}
