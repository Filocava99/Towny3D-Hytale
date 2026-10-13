package it.cava99.towny3d.map

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.protocol.packets.worldmap.MapImage
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection
import com.hypixel.hytale.server.core.universe.world.chunk.palette.BitFieldArr
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import it.cava99.towny3d.Towny3D
import java.util.concurrent.CompletableFuture

/**
 * Image builder for rendering town claims on the world map.
 * Ports CustomImageBuilder from SimpleClaims, adapted for Towny.
 *
 * Renders full terrain (blocks, heights, fluids, biome tints, shading)
 * then overlays town claim colors with borders.
 *
 * This class orchestrates:
 * - Chunk data fetching and sampling
 * - Terrain data collection (heights, blocks, fluids)
 * - Color mapping via [ColorMapper]
 * - Image rendering via [ImageRenderer]
 */
class TownyImageBuilder(
    val index: Long,
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val world: World
) {
    /** Raw ARGB pixels, populated during rendering, then palette-encoded into [image]. */
    private val rawPixels = IntArray(imageWidth * imageHeight)

    /**
     * Palette-indexed image, built by [encodeToPalette] once rendering is complete.
     *
     * The MapImage wire format changed from a flat `int[] data` (ARGB) to a
     * palette + bit-packed indices format. We now render into [rawPixels] and
     * quantize/pack at the end.
     */
    lateinit var image: MapImage
        private set

    private val sampleWidth: Int = minOf(32, imageWidth)
    private val sampleHeight: Int = minOf(32, imageHeight)
    private val blockStepX: Int = maxOf(1, 32 / imageWidth)
    private val blockStepZ: Int = maxOf(1, 32 / imageHeight)

    // Sample arrays for terrain data
    private val heightSamples = ShortArray(sampleWidth * sampleHeight)
    private val tintSamples = IntArray(sampleWidth * sampleHeight)
    private val blockSamples = IntArray(sampleWidth * sampleHeight)
    private val neighborHeightSamples = ShortArray((sampleWidth + 2) * (sampleHeight + 2))
    private val fluidDepthSamples = ShortArray(sampleWidth * sampleHeight)
    private val environmentSamples = IntArray(sampleWidth * sampleHeight)
    private val fluidSamples = IntArray(sampleWidth * sampleHeight)

    private val outColor = ImageRenderer.Color()

    private var worldChunk: WorldChunk? = null
    private var fluidSections: Array<FluidSection?> = arrayOfNulls(10)

    // Delegated components
    private val colorMapper = ColorMapper(Towny3D.instance)
    private val imageRenderer = ImageRenderer()

    /**
     * Fetches the chunk data asynchronously.
     */
    private fun fetchChunk(): CompletableFuture<TownyImageBuilder?> {
        return world.chunkStore.getChunkReferenceAsync(index).thenApplyAsync(
            { ref ->
                if (ref != null && ref.isValid) {
                    worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
                    val chunkColumn = ref.store.getComponent(ref, ChunkColumn.getComponentType())

                    // Fetch all 10 fluid sections
                    if (chunkColumn != null) {
                        for (y in 0..9) {
                            val sectionRef = chunkColumn.getSection(y)
                            if (sectionRef != null && sectionRef.isValid) {
                                fluidSections[y] = world.chunkStore.store.getComponent(sectionRef, FluidSection.getComponentType())
                            }
                        }
                    }
                    this@TownyImageBuilder
                } else {
                    null
                }
            },
            world
        )
    }

    /**
     * Samples neighbor chunks for height-based shading.
     */
    private fun sampleNeighborsSync(): CompletableFuture<TownyImageBuilder> {
        val chunk = worldChunk ?: return CompletableFuture.completedFuture(this)

        val north = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x, chunk.z - 1))
            .thenAcceptAsync({ ref -> fillNorthEdge(ref) }, world)
        val south = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x, chunk.z + 1))
            .thenAcceptAsync({ ref -> fillSouthEdge(ref) }, world)
        val west = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x - 1, chunk.z))
            .thenAcceptAsync({ ref -> fillWestEdge(ref) }, world)
        val east = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x + 1, chunk.z))
            .thenAcceptAsync({ ref -> fillEastEdge(ref) }, world)
        val northeast = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x + 1, chunk.z - 1))
            .thenAcceptAsync({ ref -> fillNorthEastCorner(ref) }, world)
        val northwest = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x - 1, chunk.z - 1))
            .thenAcceptAsync({ ref -> fillNorthWestCorner(ref) }, world)
        val southeast = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x + 1, chunk.z + 1))
            .thenAcceptAsync({ ref -> fillSouthEastCorner(ref) }, world)
        val southwest = world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunk.x - 1, chunk.z + 1))
            .thenAcceptAsync({ ref -> fillSouthWestCorner(ref) }, world)

        return CompletableFuture.allOf(north, south, west, east, northeast, northwest, southeast, southwest)
            .thenApply { this }
    }

    private fun fillNorthEdge(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                val z = (sampleHeight - 1) * blockStepZ
                for (ix in 0 until sampleWidth) {
                    val x = ix * blockStepX
                    neighborHeightSamples[1 + ix] = worldChunk.getHeight(x, z)
                }
            }
        }
    }

    private fun fillSouthEdge(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                val z = 0
                val neighbourStartIndex = (sampleHeight + 1) * (sampleWidth + 2) + 1
                for (ix in 0 until sampleWidth) {
                    val x = ix * blockStepX
                    neighborHeightSamples[neighbourStartIndex + ix] = worldChunk.getHeight(x, z)
                }
            }
        }
    }

    private fun fillWestEdge(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                val x = (sampleWidth - 1) * blockStepX
                for (iz in 0 until sampleHeight) {
                    val z = iz * blockStepZ
                    neighborHeightSamples[(iz + 1) * (sampleWidth + 2)] = worldChunk.getHeight(x, z)
                }
            }
        }
    }

    private fun fillEastEdge(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                val x = 0
                for (iz in 0 until sampleHeight) {
                    val z = iz * blockStepZ
                    neighborHeightSamples[(iz + 1) * (sampleWidth + 2) + sampleWidth + 1] = worldChunk.getHeight(x, z)
                }
            }
        }
    }

    private fun fillNorthEastCorner(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                neighborHeightSamples[0] = worldChunk.getHeight(0, (sampleHeight - 1) * blockStepZ)
            }
        }
    }

    private fun fillNorthWestCorner(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                neighborHeightSamples[sampleWidth + 1] = worldChunk.getHeight((sampleWidth - 1) * blockStepX, (sampleHeight - 1) * blockStepZ)
            }
        }
    }

    private fun fillSouthEastCorner(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                val idx = (sampleHeight + 1) * (sampleWidth + 2) + sampleWidth + 1
                neighborHeightSamples[idx] = worldChunk.getHeight(0, 0)
            }
        }
    }

    private fun fillSouthWestCorner(ref: Ref<ChunkStore>?) {
        if (ref != null && ref.isValid) {
            val worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())
            if (worldChunk != null) {
                val idx = (sampleHeight + 1) * (sampleWidth + 2)
                neighborHeightSamples[idx] = worldChunk.getHeight((sampleWidth - 1) * blockStepX, 0)
            }
        }
    }

    /**
     * Generates the map image with terrain and claim overlay.
     */
    private fun generateImageAsync(): TownyImageBuilder {
        val chunk = worldChunk ?: return this

        // Sample terrain data
        sampleTerrainData(chunk)

        // Copy height samples to neighbor array center
        for (iz in 0 until sampleHeight) {
            System.arraycopy(
                heightSamples, iz * sampleWidth,
                neighborHeightSamples, (iz + 1) * (sampleWidth + 2) + 1,
                sampleWidth
            )
        }

        // Get claim info for this chunk
        val chunkX = ChunkUtil.xOfChunkIndex(index)
        val chunkZ = ChunkUtil.zOfChunkIndex(index)
        val colorInfo = colorMapper.getChunkColorInfo(chunkX, chunkZ)
        val nearbyRegions = colorMapper.getNearbyRegions(chunkX, chunkZ)

        // Render pixels
        renderPixels(colorInfo, nearbyRegions)

        // Encode rendered pixels into a palette-indexed MapImage
        image = encodeToPalette()

        return this
    }

    /**
     * Encodes [rawPixels] into a palette-indexed [MapImage].
     *
     * Colours are quantized (with Bayer-matrix dithering in transition zones) to keep
     * the palette small, then each pixel is mapped to its palette index and bit-packed
     * via [BitFieldArr]. Ports `CustomImageBuilder#encodeToPalette` from SimpleClaims.
     */
    private fun encodeToPalette(): MapImage {
        val pixelCount = rawPixels.size
        val processedPixels = IntArray(pixelCount)
        val uniqueColors = HashSet<Int>()

        for (i in 0 until pixelCount) {
            val pixel = if (Companion.quantizationEnabled) {
                if (isInTransitionZone(i)) {
                    quantizeColorWithDither(rawPixels[i], i % imageWidth, i / imageWidth)
                } else {
                    quantizeColor(rawPixels[i])
                }
            } else {
                rawPixels[i]
            }
            processedPixels[i] = pixel
            uniqueColors.add(pixel)
        }

        val palette = uniqueColors.toIntArray()
        val bitsPerIndex = calculateBitsRequired(palette.size)
        val colorToIndex = HashMap<Int, Int>(palette.size)
        for (i in palette.indices) {
            colorToIndex[palette[i]] = i
        }

        val indices = BitFieldArr(bitsPerIndex, pixelCount)
        for (i in 0 until pixelCount) {
            indices.set(i, colorToIndex.getValue(processedPixels[i]))
        }

        return MapImage(imageWidth, imageHeight, palette, bitsPerIndex.toByte(), indices.get())
    }

    /**
     * Returns true if the pixel at [index] neighbours a differently-quantized pixel,
     * i.e. it sits on a colour boundary where dithering helps.
     */
    private fun isInTransitionZone(index: Int): Boolean {
        val centerQuantized = quantizeColor(rawPixels[index])
        val x = index % imageWidth
        val y = index / imageWidth

        for (dy in -2..2) {
            for (dx in -2..2) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until imageWidth && ny in 0 until imageHeight) {
                    if (quantizeColor(rawPixels[ny * imageWidth + nx]) != centerQuantized) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Samples terrain data (heights, blocks, fluids) from the chunk.
     */
    private fun sampleTerrainData(chunk: WorldChunk) {
        for (ix in 0 until sampleWidth) {
            for (iz in 0 until sampleHeight) {
                val sampleIndex = iz * sampleWidth + ix
                val x = ix * blockStepX
                val z = iz * blockStepZ
                val height = chunk.getHeight(x, z)
                val tint = chunk.getTint(x, z)

                heightSamples[sampleIndex] = height
                tintSamples[sampleIndex] = tint
                blockSamples[sampleIndex] = chunk.getBlock(x, height.toInt(), z)

                // Sample fluid data
                val fluidData = sampleFluidData(chunk, x, z, height)
                fluidDepthSamples[sampleIndex] = fluidData.depth
                environmentSamples[sampleIndex] = fluidData.environmentId
                fluidSamples[sampleIndex] = fluidData.fluidId
            }
        }
    }

    /**
     * Data class for fluid sampling results.
     */
    private data class FluidSampleResult(
        val fluidId: Int,
        val depth: Short,
        val environmentId: Int
    )

    /**
     * Samples fluid data at a position.
     */
    private fun sampleFluidData(chunk: WorldChunk, x: Int, z: Int, height: Short): FluidSampleResult {
        var fluidId = 0
        var fluidTop = 320
        var fluid: Fluid? = null
        val chunkYGround = ChunkUtil.chunkCoordinate(height.toInt())
        var chunkY = 9

        loop@while (chunkY >= 0 && chunkY >= chunkYGround) {
            val fluidSection = fluidSections[chunkY]
            if (fluidSection != null && !fluidSection.isEmpty) {
                val minBlockY = maxOf(ChunkUtil.minBlock(chunkY).toInt(), height.toInt())
                val maxBlockY = ChunkUtil.maxBlock(chunkY).toInt()

                for (blockY in maxBlockY downTo minBlockY) {
                    fluidId = fluidSection.getFluidId(x, blockY, z)
                    if (fluidId != 0) {
                        fluid = Fluid.getAssetMap().getAsset(fluidId)
                        fluidTop = blockY
                        break@loop
                    }
                }
                chunkY--
            } else {
                chunkY--
            }
        }

        // Find fluid bottom
        var fluidBottom = height.toInt()
        var bottomChunkY = chunkY
        while (bottomChunkY >= 0 && bottomChunkY >= chunkYGround) {
            val fluidSection = fluidSections[bottomChunkY]
            if (fluidSection == null || fluidSection.isEmpty) {
                fluidBottom = minOf(ChunkUtil.maxBlock(bottomChunkY).toInt() + 1, fluidTop)
                break
            }

            val minBlockY = maxOf(ChunkUtil.minBlock(bottomChunkY).toInt(), height.toInt())
            val maxBlockY = minOf(ChunkUtil.maxBlock(bottomChunkY).toInt(), fluidTop - 1)

            for (blockY in maxBlockY downTo minBlockY) {
                val nextFluidId = fluidSection.getFluidId(x, blockY, z)
                if (nextFluidId != fluidId) {
                    val nextFluid = Fluid.getAssetMap().getAsset(nextFluidId)
                    if (fluid?.particleColor != nextFluid?.particleColor) {
                        fluidBottom = blockY + 1
                        break
                    }
                }
            }
            bottomChunkY--
        }

        val fluidDepth = if (fluidId != 0) (fluidTop - fluidBottom + 1).toShort() else 0
        val environmentId = chunk.blockChunk?.getEnvironment(x, fluidTop, z) ?: 0

        return FluidSampleResult(fluidId, fluidDepth, environmentId)
    }

    /**
     * Renders all pixels for the map image.
     */
    private fun renderPixels(
        colorInfo: ColorMapper.ChunkColorInfo,
        nearbyRegions: Array<it.cava99.towny3d.model.RegionId?>
    ) {
        val imageToSampleRatioWidth = sampleWidth.toFloat() / imageWidth
        val imageToSampleRatioHeight = sampleHeight.toFloat() / imageHeight
        val blockPixelWidth = maxOf(1, imageWidth / sampleWidth)
        val blockPixelHeight = maxOf(1, imageHeight / sampleHeight)

        for (ix in 0 until imageWidth) {
            for (iz in 0 until imageHeight) {
                renderPixel(
                    ix, iz,
                    imageToSampleRatioWidth, imageToSampleRatioHeight,
                    blockPixelWidth, blockPixelHeight,
                    colorInfo, nearbyRegions
                )
            }
        }
    }

    /**
     * Renders a single pixel.
     */
    private fun renderPixel(
        ix: Int, iz: Int,
        imageToSampleRatioWidth: Float, imageToSampleRatioHeight: Float,
        blockPixelWidth: Int, blockPixelHeight: Int,
        colorInfo: ColorMapper.ChunkColorInfo,
        nearbyRegions: Array<it.cava99.towny3d.model.RegionId?>
    ) {
        val sampleX = minOf((ix * imageToSampleRatioWidth).toInt(), sampleWidth - 1)
        val sampleZ = minOf((iz * imageToSampleRatioHeight).toInt(), sampleHeight - 1)
        val sampleIndex = sampleZ * sampleWidth + sampleX
        val blockPixelX = ix % blockPixelWidth
        val blockPixelZ = iz % blockPixelHeight

        val height = heightSamples[sampleIndex]
        val tint = tintSamples[sampleIndex]
        val blockId = blockSamples[sampleIndex]

        // Get block color
        imageRenderer.getBlockColor(blockId, tint, outColor)

        // Apply claim overlay if claimed
        if (colorInfo.color != null) {
            val isBorder = colorMapper.isBorder(
                ix, iz, imageWidth, imageHeight,
                nearbyRegions, colorInfo.townId
            )
            imageRenderer.applyTownColor(colorInfo.color, outColor, isBorder)
        }

        // Calculate shading from neighbors
        val shade = getShadeForPixel(sampleX, sampleZ, blockPixelX, blockPixelZ, blockPixelWidth, blockPixelHeight, height)
        outColor.multiply(shade)

        // Apply fluid color if underwater
        if (height < 320) {
            val fluidId = fluidSamples[sampleIndex]
            if (fluidId != 0) {
                val fluidDepth = fluidDepthSamples[sampleIndex]
                val environmentId = environmentSamples[sampleIndex]
                imageRenderer.getFluidColor(fluidId, environmentId, fluidDepth, outColor)
            }
        }

        // Pack pixel (RGBA format) into the raw pixel buffer; palette-encoded later.
        val pixelIndex = iz * imageWidth + ix
        rawPixels[pixelIndex] = outColor.pack()
    }

    /**
     * Gets the shade multiplier for a pixel based on neighbor heights.
     */
    private fun getShadeForPixel(
        sampleX: Int, sampleZ: Int,
        blockPixelX: Int, blockPixelZ: Int,
        blockPixelWidth: Int, blockPixelHeight: Int,
        height: Short
    ): Float {
        val north = neighborHeightSamples[sampleZ * (sampleWidth + 2) + sampleX + 1]
        val south = neighborHeightSamples[(sampleZ + 2) * (sampleWidth + 2) + sampleX + 1]
        val west = neighborHeightSamples[(sampleZ + 1) * (sampleWidth + 2) + sampleX]
        val east = neighborHeightSamples[(sampleZ + 1) * (sampleWidth + 2) + sampleX + 2]
        val northWest = neighborHeightSamples[sampleZ * (sampleWidth + 2) + sampleX]
        val northEast = neighborHeightSamples[sampleZ * (sampleWidth + 2) + sampleX + 2]
        val southWest = neighborHeightSamples[(sampleZ + 2) * (sampleWidth + 2) + sampleX]
        val southEast = neighborHeightSamples[(sampleZ + 2) * (sampleWidth + 2) + sampleX + 2]

        return imageRenderer.shadeFromHeights(
            blockPixelX, blockPixelZ, blockPixelWidth, blockPixelHeight,
            height, north, south, west, east, northWest, northEast, southWest, southEast
        )
    }

    companion object {
        /**
         * Builds a TownyImageBuilder for the given chunk.
         */
        fun build(index: Long, imageWidth: Int, imageHeight: Int, world: World): CompletableFuture<TownyImageBuilder?> {
            val builder = TownyImageBuilder(index, imageWidth, imageHeight, world)
            return builder.fetchChunk()
                .thenCompose { b ->
                    if (b != null) b.sampleNeighborsSync()
                    else CompletableFuture.completedFuture(null)
                }
                .thenApplyAsync({ b -> b?.generateImageAsync() }, world)
        }

        // ---- Palette quantization (ports SimpleClaims CustomImageBuilder) ----

        @Volatile
        private var quantizationEnabled = true

        private const val QUANTIZE_STEP = 8
        private const val QUANTIZE_HALF = 4
        private val BAYER_MATRIX = arrayOf(
            intArrayOf(0, 8, 2, 10),
            intArrayOf(12, 4, 14, 6),
            intArrayOf(3, 11, 1, 9),
            intArrayOf(15, 7, 13, 5)
        )

        private fun quantizeChannel(value: Int): Int =
            minOf(255, (value + QUANTIZE_HALF) / QUANTIZE_STEP * QUANTIZE_STEP)

        private fun quantizeChannelWithDither(value: Int, ditherOffset: Int): Int {
            val adjusted = (value + ditherOffset).coerceIn(0, 255)
            return minOf(255, (adjusted + QUANTIZE_HALF) / QUANTIZE_STEP * QUANTIZE_STEP)
        }

        /** Pixel layout is R<<24 | G<<16 | B<<8 | A (matches [ImageRenderer.Color.pack]). */
        private fun quantizeColor(argb: Int): Int {
            val r = quantizeChannel(argb shr 24 and 255)
            val g = quantizeChannel(argb shr 16 and 255)
            val b = quantizeChannel(argb shr 8 and 255)
            val a = argb and 255
            return (r shl 24) or (g shl 16) or (b shl 8) or a
        }

        private fun quantizeColorWithDither(argb: Int, x: Int, y: Int): Int {
            val bayerValue = BAYER_MATRIX[y and 3][x and 3]
            val ditherOffset = (bayerValue - 8) * QUANTIZE_STEP / 16
            val r = quantizeChannelWithDither(argb shr 24 and 255, ditherOffset)
            val g = quantizeChannelWithDither(argb shr 16 and 255, ditherOffset)
            val b = quantizeChannelWithDither(argb shr 8 and 255, ditherOffset)
            val a = argb and 255
            return (r shl 24) or (g shl 16) or (b shl 8) or a
        }

        private fun calculateBitsRequired(colorCount: Int): Int = when {
            colorCount <= 16 -> 4
            colorCount <= 256 -> 8
            colorCount <= 4096 -> 12
            else -> 16
        }
    }
}
