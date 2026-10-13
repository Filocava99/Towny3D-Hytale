package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.TownyLog

/**
 * Utility functions for command handling.
 */
object CommandUtils {
    /**
     * Helper to get player UUID from Ref.
     */
    fun getPlayerUuid(ref: Ref<EntityStore>, store: Store<EntityStore>): java.util.UUID? {
        val uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType()) as? UUIDComponent
        return uuidComponent?.getUuid()
    }

    /**
     * Helper to get player position from Ref.
     */
    fun getPlayerPosition(ref: Ref<EntityStore>, store: Store<EntityStore>): org.joml.Vector3d? {
        val transformComponent = store.getComponent(ref, TransformComponent.getComponentType()) as? TransformComponent
        return transformComponent?.transform?.position
    }

    /**
     * Gets or creates a resident by UUID.
     * Returns null only if creation fails, never returns null for valid players.
     */
    suspend fun getOrCreateResident(plugin: Towny3D, playerUuid: java.util.UUID, playerName: String? = null): it.cava99.towny3d.model.Resident? {
        // Try cache first
        val cached = plugin.residentService.getResidentByUuid(playerUuid)
        if (cached != null) return cached

        // Not in cache, load or create
        val name = playerName ?: "Unknown"
        return try {
            plugin.residentService.loadOrCreate(playerUuid, name)
        } catch (e: Exception) {
            TownyLog.error("Failed to load/create resident: ${e.message}", e)
            null
        }
    }
}
