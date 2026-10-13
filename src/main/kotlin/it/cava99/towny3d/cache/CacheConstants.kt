package it.cava99.towny3d.cache

import java.time.Duration

/**
 * Cache-related constants for the Towny3D plugin.
 */
object CacheConstants {
    // Thread pool sizes
    /** Default thread pool size for cache operations */
    const val DEFAULT_CACHE_THREAD_POOL_SIZE = 4

    /** Thread pool size for TownCache */
    const val TOWN_CACHE_THREAD_POOL_SIZE = 2

    /** Thread pool size for NationCache (single thread) */
    const val NATION_CACHE_THREAD_POOL_SIZE = 1

    /** Thread pool size for RegionCache */
    const val REGION_CACHE_THREAD_POOL_SIZE = 4

    /** Thread pool size for ResidentCache */
    const val RESIDENT_CACHE_THREAD_POOL_SIZE = 4

    // Cache max sizes
    /** Maximum entries for TownCache */
    const val TOWN_CACHE_MAX_SIZE = 1_000L

    /** Maximum entries for NationCache */
    const val NATION_CACHE_MAX_SIZE = 100L

    /** Maximum entries for RegionCache */
    const val REGION_CACHE_MAX_SIZE = 10_000L

    /** Maximum entries for ResidentCache */
    const val RESIDENT_CACHE_MAX_SIZE = 10_000L

    // Cache expiry durations
    /** Town cache expiry after access (30 minutes) */
    val TOWN_CACHE_EXPIRY_AFTER_ACCESS: Duration = Duration.ofMinutes(30)

    /** Nation cache expiry after write (1 hour) */
    val NATION_CACHE_EXPIRY_AFTER_WRITE: Duration = Duration.ofHours(1)

    /** Nation cache refresh after write (15 minutes) */
    val NATION_CACHE_REFRESH_AFTER_WRITE: Duration = Duration.ofMinutes(15)

    /** Region cache expiry after access (10 minutes) */
    val REGION_CACHE_EXPIRY_AFTER_ACCESS: Duration = Duration.ofMinutes(10)

    /** Region cache refresh after write (5 minutes) */
    val REGION_CACHE_REFRESH_AFTER_WRITE: Duration = Duration.ofMinutes(5)

    /** Resident cache expiry after access (15 minutes) */
    val RESIDENT_CACHE_EXPIRY_AFTER_ACCESS: Duration = Duration.ofMinutes(15)

    // Permission cache
    /** Maximum entries for permission cache */
    const val PERMISSION_CACHE_MAX_SIZE = 10_000L

    /** Permission cache expiry in minutes */
    const val PERMISSION_CACHE_EXPIRY_MINUTES = 1L
}
