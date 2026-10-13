package it.cava99.towny3d.model

import java.util.UUID

/**
 * Unique identifier for a Resident.
 * Wraps a UUID for type safety.
 *
 * @param value The underlying UUID value
 */
@JvmInline
value class ResidentId(val value: UUID) {
    companion object {
        /**
         * Generates a new random ResidentId.
         */
        fun random() = ResidentId(UUID.randomUUID())

        /**
         * Creates a ResidentId from a string UUID.
         * @throws IllegalArgumentException if the string is not a valid UUID
         */
        fun fromString(id: String) = ResidentId(UUID.fromString(id))

        /**
         * Creates a ResidentId from a player UUID.
         */
        fun fromPlayerUuid(uuid: UUID) = ResidentId(uuid)
    }

    /**
     * Returns the string representation of this ResidentId.
     */
    override fun toString() = value.toString()
}
