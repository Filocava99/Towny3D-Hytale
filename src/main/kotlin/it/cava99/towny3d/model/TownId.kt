package it.cava99.towny3d.model

import java.util.UUID

/**
 * Unique identifier for a Town.
 * Wraps a UUID for type safety.
 *
 * @param value The underlying UUID value
 */
@JvmInline
value class TownId(val value: UUID) {
    companion object {
        /**
         * Generates a new random TownId.
         */
        fun random() = TownId(UUID.randomUUID())

        /**
         * Creates a TownId from a string UUID.
         * @throws IllegalArgumentException if the string is not a valid UUID
         */
        fun fromString(id: String) = TownId(UUID.fromString(id))
    }

    /**
     * Returns the string representation of this TownId.
     */
    override fun toString() = value.toString()
}
