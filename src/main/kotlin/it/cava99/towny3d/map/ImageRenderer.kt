package it.cava99.towny3d.map

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid

/**
 * Handles terrain and claim color rendering for the world map.
 *
 * Contains all color manipulation and rendering logic including:
 * - Block color extraction with biome tinting
 * - Fluid color rendering
 * - Height-based shading
 * - Town claim overlay colors
 */
class ImageRenderer {

    /**
     * Mutable color class for efficient color manipulation during rendering.
     */
    class Color {
        var r: Int = 0
        var g: Int = 0
        var b: Int = 0
        var a: Int = 0

        fun set(r: Int, g: Int, b: Int, a: Int) {
            this.r = r
            this.g = g
            this.b = b
            this.a = a
        }

        fun pack(): Int {
            return ((r and 0xFF) shl 24) or ((g and 0xFF) shl 16) or ((b and 0xFF) shl 8) or (a and 0xFF)
        }

        fun multiply(value: Float) {
            r = (r * value).toInt().coerceIn(0, 255)
            g = (g * value).toInt().coerceIn(0, 255)
            b = (b * value).toInt().coerceIn(0, 255)
        }
    }

    /**
     * Gets the base color for a block with biome tinting applied.
     *
     * @param blockId The block type ID
     * @param biomeTintColor The biome tint color (RGB)
     * @param outColor The output color to modify
     */
    fun getBlockColor(blockId: Int, biomeTintColor: Int, outColor: Color) {
        val block = BlockType.getAssetMap().getAsset(blockId) ?: run {
            outColor.set(255, 255, 255, 255)
            return
        }

        val biomeTintR = (biomeTintColor shr 16) and 0xFF
        val biomeTintG = (biomeTintColor shr 8) and 0xFF
        val biomeTintB = biomeTintColor and 0xFF

        val tintUp = block.tintUp
        val hasTint = tintUp != null && tintUp.isNotEmpty()
        val selfTintR = if (hasTint) tintUp[0].red.toInt() and 0xFF else 255
        val selfTintG = if (hasTint) tintUp[0].green.toInt() and 0xFF else 255
        val selfTintB = if (hasTint) tintUp[0].blue.toInt() and 0xFF else 255

        val biomeTintMultiplier = block.biomeTintUp / 100.0f
        var tintColorR = (selfTintR + ((biomeTintR - selfTintR) * biomeTintMultiplier)).toInt().coerceIn(0, 255)
        var tintColorG = (selfTintG + ((biomeTintG - selfTintG) * biomeTintMultiplier)).toInt().coerceIn(0, 255)
        var tintColorB = (selfTintB + ((biomeTintB - selfTintB) * biomeTintMultiplier)).toInt().coerceIn(0, 255)

        val particleColor = block.particleColor
        if (particleColor != null && biomeTintMultiplier < 1.0f) {
            tintColorR = (tintColorR * (particleColor.red.toInt() and 0xFF) / 255).coerceIn(0, 255)
            tintColorG = (tintColorG * (particleColor.green.toInt() and 0xFF) / 255).coerceIn(0, 255)
            tintColorB = (tintColorB * (particleColor.blue.toInt() and 0xFF) / 255).coerceIn(0, 255)
        }

        outColor.set(tintColorR, tintColorG, tintColorB, 255)
    }

    /**
     * Applies a town/nation color overlay to a block color.
     *
     * @param colorRgb The town/nation color (RGB)
     * @param outColor The current color to modify
     * @param isBorder Whether this pixel is on a border (darker)
     */
    fun applyTownColor(colorRgb: Int, outColor: Color, isBorder: Boolean) {
        val r = (colorRgb shr 16) and 0xFF
        val g = (colorRgb shr 8) and 0xFF
        val b = colorRgb and 0xFF

        val shadeMultiplier = if (isBorder) 0.6f else 1.0f

        // Blend 60% town color with 40% current color
        val blend = 0.6f
        outColor.r = ((outColor.r * (1 - blend)) + (r * blend * shadeMultiplier)).toInt().coerceIn(0, 255)
        outColor.g = ((outColor.g * (1 - blend)) + (g * blend * shadeMultiplier)).toInt().coerceIn(0, 255)
        outColor.b = ((outColor.b * (1 - blend)) + (b * blend * shadeMultiplier)).toInt().coerceIn(0, 255)
    }

