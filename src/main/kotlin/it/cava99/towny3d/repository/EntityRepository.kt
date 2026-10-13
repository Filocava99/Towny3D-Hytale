package it.cava99.towny3d.repository

/**
 * Base repository interface for entity persistence operations.
 * Provides a clean abstraction layer over storage, following the Repository pattern.
 *
 * @param ID The type of the entity's identifier
 * @param T The type of the entity
 */
interface EntityRepository<ID, T> {
    /**
     * Finds an entity by its identifier.
     * @param id The entity identifier
     * @return The entity, or null if not found
     */
    suspend fun findById(id: ID): T?

    /**
     * Saves an entity to storage.
     * @param entity The entity to save
     */
    suspend fun save(entity: T)

    /**
     * Deletes an entity by its identifier.
     * @param id The entity identifier
     */
    suspend fun delete(id: ID)

    /**
     * Loads all entities of this type.
     * @return List of all entities (empty list if none exist)
     */
    suspend fun findAll(): List<T>
}
