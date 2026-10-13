package it.cava99.towny3d.map

/**
 * Map-related constants for the Towny3D world map rendering.
 */
object MapConstants {
    /** Default scale for the world map */
    const val MAP_DEFAULT_SCALE = 128.0f

    /** Minimum scale for the world map */
    const val MAP_MIN_SCALE = 64.0f

    /** Maximum scale for the world map */
    const val MAP_MAX_SCALE = 128.0f

    /** Border size in pixels for map rendering */
    const val MAP_BORDER_SIZE_PIXELS = 2

    /** Zoom factor for map scaling */
    const val MAP_ZOOM_FACTOR = 3.0f

    /** Chunk size used in map rendering */
    const val MAP_CHUNK_SIZE = 16

    /** Image width for chunk map generation */
    const val MAP_IMAGE_WIDTH = 32

    /** Image height for chunk map generation */
    const val MAP_IMAGE_HEIGHT = 32

    /** Scale divisor for world map settings */
    const val MAP_SCALE_DIVISOR = 1.0f
}
