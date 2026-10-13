package it.cava99.towny3d.model

import java.util.UUID

/**
 * Unique identifier for a Nation.
 * Wraps a UUID for type safety.
 *
 * @param value The underlying UUID value
 */
@JvmInline
value class NationId(val value: UUID) {
    companion object {
        /**
         * Generates a new random NationId.
         */
        fun random() = NationId(UUID.randomUUID())

        /**
         * Creates a NationId from a string UUID.
         * @throws IllegalArgumentException if the string is not a valid UUID
         */
        fun fromString(id: String) = NationId(UUID.fromString(id))
    }

    /**
     * Returns the string representation of this NationId.
     */
    override fun toString() = value.toString()
}
