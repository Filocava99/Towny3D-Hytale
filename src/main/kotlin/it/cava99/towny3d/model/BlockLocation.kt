package it.cava99.towny3d.model

/**
 * Represents a 3D location in a world.
 *
 * @property world The world name
 * @property x The X coordinate
 * @property y The Y coordinate (range: -512 to 512 in Hytale)
 * @property z The Z coordinate
 */
data class BlockLocation(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int
) {
    /**
     * Gets the chunk position containing this block.
     */
    val chunkPos: ChunkPos
        get() = ChunkPos.fromBlock(x, z)

    /**
     * Returns a string representation of this location.
     */
    override fun toString(): String = "BlockLocation(world=$world, x=$x, y=$y, z=$z)"

    /**
     * Calculates the distance squared to another location.
     */
    fun distanceSquaredTo(other: BlockLocation): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return dx * dx + dy * dy + dz * dz
    }

    /**
     * Calculates the distance to another location.
     */
    fun distanceTo(other: BlockLocation): Double {
        return kotlin.math.sqrt(distanceSquaredTo(other))
    }

    companion object {
        /**
         * Creates a BlockLocation from a packed long (similar to Hytale's BlockUtil).
         * Note: This is a simplified version. Hytale's actual packing includes world info.
         */
        fun fromPacked(packed: Long, world: String): BlockLocation {
            val x = (packed shr 42).toInt()
            val y = ((packed shr 20) and 0xFFFFF).toInt() - 512 // Shift to allow negative
            val z = ((packed shl 20) shr 42).toInt()
            return BlockLocation(world, x, y, z)
        }

        /**
         * Packs this location into a long (simplified version).
         */
        fun toPacked(x: Int, y: Int, z: Int): Long {
            val packedX = (x.toLong() and 0x3FFFFF) shl 42
            val packedY = ((y + 512).toLong() and 0xFFFFF) shl 20
            val packedZ = (z.toLong() and 0x3FFFFF)
            return packedX or packedY or packedZ
        }
    }
}
