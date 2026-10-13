package it.cava99.towny3d

import it.cava99.towny3d.storage.StorageBackend
import it.cava99.towny3d.storage.BatchStorageWriter
import it.cava99.towny3d.storage.JsonStorageBackend
import kotlinx.coroutines.CoroutineScope
import it.cava99.towny3d.config.TownyConfig
import java.nio.file.Path

/**
 * Manages storage backend and batch writer for the plugin.
 * Handles initialization, shutdown, and data persistence operations.
 */
class StorageManager(
    private val dataDirectory: Path,
    private val pluginScope: CoroutineScope,
    private val config: TownyConfig
) {
    private lateinit var _storageBackend: StorageBackend
    private lateinit var _batchWriter: BatchStorageWriter

    /**
     * Initializes the storage backend and starts the batch writer.
     */
    suspend fun initialize() {
        val storageDir = dataDirectory.resolve("data")
        java.nio.file.Files.createDirectories(storageDir)
        _storageBackend = JsonStorageBackend(storageDir, config = config)
        _batchWriter = BatchStorageWriter(_storageBackend, pluginScope)
        _batchWriter.start()
    }

    /**
     * Shuts down the storage manager, flushing any pending writes and stopping the batch writer.
     */
    suspend fun shutdown() {
        flush()
        _batchWriter.stop()
    }

    /**
     * Flushes any pending writes in the batch writer.
     */
    suspend fun flush() = _batchWriter.flush()

    /**
     * Returns the storage backend.
     */
    fun getStorageBackend(): StorageBackend = _storageBackend

    /**
     * Returns the batch writer.
     */
    fun getBatchWriter(): BatchStorageWriter = _batchWriter
}
