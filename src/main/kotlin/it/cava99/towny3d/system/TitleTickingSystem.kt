package it.cava99.towny3d.system

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.EventTitleUtil
import it.cava99.towny3d.Towny3D

import org.checkerframework.checker.nullness.compatqual.NonNullDecl
import org.checkerframework.checker.nullness.compatqual.NullableDecl
import java.awt.Color
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Entity ticking system that sends titles when players enter different territory types.
 *
 * This runs every tick for each player and checks if their territory type has changed.
 * Only sends a title when the territory TYPE changes (wilderness vs town).
 */
class TitleTickingSystem() : EntityTickingSystem<EntityStore>() {

    // Track last title text for each player (not the full territory type, just text comparison)
    private val playerLastTitle = ConcurrentHashMap<UUID, String>()

    companion object {
        private const val WILDERNESS_TEXT = "Wilderness"

        /** Default Y coordinate for region lookups */
        const val DEFAULT_Y_COORDINATE = 64

        /** Title fade in time in seconds */
        const val TITLE_FADE_IN_SECONDS = 2.0f

        /** Title stay time in seconds */
        const val TITLE_STAY_SECONDS = 0.5f

        /** Title fade out time in seconds */
        const val TITLE_FADE_OUT_SECONDS = 0.5f
    }

    override fun tick(
        v: Float,
        index: Int,
        @NonNullDecl archetypeChunk: ArchetypeChunk<EntityStore>,
        @NonNullDecl store: Store<EntityStore>,
        @NonNullDecl commandBuffer: CommandBuffer<EntityStore>
    ) {
        val ref = archetypeChunk.getReferenceTo(index)
        val playerRef = store.getComponent(ref, PlayerRef.getComponentType())
        val player = store.getComponent(ref, Player.getComponentType())

        if (playerRef == null || player == null) {
            return
        }

        // Get player's position
        val posX = playerRef.transform.position.x.toInt()
        val posZ = playerRef.transform.position.z.toInt()
        val worldName = player.world?.name ?: return

        // Access plugin and services synchronously
        val plugin = Towny3D.instance

        // Check if player is in a town claim using spatial cache (synchronous)
        val chunkPos = it.cava99.towny3d.model.ChunkPos.fromBlock(posX, posZ)
        val regionId = plugin.spatialIndex.getRegionAt(chunkPos, DEFAULT_Y_COORDINATE)

        val titleText: String
        val titleMessage: Message
        val subtitleMessage: Message

        if (regionId != null) {
            // Player is in a town - look up town synchronously from cache
            val region = plugin.regionCache.getSync(regionId)
            if (region != null) {
                val town = plugin.townCache.getSync(region.townId)
                if (town != null) {
                    val mayor = plugin.residentCache.getSync(town.mayorId)
                    titleText = town.name
                    titleMessage = Message.raw("Entering ${town.name}").color(Color.YELLOW)
                    subtitleMessage = Message.raw("Owned by ${mayor?.name ?: "Unknown"}").color(Color.GRAY)
                } else {
                    // Region exists but town not found - treat as wilderness
                    titleText = WILDERNESS_TEXT
                    titleMessage = Message.raw("Entering Wilderness").color(Color.GREEN)
                    subtitleMessage = Message.raw("Unclaimed territory")
                }
            } else {
                titleText = WILDERNESS_TEXT
                titleMessage = Message.raw("Entering Wilderness").color(Color.GREEN)
                subtitleMessage = Message.raw("Unclaimed territory")
            }
        } else {
            // Player is in wilderness
            titleText = WILDERNESS_TEXT
            titleMessage = Message.raw("Entering Wilderness").color(Color.GREEN)
            subtitleMessage = Message.raw("Unclaimed territory")
        }

        // Only show title if text changed
        val previousTitle = playerLastTitle[playerRef.uuid]
        if (titleText != previousTitle) {
            playerLastTitle[playerRef.uuid] = titleText
            EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                titleMessage,
                subtitleMessage,
                false,
                null,
                TITLE_FADE_IN_SECONDS,
                TITLE_STAY_SECONDS,
                TITLE_FADE_OUT_SECONDS
            )
        }
    }

    /**
     * Call this when a player disconnects to clean up tracking.
     */
    fun removePlayer(playerId: UUID) {
        playerLastTitle.remove(playerId)
    }

    @NullableDecl
    override fun getQuery(): Query<EntityStore> {
        return PlayerRef.getComponentType()
    }
}
