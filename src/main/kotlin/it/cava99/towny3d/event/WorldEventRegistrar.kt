package it.cava99.towny3d.event

import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent
import com.hypixel.hytale.server.core.universe.PlayerRef
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.PermissionGroup
import it.cava99.towny3d.model.PermissionType
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.util.TownyLog
import java.util.UUID

/**
 * Registers world/block-related event handlers with Hytale's EventBus.
 *
 * Handles:
 * - Block break events (with permission checks)
 * - Block place events (with permission checks)
 */
class WorldEventRegistrar(
    private val plugin: Towny3D
) {
    private val eventRegistry = plugin.getEventRegistry()

    /**
     * Registers all world/block event handlers.
     */
    fun registerAll() {
        registerBlockEvents()
        TownyLog.info("Registered world events")
    }

    /**
     * Registers block-related events (break, place).
     */
    private fun registerBlockEvents() {
        // Block break event
        eventRegistry.register(BreakBlockEvent::class.java) { event ->
            try {
                // Try to get player UUID from event
                val playerUuid = try {
                    val method = event.javaClass.methods.find { it.name == "getPlayerRef" || it.name == "getPlayer" }
                    method?.invoke(event)
                } catch (e: Exception) {
                    null
                }

                if (playerUuid == null) {
                    return@register
                }

                val uuid = when (playerUuid) {
                    is UUID -> playerUuid
                    is PlayerRef -> playerUuid.uuid
                    else -> null
                }

                if (uuid == null) {
                    return@register
                }

                val targetBlock = event.getTargetBlock()
                val x = targetBlock.x
                val y = targetBlock.y
                val z = targetBlock.z

                // Synchronous permission check using cache
                val canDestroy = checkBlockPermissionSync(uuid, x, y, z, PermissionType.DESTROY)

                if (!canDestroy) {
                    event.setCancelled(true)
                    TownyLog.debug("Block break cancelled at ($x, $y, $z) - no permission")
                }
            } catch (e: Exception) {
                TownyLog.warn("Error handling block break event: ${e.message}")
            }
        }

        // Block place event
        eventRegistry.register(PlaceBlockEvent::class.java) { event ->
            try {
                val playerUuid = try {
                    val method = event.javaClass.methods.find { it.name == "getPlayerRef" || it.name == "getPlayer" }
                    method?.invoke(event)
                } catch (e: Exception) {
                    null
                }

                if (playerUuid == null) {
                    return@register
                }

                val uuid = when (playerUuid) {
                    is UUID -> playerUuid
                    is PlayerRef -> playerUuid.uuid
                    else -> null
                }

                if (uuid == null) {
                    return@register
                }

                val targetBlock = event.getTargetBlock()
                val x = targetBlock.x
                val y = targetBlock.y
                val z = targetBlock.z

                // Synchronous permission check using cache
                val canBuild = checkBlockPermissionSync(uuid, x, y, z, PermissionType.BUILD)

                if (!canBuild) {
                    event.setCancelled(true)
                    TownyLog.debug("Block place cancelled at ($x, $y, $z) - no permission")
                }
            } catch (e: Exception) {
                TownyLog.warn("Error handling block place event: ${e.message}")
            }
        }

        TownyLog.info("Registered block events")
    }

    /**
     * Synchronous permission check using cache (for event handlers).
     * Returns true if action is allowed, false if denied.
     */
    private fun checkBlockPermissionSync(
        playerUuid: UUID,
        x: Int,
        y: Int,
        z: Int,
        permissionType: PermissionType
    ): Boolean {
        val chunkPos = ChunkPos(x shr 5, z shr 5)
        val region = plugin.regionService.getRegionAtSync(chunkPos, y)

        if (region == null) {
            return true // Wilderness - always allowed
        }

        // Get resident from cache synchronously
        val residentId = ResidentId.fromPlayerUuid(playerUuid)
        val resident = plugin.residentCache.getSync(residentId)

        if (resident == null) {
            // No resident record - deny for safety
            return false
        }

        // Check if player owns this region
        if (region.ownerId == resident.id) {
            return true // Region owner can do anything
        }

        val residentTownId = resident.townId

        // Check if player is in the owning town
        val town = if (region.townId == residentTownId && residentTownId != null) {
            plugin.townCache.getSync(region.townId)
        } else {
            null
        }

        if (town != null) {
            // Player is in the town that owns this region
            val isAssistant = town.isAssistant(resident.id)
            val isMayor = town.isMayor(resident.id)

            if (isMayor || isAssistant) {
                return true // Mayor/assistants can do anything
            }

            // Check town permissions for residents
            return region.permissions.has(PermissionGroup.RESIDENT, permissionType)
        }

        // Check if player is in an allied nation
        if (residentTownId != null && town?.nationId != null) {
            val residentTown = plugin.townCache.getSync(residentTownId)
            if (residentTown?.nationId == town.nationId && residentTown.nationId != null) {
                // Same nation - check ally permissions
                return region.permissions.has(PermissionGroup.ALLY, permissionType)
            }
        }

        // Not owner, not in town, not ally - check general permissions
        return region.permissions.has(PermissionGroup.OUTSIDER, permissionType)
    }
}
