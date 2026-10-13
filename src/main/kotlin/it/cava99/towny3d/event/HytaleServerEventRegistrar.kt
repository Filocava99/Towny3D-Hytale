package it.cava99.towny3d.event

import it.cava99.towny3d.util.TownyLog

/**
 * Registers events directly to the HytaleServer event bus.
 *
 * NOTE: ClaimStickSystem has been refactored to use PlayerMouseButtonEvent
 * registered via the plugin's getEventRegistry() in Towny3D.setup().
 *
 * This registrar is a placeholder for future server-level event registrations
 * that may be needed for Hytale-specific server events not available through
 * the standard plugin event registry.
 *
 * Currently no server-level events are registered. This class is retained
 * for future extensibility when Hytale adds more server-level event hooks.
 */
object HytaleServerEventRegistrar {

    fun registerAllEvents() {
        // Server-level events can be registered here in the future
        TownyLog.info("HytaleServerEventRegistrar initialized (no server-level events registered)")
    }
}
