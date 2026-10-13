package it.cava99.towny3d.gui.common

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil
import java.awt.Color

/**
 * Registers an optional (nullable) String field on a [BuilderCodec] builder, mapping it
 * to a property through the supplied accessors. This is the direct-BuilderCodec replacement
 * for the former `dev.unnm3d.codeclib` `@FieldName` + `CodecFactory.createClassCodec`
 * reflection helper used by GUI event-data classes.
 */
fun <T> BuilderCodec.Builder<T>.stringField(
    name: String,
    getter: (T) -> String?,
    setter: (T, String?) -> Unit
): BuilderCodec.Builder<T> {
    addField(KeyedCodec(name, Codec.STRING, false), setter, getter)
    return this
}

/**
 * Shared utilities for Towny3D GUIs.
 */
object GuiUtils {

    /**
     * Format a double as money (e.g., "$1,234.56")
     */
    fun formatMoney(amount: Double): String {
        return "$%,.2f".format(amount)
    }

    /**
     * Parse a money string to double (handles $, commas)
     */
    fun parseMoney(input: String): Double? {
        return input
            .replace("$", "")
            .replace(",", "")
            .trim()
            .toDoubleOrNull()
    }

    /**
     * Convert a hex color string to RGBA with alpha.
     * @param hexColor Hex color like "#3b82f6"
     * @param alpha Alpha value 0-255 (default 128 for 50%)
     * @return Hex string with alpha like "#3b82f680"
     */
    fun hexWithAlpha(hexColor: String, alpha: Int = 128): String {
        val color = Color.decode(hexColor)
        val withAlpha = Color(color.red, color.green, color.blue, alpha)
        return ColorParseUtil.colorToHexAlpha(withAlpha)
    }

    /**
     * Create a colored text message.
     * @param text The text content
     * @param color Optional hex color (e.g., "#FF0000")
     * @param bold Whether to make text bold
     * @return The formatted Message
     */
    fun coloredText(text: String, color: String? = null, bold: Boolean = false): Message {
        var msg = Message.raw(text)
        if (color != null) {
            msg = msg.color(color)
        }
        if (bold) {
            msg = msg.bold(true)
        }
        return msg
    }

    /**
     * Build a tooltip message by chaining multiple text segments.
     * @return A TooltipBuilder instance
     */
    fun tooltipBuilder(): TooltipBuilder = TooltipBuilder()
}

/**
 * Builder for creating multi-line tooltips using Message.insert().
 */
class TooltipBuilder {
    private var message: Message? = null

    /**
     * Append text to the tooltip.
     * @param text The text content
     * @param color Optional hex color (e.g., "#FF0000")
     * @param bold Whether to make text bold
     */
    fun append(text: String, color: String? = null, bold: Boolean = false): TooltipBuilder {
        val segment = GuiUtils.coloredText(text, color, bold)
        message = if (message == null) {
            segment
        } else {
            message!!.insert(segment)
        }
        return this
    }

    /**
     * Add a newline to the tooltip.
     */
    fun nl(): TooltipBuilder {
        message = if (message == null) {
            Message.raw("\n")
        } else {
            message!!.insert(Message.raw("\n"))
        }
        return this
    }

    /**
     * Build the final Message for use in tooltips.
     */
    fun build(): Message = message ?: Message.raw("")
}
