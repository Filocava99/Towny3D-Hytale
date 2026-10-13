package it.cava99.towny3d.storage

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import it.cava99.towny3d.exception.StorageException
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Utility object for generic JSON file I/O operations.
 * Eliminates DRY violations by providing reusable load/save functions.
 */
object StorageIo {
    /**
     * Loads a JSON file and deserializes it to the specified type.
     *
     * @param dir The directory containing the JSON file
     * @param id The file identifier (without .json extension)
     * @param gson The Gson instance for deserialization
     * @param clazz The target class type
     * @return The deserialized object, or null if the file doesn't exist
     * @throws StorageException if parsing or I/O fails
     */
    suspend fun <T> loadJson(
        dir: Path,
        id: String,
        gson: Gson,
        clazz: Class<T>
    ): T? {
        val file = dir.resolve("$id.json")
        if (!file.exists()) return null
        return withContext(Dispatchers.IO) {
            try {
                gson.fromJson(file.readText(), clazz)
            } catch (e: JsonSyntaxException) {
                throw StorageException("Failed to parse ${clazz.simpleName} data for $id", e)
            } catch (e: IOException) {
                throw StorageException("Failed to read ${clazz.simpleName} file for $id", e)
            }
        }
    }

    /**
     * Loads all JSON files in a directory and deserializes them to the specified type.
     * Files that fail to parse are logged and skipped.
     *
     * @param dir The directory containing the JSON files
     * @param gson The Gson instance for deserialization
     * @param clazz The target class type
     * @return A list of deserialized objects
     * @throws StorageException if listing the directory fails
     */
    suspend fun <T> loadAllJson(
        dir: Path,
        gson: Gson,
        clazz: Class<T>
    ): List<T> {
        if (!dir.exists()) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                Files.list(dir)
                    .filter { it.toString().endsWith(".json") }
                    .collect(Collectors.toList())
                    .mapNotNull { file ->
                        try {
                            gson.fromJson(file.readText(), clazz)
                        } catch (e: Exception) {
                            TownyLog.warn("Failed to load ${clazz.simpleName} from ${file.fileName}: ${e.message}")
                            null
                        }
                    }
            } catch (e: IOException) {
                throw StorageException("Failed to list files in directory: $dir", e)
            }
        }
    }

    /**
     * Serializes an object to JSON and writes it to a file.
     *
     * @param dir The directory to write the file to
     * @param id The file identifier (without .json extension)
     * @param entity The object to serialize
     * @param gson The Gson instance for serialization
     * @throws StorageException if I/O fails
     */
    suspend fun <T> saveJson(
        dir: Path,
        id: String,
        entity: T,
        gson: Gson
    ) {
        val file = dir.resolve("$id.json")
        withContext(Dispatchers.IO) {
            try {
                dir.createDirectories()
                file.writeText(gson.toJson(entity))
            } catch (e: IOException) {
                throw StorageException("Failed to write file for $id", e)
            }
        }
    }

    /**
     * Deletes a JSON file if it exists.
     *
     * @param dir The directory containing the file
     * @param id The file identifier (without .json extension)
     * @throws StorageException if deletion fails
     */
    suspend fun deleteJson(
        dir: Path,
        id: String
    ) {
        val file = dir.resolve("$id.json")
        if (file.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    Files.delete(file)
                } catch (e: IOException) {
                    throw StorageException("Failed to delete file for $id", e)
                }
            }
        }
    }
}
