package it.cava99.towny3d.model

import java.util.UUID

/**
 * Unique identifier for a Region.
 * Wraps a UUID for type safety.
 *
 * @param value The underlying UUID value
 */
@JvmInline
value class RegionId(val value: UUID) {
    companion object {
        /**
         * Generates a new random RegionId.
         */
        fun random() = RegionId(UUID.randomUUID())

        /**
         * Creates a RegionId from a string UUID.
         * @throws IllegalArgumentException if the string is not a valid UUID
         */
        fun fromString(id: String) = RegionId(UUID.fromString(id))
    }

    /**
     * Returns the string representation of this RegionId.
     */
    override fun toString() = value.toString()
}
