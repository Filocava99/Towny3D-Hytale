package it.cava99.towny3d.gui

import com.hypixel.hytale.common.util.ArrayUtil
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.util.MathUtil
import com.hypixel.hytale.protocol.ToClientPacket
import com.hypixel.hytale.protocol.packets.setup.AssetFinalize
import com.hypixel.hytale.protocol.packets.setup.AssetInitialize
import com.hypixel.hytale.protocol.packets.setup.AssetPart
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild
import com.hypixel.hytale.protocol.packets.worldmap.MapImage
import com.hypixel.hytale.server.core.asset.common.CommonAsset
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry
import com.hypixel.hytale.server.core.io.PacketHandler
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.chunk.palette.BitFieldArr
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.chunk.ChunkWorldMap
import it.cava99.towny3d.util.TownyLog
import it.unimi.dsi.fastutil.longs.LongArraySet
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

/**
 * Generates a world map image for the chunks requested, combining them into a single PNG.
 * This is sent to the client to display as the background of the RegionMapGui.
 *
 * Uses the same hash for all generated images so the client only caches one image.
 */
class RegionMapAsset private constructor(
    private val data: ByteArray
) : CommonAsset(PATH, HASH, data) {

    companion object {
        // "Towny3D_RegionMap" in hex padded with 2 zeros at the start and then padded to 64 characters
        private const val HASH = "00546f776e7933445f526567696f6e4d61700000000000000000000000000000"
        private const val PATH = "UI/Custom/HytaleTowny/Map.png"

        /**
         * Get the empty/default asset from the registry.
         */
        fun empty(): CommonAsset? = CommonAssetRegistry.getByName(PATH)

        /**
         * Decodes a palette-indexed [MapImage] back into a raw ARGB pixel array.
         *
         * The new MapImage format stores colours in a [MapImage.palette] plus bit-packed
         * indices ([MapImage.packedIndices], [MapImage.bitsPerIndex]) instead of a flat
         * pixel array. When [MapImage.bitsPerIndex] is 0 the palette itself holds the
         * raw pixels (one entry per pixel, in order).
         */
        private fun getPixels(image: MapImage): IntArray? {
            val palette = image.palette ?: return null
            if (image.bitsPerIndex.toInt() == 0) return palette
            val packed = image.packedIndices ?: return null
            val pixelCount = image.width * image.height
            val pixels = IntArray(pixelCount)
            val indices = BitFieldArr(image.bitsPerIndex.toInt() and 0xFF, pixelCount)
            indices.set(packed)
            for (i in 0 until pixelCount) {
                pixels[i] = palette[indices.get(i)]
            }
            return pixels
        }

        /**
         * Generate a map image for the specified chunk range.
         *
         * @param player The player to generate the map for
         * @param minChunkX Minimum chunk X (inclusive)
         * @param minChunkZ Minimum chunk Z (inclusive)
         * @param maxChunkX Maximum chunk X (inclusive)
         * @param maxChunkZ Maximum chunk Z (inclusive)
         * @return CompletableFuture containing the generated asset, or null if generation failed
         */
        fun generate(
            player: PlayerRef,
            minChunkX: Int,
            minChunkZ: Int,
            maxChunkX: Int,
            maxChunkZ: Int
        ): CompletableFuture<RegionMapAsset?>? {
            val worldId = player.worldUuid ?: return null
            val world = Universe.get().getWorld(worldId) ?: return null
            val manager = world.worldMapManager
            val partSize = MathUtil.fastFloor(32.0f * manager.worldMapSettings.imageScale)

            // Build set of chunk indices
            val chunks = LongArraySet()
            for (x in minChunkX..maxChunkX) {
                for (z in minChunkZ..maxChunkZ) {
                    chunks.add(ChunkUtil.indexChunk(x, z))
                }
            }

            return ChunkWorldMap.INSTANCE.generate(world, partSize, partSize, chunks)
                .thenApply { map ->
                    try {
                        val image = BufferedImage(
                            partSize * (maxChunkX - minChunkX + 1),
                            partSize * (maxChunkZ - minChunkZ + 1),
                            BufferedImage.TYPE_INT_ARGB
                        )

                        for (x in minChunkX..maxChunkX) {
                            for (z in minChunkZ..maxChunkZ) {
                                val index = ChunkUtil.indexChunk(x, z)
                                val chunkImage = map.chunks[index] ?: continue
                                val pixels = getPixels(chunkImage) ?: continue
                                val width = chunkImage.width
                                val height = chunkImage.height

                                if (width != partSize || height != partSize) {
                                    TownyLog.warn("Chunk image size mismatch: ${width}x${height}")
                                    continue
                                }

                                val imageX = (x - minChunkX) * partSize
                                val imageZ = (z - minChunkZ) * partSize

                                for (i in pixels.indices) {
                                    val pixel = pixels[i]
                                    // Convert ABGR to ARGB
                                    val abgrToArgb = (pixel shl 24) or ((pixel ushr 8) and 0x00FFFFFF)
                                    val pixelX = i % width
                                    val pixelY = i / width
                                    image.setRGB(imageX + pixelX, imageZ + pixelY, abgrToArgb)
                                }
                            }
                        }

                        val baos = ByteArrayOutputStream()
                        ImageIO.write(image, "PNG", baos)
                        RegionMapAsset(baos.toByteArray())
                    } catch (e: Exception) {
                        TownyLog.error("Failed to generate region map image", e)
                        null
                    }
                }
        }

        /**
         * Send an asset to a player via their packet handler.
         * Splits large assets into 2.5MB chunks.
         */
        fun sendToPlayer(handler: PacketHandler, asset: CommonAsset) {
            val allBytes = asset.blob.join()
            val parts = ArrayUtil.split(allBytes, 2621440)
            val packets = ArrayList<ToClientPacket>(2 + parts.size)

            packets.add(AssetInitialize(asset.toPacket(), allBytes.size))
            for (partIndex in parts.indices) {
                packets.add(AssetPart(parts[partIndex]))
            }
            packets.add(AssetFinalize())

            handler.write(*packets.toTypedArray())
            handler.writeNoCache(RequestCommonAssetsRebuild())
        }
    }

    override fun getBlob0(): CompletableFuture<ByteArray> = CompletableFuture.completedFuture(data)
}
