package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.AbstractCommand
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.TownyLog
import it.cava99.towny3d.command.chat.TownChatCommand
import it.cava99.towny3d.command.chat.NationChatCommand
import it.cava99.towny3d.command.chat.GlobalChatCommand
import it.cava99.towny3d.command.invite.AcceptCommand
import it.cava99.towny3d.command.invite.DeclineCommand

/**
 * Registers all Towny commands with Hytale's CommandRegistry.
 *
 * Provides centralized command registration with aliases and
 * proper lifecycle management.
 */
class CommandRegistrar(
    private val plugin: Towny3D
) {
    private val commandRegistry = plugin.getCommandRegistry()
    private val registeredCommands = mutableListOf<AbstractCommand>()

    /**
     * Registers all Towny commands.
     */
    fun registerAllCommands() {
        try {
            // Main command collections (these register their own subcommands)
            registerCommand(TownCommand())
            registerCommand(TownAliasCommand())  // /t alias for /town
            registerCommand(NationCommand())
            registerCommand(PlotCommand())
            registerCommand(ClaimCommand())
            registerCommand(ToggleCommand())
            registerCommand(ChatCommand())
            registerCommand(InviteCommand())
            registerCommand(AdminCommand())
            registerCommand(ResidentCommand())
            registerCommand(TaxCommand())

            // Aliases
            registerCommand(ResCommand())  // /res alias for /resident

            // Quick aliases for chat
            registerCommand(TownChatCommand())
            registerCommand(NationChatCommand())
            registerCommand(GlobalChatCommand())

            // Quick aliases for invitations
            registerCommand(AcceptCommand())
            registerCommand(DeclineCommand())

            // Selection commands (WorldEdit-style position selection)
            registerCommand(Pos1Command())  // /p1
            registerCommand(Pos2Command())  // /p2

            TownyLog.info("Successfully registered ${registeredCommands.size} commands")
        } catch (e: Exception) {
            TownyLog.error("Failed to register commands: ${e.message}", e)
            throw e
        }
    }

    /**
     * Registers a single command and tracks it for cleanup.
     */
    private fun registerCommand(command: AbstractCommand) {
        commandRegistry.registerCommand(command)
        registeredCommands.add(command)

        val commandName = command.javaClass.simpleName
        TownyLog.info("Registered command: $commandName")
    }

    /**
     * Unregisters all commands.
     *
     * Note: Hytale's CommandRegistry may not support unregistration,
     * so this primarily clears our tracking list.
     */
    fun unregisterAllCommands() {
        // Note: CommandRegistry doesn't have an unregisterCommand method
        // Commands will be cleaned up when the plugin is disabled
        registeredCommands.clear()
        TownyLog.info("Cleared command tracking list")
    }
}
