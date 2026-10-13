package it.cava99.towny3d.listener

import it.cava99.towny3d.model.BlockLocation
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Event listener for player block interactions.
 * Handles claim stick selection and protection checks.
 */
class PlayerInteractionListener(
    private val deps: ProtectionDependencies
) {

    /**
     * Handles left-click on a block (primary selection).
     * @deprecated This method is based on incorrect assumptions about Hytale's input system.
     *             Use ClaimStickSystem with UseBlockEvent.Pre instead.
     *             Hytale uses InteractionType.Primary (left-click) and InteractionType.Use (F key).
     */
    @Deprecated(
        "Use ClaimStickSystem with UseBlockEvent.Pre instead",
        ReplaceWith("ClaimStickSystem")
    )
    fun onBlockLeftClick(
        playerUuid: UUID,
        playerName: String,
        world: String,
        x: Int,
        y: Int,
        z: Int,
        itemStack: Any?
    ) {
        // Check if holding claim stick
        if (isClaimStick(itemStack)) {
            deps.pluginScope.launch {
                try {
                    val location = BlockLocation(world, x, y, z)
                    deps.claimStickManager.onLeftClick(playerUuid, playerName, location)
                } catch (e: Exception) {
                    TownyLog.warn("Failed to handle claim stick left click: ${e.message}")
                }
            }
            return // Don't process protection for claim stick
        }

        // Check protection
        val location = BlockLocation(world, x, y, z)
        val allowed = checkProtection(playerUuid, location, it.cava99.towny3d.model.PermissionType.BUILD)

        if (!allowed) {
            // Cancel the event
            // TODO(2026-02-22): Send feedback to player
            TownyLog.info("Blocked $playerName from breaking block at ($x, $y, $z)")
        }
    }

    /**
     * Handles right-click on a block (secondary selection).
     * @deprecated This method is based on incorrect assumptions about Hytale's input system.
     *             Use ClaimStickSystem with UseBlockEvent.Pre instead.
     *             Hytale uses InteractionType.Primary (left-click) and InteractionType.Use (F key).
     */
    @Deprecated(
        "Use ClaimStickSystem with UseBlockEvent.Pre instead",
        ReplaceWith("ClaimStickSystem")
    )
    fun onBlockRightClick(
        playerUuid: UUID,
        playerName: String,
        world: String,
        x: Int,
        y: Int,
        z: Int,
        itemStack: Any?
    ) {
        // Check if holding claim stick
        if (isClaimStick(itemStack)) {
            deps.pluginScope.launch {
                try {
                    val location = BlockLocation(world, x, y, z)
                    deps.claimStickManager.onRightClick(playerUuid, playerName, location)
                } catch (e: Exception) {
                    TownyLog.warn("Failed to handle claim stick F click: ${e.message}")
                }
            }
            return // Don't process protection for claim stick
        }

        // Check protection
        val location = BlockLocation(world, x, y, z)
        val allowed = checkProtection(playerUuid, location, it.cava99.towny3d.model.PermissionType.BUILD)

        if (!allowed) {
            // Cancel the event
            // TODO(2026-02-22): Send feedback to player
            TownyLog.info("Blocked $playerName from placing block at ($x, $y, $z)")
        }
    }

    /**
     * Handles player interaction with an entity (e.g., animals, NPCs).
     */
    fun onEntityInteract(
        playerUuid: UUID,
        playerName: String,
        world: String,
        x: Int,
        y: Int,
        z: Int,
        entityType: String
    ) {
        val location = BlockLocation(world, x, y, z)
        val permissionType = when {
            entityType.contains("villager", ignoreCase = true) ||
            entityType.contains("npc", ignoreCase = true) -> it.cava99.towny3d.model.PermissionType.INTERACT
            entityType.contains("animal", ignoreCase = true) ||
            entityType.contains("monster", ignoreCase = true) -> it.cava99.towny3d.model.PermissionType.INTERACT
            else -> it.cava99.towny3d.model.PermissionType.INTERACT
        }

        val allowed = checkProtection(playerUuid, location, permissionType)

        if (!allowed) {
            // Cancel the event
            TownyLog.info("Blocked $playerName from interacting with $entityType at ($x, $y, $z)")
        }
    }

    /**
     * Handles player breaking a block.
     */
    fun onBlockBreak(
        playerUuid: UUID,
        playerName: String,
        world: String,
        x: Int,
        y: Int,
        z: Int
    ): Boolean {
        val location = BlockLocation(world, x, y, z)
        val allowed = checkProtection(playerUuid, location, it.cava99.towny3d.model.PermissionType.BUILD)

        if (!allowed) {
            TownyLog.info("Blocked $playerName from breaking block at ($x, $y, $z)")
        }

        return allowed
    }

    /**
     * Handles player placing a block.
     */
    fun onBlockPlace(
        playerUuid: UUID,
        playerName: String,
        world: String,
        x: Int,
        y: Int,
        z: Int
    ): Boolean {
        val location = BlockLocation(world, x, y, z)
        val allowed = checkProtection(playerUuid, location, it.cava99.towny3d.model.PermissionType.BUILD)

        if (!allowed) {
            TownyLog.info("Blocked $playerName from placing block at ($x, $y, $z)")
        }

        return allowed
    }

    /**
     * Checks if the player has permission to perform an action at a location.
     */
    private fun checkProtection(
        playerUuid: UUID,
        location: BlockLocation,
        permissionType: it.cava99.towny3d.model.PermissionType
    ): Boolean {
        // Get the resident from cache (synchronous)
        val residentId = it.cava99.towny3d.model.ResidentId.fromPlayerUuid(playerUuid)
        val resident = deps.residentCache.getSync(residentId)
            ?: return false // Player not found

        val chunkPos = ChunkPos(location.x shr 5, location.z shr 5)

        // No regions = wilderness - for now allow everything
        // TODO(2026-02-22): Implement proper region checking when getRegionsAt is available
        return true
    }

    /**
     * Checks if the item stack is a claim stick.
     */
    private fun isClaimStick(itemStack: Any?): Boolean {
        if (itemStack == null) return false

        return try {
            val itemClass = itemStack.javaClass
            val item = itemClass.getMethod("getItem").invoke(itemStack)
            val itemClass2 = item.javaClass
            val itemId = itemClass2.getMethod("getIdentifier").invoke(item) as? String

            // TODO(2026-02-22): Make this configurable
            itemId == "minecraft:stick" || itemId == "hytale:claim_stick"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Handles player disconnect.
     */
    fun onPlayerDisconnect(playerUuid: UUID) {
        deps.claimStickManager.clearVisualization(playerUuid)
    }

    /**
     * Handles player join.
     */
    fun onPlayerJoin(playerUuid: UUID, playerName: String) {
        deps.pluginScope.launch {
            try {
                // Load or create resident
                deps.residentService.loadOrCreate(playerUuid, playerName)
            } catch (e: Exception) {
                TownyLog.warn("Failed to load resident $playerName: ${e.message}")
            }
        }
    }
}
