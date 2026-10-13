package it.cava99.towny3d.command

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.util.MessageFormatter
import java.util.UUID

/**
 * Helper object for validating and extracting player context in commands.
 * Provides centralized validation logic with consistent error messaging.
 */
object CommandValidation {
    /**
     * Validates that the command sender is a player and extracts player context.
     * Returns null if validation fails, sending appropriate error message to sender.
     *
     * @param context The command context
     * @param store The entity store
     * @return PlayerContext if validation succeeds, null otherwise
     */
    fun validatePlayer(
        context: CommandContext,
        store: Store<EntityStore>
    ): PlayerContext? {
        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use this command"))
            return null
        }

        val playerRef = context.senderAsPlayerRef()
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference"))
            return null
        }

        val uuid = CommandUtils.getPlayerUuid(playerRef, store)
        val position = CommandUtils.getPlayerPosition(playerRef, store)

        if (uuid == null || position == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return null
        }

        return PlayerContext(uuid, position, playerRef)
    }

    /**
     * Validates that the command sender is a player and extracts basic player info.
     * Use this when you don't need the player's position.
     *
     * @param context The command context
     * @param store The entity store
     * @return BasicPlayerContext if validation succeeds, null otherwise
     */
    fun validatePlayerBasic(
        context: CommandContext,
        store: Store<EntityStore>
    ): BasicPlayerContext? {
        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can use this command"))
            return null
        }

        val playerRef = context.senderAsPlayerRef()
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference"))
            return null
        }

        val uuid = CommandUtils.getPlayerUuid(playerRef, store)
        if (uuid == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return null
        }

        return BasicPlayerContext(uuid, playerRef)
    }
}

/**
 * Player context with UUID, position, and entity reference.
 * Used by commands that need all player information.
 */
data class PlayerContext(
    val uuid: UUID,
    val position: org.joml.Vector3d,
    val ref: Ref<EntityStore>
)

/**
 * Basic player context with UUID and entity reference (no position).
 * Used by commands that don't need positional data.
 */
data class BasicPlayerContext(
    val uuid: UUID,
    val ref: Ref<EntityStore>
)
