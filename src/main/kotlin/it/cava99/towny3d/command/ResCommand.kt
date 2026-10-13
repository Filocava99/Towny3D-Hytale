package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /res - Alias for /resident command
 */
class ResCommand : AbstractCommandCollection("res", "Resident commands (alias)") {

    init {
        requirePermission("towny3d.resident")
        this.addSubCommand(ResInfoCommand())
        this.addSubCommand(ResListCommand())
        this.addSubCommand(ResBalanceCommand())
    }
}

/**
 * /res info [name] - Show resident information
 */
class ResInfoCommand : AbstractWorldCommand {

    private val nameArg = this.withOptionalArg("name", "Player name", ArgTypes.STRING)

    constructor() : super("info", "Show resident information") {
        requirePermission("towny3d.resident.info")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val targetName = if (nameArg.provided(context)) {
            nameArg.get(context) as? String
        } else {
            null
        }

        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use this command"))
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
                val resident = if (targetName != null) {
                    plugin.residentService.getResidentByName(targetName)
                } else {
                    plugin.residentService.getResidentByUuid(playerUuid)
                }

                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                val info = buildString {
                    append("\n=== Resident Info: ${resident.name} ===\n")
                    append("UUID: ${resident.uuid}\n")
                    append("Joined: ${resident.joinedAt}\n")
                    append("Last Online: ${resident.lastOnline}\n")

                    if (plugin.getConfig().enableEconomy) {
                        val balance = plugin.economyService.getBalance(resident.id)
                        append("Balance: ${plugin.economyService.formatAmount(balance)}\n")
                    }

                    if (resident.townId != null) {
                        val town = plugin.townService.getTown(resident.townId!!)
                        if (town != null) {
                            append("Town: ${town.name}\n")
                            val isMayor = town.mayorId == resident.id
                            append("Role: ${if (isMayor) "Mayor" else "Resident"}\n")
                        }
                    } else {
                        append("Town: None\n")
                    }

                    append("Chat Mode: ${resident.chatMode}\n")
                }

                context.sendMessage(Message.raw(info))

            } catch (e: Exception) {
                TownyLog.error("Failed to get resident info: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get resident info: ${e.message}"))
            }
        }
    }
}

/**
 * /res list - List all residents
 */
class ResListCommand : AbstractWorldCommand {

    constructor() : super("list", "List all residents") {
        requirePermission("towny3d.resident.list")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                val residents = plugin.residentService.getAllResidents()

                if (residents.isEmpty()) {
                    context.sendMessage(MessageFormatter.info("No residents found"))
                    return@launch
                }

                context.sendMessage(Message.raw("\n=== Residents (${residents.size}) ===\n\n"))
                for (resident in residents) {
                    val townInfo = if (resident.townId != null) {
                        val town = plugin.townService.getTown(resident.townId!!)
                        if (town != null) " [${town.name}]" else ""
                    } else {
                        ""
                    }
                    context.sendMessage(Message.raw("- ${resident.name}$townInfo\n"))
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to list residents: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to list residents: ${e.message}"))
            }
        }
    }
}

/**
 * /res balance - Check your balance
 */
class ResBalanceCommand : AbstractWorldCommand {

    constructor() : super("balance", "Check your balance") {
        requirePermission("towny3d.resident.balance")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use this command"))
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
                if (!plugin.getConfig().enableEconomy) {
                    context.sendMessage(MessageFormatter.error("Economy is disabled on this server"))
                    return@launch
                }

                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }

                val balance = plugin.economyService.getBalance(resident.id)
                val formatted = plugin.economyService.formatAmount(balance)

                val msg = MessageFormatter.success("Balance: $formatted")
                context.sendMessage(msg)

            } catch (e: Exception) {
                TownyLog.error("Failed to get balance: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to get balance: ${e.message}"))
            }
        }
    }
}
