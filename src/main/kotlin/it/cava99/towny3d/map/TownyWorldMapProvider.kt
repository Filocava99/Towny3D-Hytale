package it.cava99.towny3d.map

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet

/**
 * World map provider for Towny plugin.
 * Shows town claims on the in-game map with colored chunks.
 */
class TownyWorldMapProvider : IWorldMapProvider {
    companion object {
        const val ID = "Towny"
        val CODEC: BuilderCodec<TownyWorldMapProvider> = BuilderCodec.builder(TownyWorldMapProvider::class.java, ::TownyWorldMapProvider).build()

        // Adjacent chunk offsets (3x3 grid around the target chunk)
        private val DX = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
        private val DZ = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)

        // Track chunks that need refreshing
        private val chunksToRefresh = LongOpenHashSet()

        /**
         * Marks a chunk and its 8 neighbors for map refresh.
         * Adjacent chunks are included because map images may render borders
         * that span chunk boundaries.
         *
         * @param chunkX chunk X coordinate
         * @param chunkZ chunk Z coordinate
         */
        fun markChunkForRefresh(chunkX: Int, chunkZ: Int) {
            synchronized(chunksToRefresh) {
                chunksToRefresh.add(ChunkUtil.indexChunk(chunkX, chunkZ))
                for (i in DX.indices) {
                    chunksToRefresh.add(ChunkUtil.indexChunk(chunkX + DX[i], chunkZ + DZ[i]))
                }
            }
        }

        /**
         * Returns all queued chunks and clears the queue.
         * Called by [it.cava99.towny3d.system.WorldMapUpdateTickingSystem].
         */
        fun getAndClearChunksToRefresh(): LongSet {
            synchronized(chunksToRefresh) {
                if (chunksToRefresh.isEmpty()) return LongOpenHashSet()
                val copy = LongOpenHashSet(chunksToRefresh)
                chunksToRefresh.clear()
                return copy
            }
        }
    }

    override fun getGenerator(world: World): IWorldMap {
        return TownyChunkWorldMap.INSTANCE
    }
}
