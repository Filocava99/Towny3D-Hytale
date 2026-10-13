package it.cava99.towny3d.util

import com.hypixel.hytale.server.core.Message
import java.awt.Color

/**
 * Utility class for formatting chat messages with colors and styling.
 * Uses Hytale's Message API for rich text formatting.
 */
object MessageFormatter {

    // ========== Colors ==========

    private val COLOR_SUCCESS = "#55FF55"  // Bright green
    private val COLOR_ERROR = "#FF5555"    // Bright red
    private val COLOR_WARNING = "#FFFF55"  // Bright yellow
    private val COLOR_HIGHLIGHT = "#FFAA00" // Gold
    private val COLOR_INFO = "#55FFFF"     // Aqua
    private val COLOR_SECONDARY = "#AAAAAA" // Gray
    private val COLOR_TOWN = "#55FF55"     // Green for towns
    private val COLOR_NATION = "#FFFF55"   // Yellow for nations
    private val COLOR_RESIDENT = "#AAFFFF" // Light cyan for residents

    // Translation support
    private var translationManager: it.cava99.towny3d.translation.TranslationManager? = null

    /**
     * Set the TranslationManager instance. Called during plugin setup.
     */
    fun setTranslationManager(translator: it.cava99.towny3d.translation.TranslationManager) {
        translationManager = translator
    }

    // ========== Simple Message Formatters (String-based for reliability) ==========

    /**
     * Creates the plugin prefix message.
     */
    fun prefix(): String = "[Towny] "

    /**
     * Formats a success message in green.
     * Supports translation keys with placeholders, or raw text for backward compatibility.
     */
    fun success(keyOrMessage: String, vararg args: Pair<String, Any>): Message {
        val message = getTranslationOrRaw(keyOrMessage, *args)
        return Message.raw(prefix() + message).color(COLOR_SUCCESS)
    }

    /**
     * Formats an error message in red.
     */
    fun error(keyOrMessage: String, vararg args: Pair<String, Any>): Message {
        val message = getTranslationOrRaw(keyOrMessage, *args)
        return Message.raw(prefix() + message).color(COLOR_ERROR)
    }

    /**
     * Formats an info message in aqua.
     */
    fun info(keyOrMessage: String, vararg args: Pair<String, Any>): Message {
        val message = getTranslationOrRaw(keyOrMessage, *args)
        return Message.raw(prefix() + message).color(COLOR_INFO)
    }

    /**
     * Formats a warning message in yellow.
     */
    fun warning(keyOrMessage: String, vararg args: Pair<String, Any>): Message {
        val message = getTranslationOrRaw(keyOrMessage, *args)
        return Message.raw(prefix() + message).color(COLOR_WARNING)
    }

    // ========== Legacy Helper Methods ==========

    /**
     * Formats a town name with green color.
     */
    fun formatTown(name: String, isPublic: Boolean = true): Message {
        val result = Message.raw(name).color(COLOR_TOWN).bold(true)
        return if (!isPublic) {
            result.insert(Message.raw(" [Private]").color(COLOR_SECONDARY))
        } else {
            result
        }
    }

    /**
     * Formats a resident name with role prefix.
     */
    fun formatResident(name: String, isMayor: Boolean = false, isAssistant: Boolean = false): Message {
        val role = when {
            isMayor -> "[Mayor] "
            isAssistant -> "[Asst] "
            else -> ""
        }
        val roleMsg = if (role.isNotEmpty()) {
            Message.raw(role).color(COLOR_HIGHLIGHT).bold(true)
        } else {
            Message.empty()
        }
        return roleMsg.insert(Message.raw(name).color(COLOR_RESIDENT))
    }

    /**
     * Formats a money amount with gold color.
     */
    fun formatMoney(amount: Double, currencySymbol: String = "coins"): Message {
        return Message.raw("${String.format("%.2f", amount)} $currencySymbol")
            .color(COLOR_HIGHLIGHT)
            .bold(true)
    }

    /**
     * Formats a balance display.
     */
    fun formatBalance(balance: Double, economyService: it.cava99.towny3d.economy.EconomyService): Message {
        return Message.raw("Balance: " + economyService.formatAmount(balance))
            .color(COLOR_SECONDARY)
            .insert(Message.raw(economyService.formatAmount(balance)).color(COLOR_HIGHLIGHT).bold(true))
    }

    /**
     * Formats a label/secondary text in gray.
     */
    fun label(message: String): Message {
        return Message.raw(message).color(COLOR_SECONDARY)
    }

    /**
     * Formats a header/section divider with styling.
     */
    fun formatHeader(title: String): String {
        return "\n=== $title ===\n"
    }

    /**
     * Formats a nation name with yellow color.
     */
    fun formatNation(name: String, isPublic: Boolean = true): Message {
        val result = Message.raw(name).color(COLOR_NATION).bold(true)
        return if (!isPublic) {
            result.insert(Message.raw(" [Private]").color(COLOR_SECONDARY))
        } else {
            result
        }
    }

    /**
     * Formats coordinates as a simple string.
     */
    fun formatCoords(x: Int, y: Int, z: Int): String {
        return "($x, $y, $z)"
    }

    /**
     * Formats a town chat message.
     */
    fun formatTownChat(sender: String, message: String): Message {
        return Message.raw("[Town] ")
            .color(COLOR_TOWN)
            .insert(Message.raw("$sender: ").color(COLOR_RESIDENT).bold(true))
            .insert(Message.raw(message).color("#FFFFFF"))
    }

    /**
     * Formats a nation chat message.
     */
    fun formatNationChat(sender: String, message: String): Message {
        return Message.raw("[Nation] ")
            .color(COLOR_NATION)
            .insert(Message.raw("$sender: ").color(COLOR_RESIDENT).bold(true))
            .insert(Message.raw(message).color("#FFFFFF"))
    }

    /**
     * Get translation if key exists, otherwise return raw message (backward compatible).
     */
    private fun getTranslationOrRaw(key: String, vararg args: Pair<String, Any>): String {
        val tm = translationManager
        return if (tm != null && tm.hasKey(key)) {
            tm.get(key, *args)
        } else {
            // Raw message - replace any placeholders manually
            var result = key
            args.forEach { (placeholder, value) ->
                result = result.replace("{$placeholder}", value.toString())
            }
            result
        }
    }
}
