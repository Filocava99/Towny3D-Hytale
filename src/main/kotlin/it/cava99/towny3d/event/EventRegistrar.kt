package it.cava99.towny3d.event

import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.util.TownyLog

/**
 * Orchestrates registration of all Towny3D event handlers with Hytale's EventBus.
 *
 * Delegates to specialized registrars:
 * - [PlayerEventRegistrar] for player connect/disconnect and chat events
 * - [WorldEventRegistrar] for block break/place events
 */
class EventRegistrar(
    private val plugin: Towny3D
) {
    private val playerEventRegistrar = PlayerEventRegistrar(plugin)
    private val worldEventRegistrar = WorldEventRegistrar(plugin)

    /**
     * Registers all event handlers.
     */
    fun registerAllEvents() {
        try {
            worldEventRegistrar.registerAll()
            playerEventRegistrar.registerAll()
            TownyLog.info("Successfully registered all event listeners")
        } catch (e: Exception) {
            TownyLog.error("Failed to register event listeners: ${e.message}", e)
        }
    }
}
