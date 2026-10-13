package it.cava99.towny3d.util

/**
 * Time-related constants for the Towny3D plugin.
 * All values are in milliseconds unless otherwise specified.
 */
object TimeConstants {
    /** Milliseconds per second */
    const val MS_PER_SECOND = 1000L

    /** Milliseconds per minute */
    const val MS_PER_MINUTE = 60_000L

    /** Milliseconds per hour */
    const val MS_PER_HOUR = 3_600_000L

    /** Interval for plot border particle updates */
    const val PLOT_BORDER_UPDATE_INTERVAL_MS = 1000L

    /** Interval for claim stick visualization updates */
    const val CLAIM_STICK_UPDATE_INTERVAL_MS = 500L

    /** Default flush interval for batch storage writer */
    const val DEFAULT_FLUSH_INTERVAL_MS = 5000L

    /** Default max batch size for storage operations */
    const val DEFAULT_MAX_BATCH_SIZE = 100

    /** Deduplication window for claim stick packet processing */
    const val PACKET_DEDUPE_MS = 200L
}
