package it.cava99.towny3d.map

import com.hypixel.hytale.protocol.packets.worldmap.MapMarker
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapSettings
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.map.WorldMap
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings
import it.unimi.dsi.fastutil.longs.LongIterator
import it.unimi.dsi.fastutil.longs.LongSet
import java.util.Collections
import java.util.concurrent.CompletableFuture

/**
 * World map implementation that renders town claims as colored chunks.
 *
 * Based on SimpleClaims SimpleClaimsChunkWorldMap.
 */
class TownyChunkWorldMap : IWorldMap {

    companion object {
        val INSTANCE = TownyChunkWorldMap()
    }

    override fun getWorldMapSettings(): WorldMapSettings {
        val settingsPacket = UpdateWorldMapSettings()
        settingsPacket.defaultScale = MapConstants.MAP_DEFAULT_SCALE
        settingsPacket.minScale = MapConstants.MAP_MIN_SCALE
        settingsPacket.maxScale = MapConstants.MAP_MAX_SCALE
        return WorldMapSettings(
            null,
            MapConstants.MAP_ZOOM_FACTOR,
            MapConstants.MAP_SCALE_DIVISOR,
            MapConstants.MAP_CHUNK_SIZE,
            MapConstants.MAP_IMAGE_WIDTH,
            settingsPacket
        )
    }

    override fun generate(
        world: World,
        imageWidth: Int,
        imageHeight: Int,
        chunksToGenerate: LongSet
    ): CompletableFuture<WorldMap> {
        // Create futures for each chunk
        val futures = arrayOfNulls<CompletableFuture<TownyImageBuilder?>>(chunksToGenerate.size)
        var futureIndex = 0

        val iterator: LongIterator = chunksToGenerate.iterator()
        while (iterator.hasNext()) {
            val chunkIndex = iterator.nextLong()
            futures[futureIndex++] = TownyImageBuilder.build(chunkIndex, imageWidth, imageHeight, world)
        }

        // Wait for all chunks to build, then combine into WorldMap
        return CompletableFuture.allOf(*futures).thenApply {
            val worldMap = WorldMap(futures.size)

            for (i in futures.indices) {
                val builder = futures[i]?.getNow(null)
                if (builder != null) {
                    worldMap.chunks.put(builder.index, builder.image)
                }
            }

            worldMap
        }
    }

    override fun generatePointsOfInterest(world: World): CompletableFuture<Map<String, MapMarker>> {
        // TODO(2026-02-22): Implement POI markers for town names
        // MapMarker API needs to be figured out - requires proper constructor parameters
        // For now, return empty map to avoid crashes
        return CompletableFuture.completedFuture(Collections.emptyMap())
    }
}
