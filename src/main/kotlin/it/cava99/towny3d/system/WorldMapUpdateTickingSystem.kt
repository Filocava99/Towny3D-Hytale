package it.cava99.towny3d.system

import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.system.DelayedSystem
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import it.cava99.towny3d.map.TownyWorldMapProvider
import org.checkerframework.checker.nullness.compatqual.NonNullDecl

/**
 * Ticking system that processes queued world map chunk refreshes.
 *
 * After a claim/unclaim, chunks are queued for refresh. This system
 * clears the cached map images so the framework re-renders them
 * with updated claim overlays.
 */
class WorldMapUpdateTickingSystem : DelayedSystem<ChunkStore>(TICK_DELAY) {

    companion object {
        private const val TICK_DELAY = 3f
    }

    override fun delayedTick(v: Float, i: Int, @NonNullDecl store: Store<ChunkStore>) {
        val world = store.externalData.world
        val chunks = TownyWorldMapProvider.getAndClearChunksToRefresh()

        if (chunks.isEmpty()) return

        world.execute {
            world.worldMapManager.clearImagesInChunks(chunks)
            for (playerRef in world.playerRefs) {
                val ref = playerRef.reference ?: continue
                val player = world.entityStore.store.getComponent(
                    ref,
                    Player.getComponentType()
                )
                player?.worldMapTracker?.clearChunks(chunks)
            }
        }
    }
}
