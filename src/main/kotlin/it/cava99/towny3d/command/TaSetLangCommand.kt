package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import java.io.File
import javax.annotation.Nonnull
import kotlinx.coroutines.launch

/**
 * /ta setlang <locale> - Change server language
 */
class TaSetLangCommand : AbstractWorldCommand {

    private val localeArg = this.withRequiredArg("locale", "Language locale (e.g., en-US, it-IT)", com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING)

    constructor() : super("setlang", "Change server language") {
        requirePermission("towny3d.admin")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val locale = localeArg.get(context) as? String

        if (locale.isNullOrBlank()) {
            context.sendMessage(MessageFormatter.error("Usage: /ta setlang <locale>"))
            context.sendMessage(MessageFormatter.info("Available locales: en-US, it-IT"))
            return
        }

        val plugin = Towny3D.instance

        plugin.pluginScope.launch {
            try {
                // Check if locale directory exists
                val localeDir = File(plugin.dataDirectory.toFile(), "Server/Languages/$locale")
                if (!localeDir.exists()) {
                    context.sendMessage(
                        MessageFormatter.error("Locale '$locale' not found")
                    )

                    // List available locales
                    val languagesDir = File(plugin.dataDirectory.toFile(), "Server/Languages")
                    if (languagesDir.exists()) {
                        val available = languagesDir.listFiles()
                            ?.filter { it.isDirectory }
                            ?.map { it.name }
                            ?.joinToString(", ") ?: "none"
                        context.sendMessage(
                            MessageFormatter.info("Available locales: $available")
                        )
                    }
                    return@launch
                }

                // Load the new locale
                plugin.translationManager.loadLocale(locale)

                // Update config
                val newConfig = plugin.getConfig().copy(defaultLanguage = locale)
                plugin.setConfig(newConfig)

                context.sendMessage(
                    MessageFormatter.success("Language changed to $locale")
                )

                TownyLog.info("Language changed to '$locale' by admin")

            } catch (e: Exception) {
                TownyLog.error("Failed to change language: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to change language: ${e.message}"))
            }
        }
    }
}
