package it.cava99.towny3d.listener

import it.cava99.towny3d.service.PermissionEvaluator
import it.cava99.towny3d.service.RegionService
import it.cava99.towny3d.service.ResidentService
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

/**
 * Dependencies required by ProtectionService and its listeners.
 * This class provides a clean separation from the main plugin class.
 */
data class ProtectionDependencies(
    val pluginScope: CoroutineScope,
    val regionService: RegionService,
    val residentService: ResidentService,
    val permissionEvaluator: PermissionEvaluator,
    val spatialIndex: it.cava99.towny3d.cache.SpatialRegionIndex,
    val residentCache: it.cava99.towny3d.cache.ResidentCache,
    val townCache: it.cava99.towny3d.cache.TownCache,
    val regionCache: it.cava99.towny3d.cache.RegionCache,
    val claimStickManager: it.cava99.towny3d.item.ClaimStickManager
)

/**
 * Central protection service that coordinates all protection listeners.
 * This is the main entry point for all protection checks.
 */
class ProtectionService(
    private val deps: ProtectionDependencies
) {
    val playerInteractionListener = PlayerInteractionListener(deps)
    private val explosionProtectionListener = ExplosionProtectionListener(deps)
    private val pvpProtectionListener = PvPProtectionListener(deps)
    private val entityProtectionListener = EntityProtectionListener(deps)

    /**
     * Gets comprehensive protection info for a location.
     * Useful for debugging and displaying protection status to players.
     */
    suspend fun getProtectionInfo(world: String, x: Int, y: Int, z: Int): ProtectionInfo {
        val chunkPos = it.cava99.towny3d.model.ChunkPos(x shr 5, z shr 5)
        val regions = deps.regionService.getRegionsAt(chunkPos)

        if (regions.isEmpty()) {
            return ProtectionInfo(
                isProtected = false,
                isWilderness = true,
                regions = emptyList(),
                pvpStatus = PvPStatus.WILDERNESS,
                canExplode = true,
                canBurn = true
            )
        }

        // Check region toggle flags - use first region's toggles for protection status
        // (regions at the same location should have consistent toggle settings)
        val regionToggles = regions.first().toggles
        val pvpStatus = if (regionToggles.pvp) PvPStatus.ENABLED else PvPStatus.DISABLED
        val canExplode = regionToggles.explosion
        val canBurn = regionToggles.fire

        return ProtectionInfo(
            isProtected = true,
            isWilderness = false,
            regions = regions,
            pvpStatus = pvpStatus,
            canExplode = canExplode,
            canBurn = canBurn
        )
    }

    /**
     * Protection information for a location.
     */
    data class ProtectionInfo(
        val isProtected: Boolean,
        val isWilderness: Boolean,
        val regions: List<it.cava99.towny3d.model.Region>,
        val pvpStatus: PvPStatus,
        val canExplode: Boolean,
        val canBurn: Boolean
    )

    /**
     * PvP status at a location.
     */
    enum class PvPStatus {
        WILDERNESS,
        ENABLED,
        DISABLED
    }
}