    /**
     * Applies fluid color overlay for underwater rendering.
     *
     * @param fluidId The fluid type ID
     * @param environmentId The environment ID for water tint
     * @param fluidDepth The depth of the fluid
     * @param outColor The current color to modify
     */
    fun getFluidColor(fluidId: Int, environmentId: Int, fluidDepth: Short, outColor: Color) {
        val tintColorR = 255
        val tintColorG = 255
        val tintColorB = 255

        val environment = Environment.getAssetMap().getAsset(environmentId)
        val waterTint = environment?.waterTint
        var r = tintColorR
        var g = tintColorG
        var b = tintColorB

        if (waterTint != null) {
            r = (r * (waterTint.red.toInt() and 0xFF) / 255).coerceIn(0, 255)
            g = (g * (waterTint.green.toInt() and 0xFF) / 255).coerceIn(0, 255)
            b = (b * (waterTint.blue.toInt() and 0xFF) / 255).coerceIn(0, 255)
        }

        val fluid = Fluid.getAssetMap().getAsset(fluidId)
        val particleColor = fluid?.particleColor
        if (particleColor != null) {
            r = (r * (particleColor.red.toInt() and 0xFF) / 255).coerceIn(0, 255)
            g = (g * (particleColor.green.toInt() and 0xFF) / 255).coerceIn(0, 255)
            b = (b * (particleColor.blue.toInt() and 0xFF) / 255).coerceIn(0, 255)
        }

        val depthMultiplier = minOf(1.0f, 1.0f / fluidDepth)
        outColor.r = ((r + ((outColor.r - r) * depthMultiplier))).toInt().coerceIn(0, 255)
        outColor.g = ((g + ((outColor.g - g) * depthMultiplier))).toInt().coerceIn(0, 255)
        outColor.b = ((b + ((outColor.b - b) * depthMultiplier))).toInt().coerceIn(0, 255)
    }

    /**
     * Calculates shade multiplier based on neighbor heights for 3D effect.
     *
     * @param blockPixelX The pixel X within the block
     * @param blockPixelZ The pixel Z within the block
     * @param blockPixelWidth The width of a block in pixels
     * @param blockPixelHeight The height of a block in pixels
     * @param height The center height
     * @param north Height to the north
     * @param south Height to the south
     * @param west Height to the west
     * @param east Height to the east
     * @param northWest Height to the north-west
     * @param northEast Height to the north-east
     * @param southWest Height to the south-west
     * @param southEast Height to the south-east
     * @return Shade multiplier (0.4 to 1.0)
     */
    fun shadeFromHeights(
        blockPixelX: Int, blockPixelZ: Int, blockPixelWidth: Int, blockPixelHeight: Int,
        height: Short, north: Short, south: Short, west: Short, east: Short,
        northWest: Short, northEast: Short, southWest: Short, southEast: Short
    ): Float {
        val u = (blockPixelX + 0.5f) / blockPixelWidth
        val v = (blockPixelZ + 0.5f) / blockPixelHeight
        val ud = (u + v) / 2.0f
        val vd = (1.0f - u + v) / 2.0f

        val dhdx1 = (height - west) * (1.0f - u) + (east - height) * u
        val dhdz1 = (height - north) * (1.0f - v) + (south - height) * v
        val dhdx2 = (height - northWest) * (1.0f - ud) + (southEast - height) * ud
        val dhdz2 = (height - northEast) * (1.0f - vd) + (southWest - height) * vd

        val dhdx = dhdx1 * 2.0f + dhdx2
        val dhdz = dhdz1 * 2.0f + dhdz2
        val dy = 3.0f

        val invS = 1.0f / kotlin.math.sqrt((dhdx * dhdx + dy * dy + dhdz * dhdz).toDouble())
        val nx = (dhdx * invS).toFloat()
        val ny = (dy * invS).toFloat()
        val nz = (dhdz * invS).toFloat()

        val lx = -0.2f
        val ly = 0.8f
        val lz = 0.5f
        val invL = 1.0f / kotlin.math.sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()
        val lxl = lx * invL
        val lyl = ly * invL
        val lzl = lz * invL

        val lambert = maxOf(0.0f, nx * lxl + ny * lyl + nz * lzl)
        val ambient = 0.4f
        val diffuse = 0.6f
        return ambient + diffuse * lambert
    }
}
