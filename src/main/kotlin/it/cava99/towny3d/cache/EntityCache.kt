package it.cava99.towny3d.cache

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import it.cava99.towny3d.repository.EntityRepository
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Base class for entity caches using Caffeine.
 *
 * Provides common caching functionality for all entity types:
 * - Async loading from repository
 * - Configurable expiration policies
 * - NULL sentinel pattern for negative caching
 * - Thread-safe operations
 *
 * @param ID The type of the entity's identifier
 * @param T The type of the entity
 * @param repository The repository for loading entities
 * @param config The cache configuration
 * @param executor The executor for async operations
 * @param nullSentinel The sentinel value to represent null
 */
abstract class EntityCache<ID : Any, T : Any>(
    private val repository: EntityRepository<ID, T>,
    private val config: CacheConfig,
    executor: Executor,
    private val nullSentinel: T
) {
    protected val cache: AsyncCache<ID, T>

    init {
        cache = Caffeine.newBuilder()
            .maximumSize(config.maxSize)
            .apply {
                if (config.expireAfterAccess != null) {
                    expireAfterAccess(config.expireAfterAccess)
                }
                if (config.expireAfterWrite != null) {
                    expireAfterWrite(config.expireAfterWrite)
                }
                if (config.refreshAfterWrite != null) {
                    refreshAfterWrite(config.refreshAfterWrite)
                }
            }
            .executor(executor)
            .buildAsync { id, _ ->
                CompletableFuture.supplyAsync { loadBlocking(id) }
            }
    }

    /**
     * Blocking load for Caffeine cache loader.
     * Bridges suspend repository functions to non-suspend context.
     */
    private fun loadBlocking(id: ID): T {
        return kotlinx.coroutines.runBlocking {
            try {
                repository.findById(id) ?: nullSentinel
            } catch (e: Exception) {
                TownyLog.error("Failed to load entity $id: ${e.message}", e)
                nullSentinel
            }
        }
    }

    /**
     * Gets an entity from cache asynchronously.
     * @param id The entity identifier
     * @return The entity, or null if not found
     */
    suspend fun get(id: ID): T? {
        val future = cache.getIfPresent(id) ?: return null
        val entity = future.await()
        return if (entity === nullSentinel) null else entity
    }

    /**
     * Gets an entity from cache synchronously.
     * @param id The entity identifier
     * @return The entity, or null if not found
     */
    fun getSync(id: ID): T? {
        val future = cache.getIfPresent(id) ?: return null
        val entity = future.getNow(nullSentinel)
        return if (entity === nullSentinel) null else entity
    }

    /**
     * Puts an entity into the cache.
     * @param entity The entity to cache
     */
    open fun put(entity: T) {
        cache.put(getEntityId(entity), CompletableFuture.completedFuture(entity))
    }

    /**
     * Puts multiple entities into the cache.
     * @param entities The entities to cache
     */
    open fun putAll(entities: Collection<T>) {
        entities.forEach { put(it) }
    }

    /**
     * Invalidates an entity from the cache.
     * @param id The entity identifier
     */
    open suspend fun invalidate(id: ID) {
        cache.synchronous().invalidate(id)
    }

    /**
     * Invalidates all entities from the cache.
     */
    open fun invalidateAll() {
        cache.synchronous().invalidateAll()
    }

    /**
     * Gets the estimated size of the cache.
     */
    val size: Long
        get() = cache.synchronous().estimatedSize()

    /**
     * Gets all entities from repository and populates cache.
     * @return List of all entities
     */
    suspend fun getAll(): List<T> {
        return repository.findAll().also { putAll(it) }
    }

    /**
     * Gets all entities currently in the in-memory cache (without hitting repository).
     * Useful for rebuilding indexes from the most up-to-date in-memory state,
     * especially when batch writes haven't flushed to disk yet.
     * @return List of all cached entities (excludes null sentinels)
     */
    fun getAllCached(): List<T> {
        return cache.asMap().values.mapNotNull { future ->
            val entity = future.getNow(null)
            if (entity != null && entity !== nullSentinel) entity else null
        }
    }

    /**
     * Warms up the cache by loading all entities from repository.
     */
    suspend fun warmUp() {
        putAll(repository.findAll())
    }

    /**
     * Extracts the entity ID from the entity instance.
     * Subclasses must implement this to support put operations.
     * @param entity The entity instance
     * @return The entity's identifier
     */
    protected abstract fun getEntityId(entity: T): ID
}

/**
 * Configuration for entity caches.
 *
 * @param maxSize Maximum number of entries in the cache
 * @param expireAfterAccess Time to expire after last access (null for no expiration)
 * @param expireAfterWrite Time to expire after write (null for no expiration)
 * @param refreshAfterWrite Time to refresh after write (null for no refresh)
 */
data class CacheConfig(
    val maxSize: Long,
    val expireAfterAccess: Duration? = null,
    val expireAfterWrite: Duration? = null,
    val refreshAfterWrite: Duration? = null
)
