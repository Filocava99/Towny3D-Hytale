package it.cava99.towny3d.model

/**
 * Constants for chunk-related calculations.
 * Hytale chunks are 32x32 blocks (vs 16x16 in Minecraft).
 */
object ChunkConstants {
    /** The size of a chunk in blocks (32x32) */
    const val CHUNK_SIZE = 32
    /** Half the chunk size, used for center calculations */
    const val CHUNK_HALF_SIZE = 16
    /** Number of bits to right-shift to get chunk X/Z from block X/Z */
    const val CHUNK_SIZE_BITS = 5
}

/**
 * Represents the position of a chunk in a world.
 * Hytale chunks are 32x32 blocks (vs 16x16 in Minecraft).
 *
 * @property x The chunk X coordinate
 * @property z The chunk Z coordinate
 */
@JvmInline
value class ChunkPos(val packed: Long) {
    /**
     * Creates a ChunkPos from x and z coordinates.
     */
    constructor(x: Int, z: Int) : this(pack(x, z))

    val x: Int get() = unpackX(packed)
    val z: Int get() = unpackZ(packed)

    /**
     * Gets the block X coordinate of the chunk's center.
     */
    val centerX: Double get() = x * ChunkConstants.CHUNK_SIZE + ChunkConstants.CHUNK_HALF_SIZE.toDouble()

    /**
     * Gets the block Z coordinate of the chunk's center.
     */
    val centerZ: Double get() = z * ChunkConstants.CHUNK_SIZE + ChunkConstants.CHUNK_HALF_SIZE.toDouble()

    /**
     * Returns a string representation of this chunk position.
     */
    override fun toString(): String = "ChunkPos(x=$x, z=$z)"

    /**
     * Calculates the distance squared to another chunk position.
     */
    fun distanceSquaredTo(other: ChunkPos): Long {
        val dx = (x - other.x).toLong()
        val dz = (z - other.z).toLong()
        return dx * dx + dz * dz
    }

    companion object {
        /**
         * Packs x and z coordinates into a single long.
         * Each coordinate uses 32 bits.
         */
        fun pack(x: Int, z: Int): Long {
            return (x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)
        }

        /**
         * Unpacks the x coordinate from a packed long.
         */
        fun unpackX(packed: Long): Int = (packed shr 32).toInt()

        /**
         * Unpacks the z coordinate from a packed long.
         */
        fun unpackZ(packed: Long): Int = (packed and 0xFFFFFFFFL).toInt()

        /**
         * Creates a ChunkPos from block coordinates.
         * @param blockX The block X coordinate
         * @param blockZ The block Z coordinate
         * @return The ChunkPos containing the block
         */
        fun fromBlock(blockX: Int, blockZ: Int): ChunkPos {
            val chunkX = blockX shr ChunkConstants.CHUNK_SIZE_BITS
            val chunkZ = blockZ shr ChunkConstants.CHUNK_SIZE_BITS
            return ChunkPos(chunkX, chunkZ)
        }
    }
}
