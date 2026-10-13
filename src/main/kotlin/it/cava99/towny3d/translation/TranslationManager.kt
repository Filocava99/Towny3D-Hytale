package it.cava99.towny3d.translation

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Manages loading and retrieval of translated messages.
 */
class TranslationManager(
    private val dataDirectory: File,
    private val logger: Logger
) {

    companion object {
        private const val LANGUAGES_DIR = "Server/Languages"
    }

    private val translations = ConcurrentHashMap<String, String>()
    private var currentLocale = "en-US"

    /**
     * Load all .lang files for the specified locale.
     * First tries to load from data directory (user customizations),
     * extracts from JAR if not present, then loads.
     */
    fun loadLocale(locale: String): Unit {
        require(locale.isNotBlank()) { "Locale cannot be blank" }

        translations.clear()
        currentLocale = locale

        val localeDir = dataDirectory.resolve("$LANGUAGES_DIR/$locale")

        // If locale directory doesn't exist, extract files from JAR
        if (!localeDir.exists()) {
            logger.info("[Towny3D] Locale directory not found, extracting translation files to: $localeDir")
            extractLocaleFiles(locale, localeDir)
        }

        // Load from data directory (user customizations or extracted files)
        if (localeDir.exists()) {
            localeDir.walkTopDown()
                .filter { it.isFile && it.extension == "lang" }
                .forEach { langFile ->
                    loadLangFile(langFile)
                }
            logger.info("[Towny3D] Loaded ${translations.size} translations for locale '$locale' from data directory")
            return
        }

        // Should not reach here if extraction worked, but handle gracefully
        throw IllegalArgumentException("Locale '$locale' not found - failed to extract from JAR resources")
    }

    /**
     * Load a single .lang file into the translations map.
     */
    private fun loadLangFile(file: File) {
        try {
            Files.readAllLines(file.toPath()).forEach { line ->
                val trimmed = line.trim()

                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEach
                }

                // Parse key = value format
                val separatorIndex = trimmed.indexOf('=')
                if (separatorIndex > 0) {
                    val key = trimmed.substring(0, separatorIndex).trim()
                    val value = trimmed.substring(separatorIndex + 1).trim()

                    if (key.isNotEmpty()) {
                        translations[key] = value
                    }
                }
            }
        } catch (e: IOException) {
            logger.warning("[Towny3D WARNING] Failed to load language file ${file.name}: ${e.message}")
        }
    }

    /**
     * Extract translation files from JAR to data directory.
     * This allows users to customize translations without rebuilding the plugin.
     */
    private fun extractLocaleFiles(locale: String, targetDir: File) {
        try {
            // Create target directory
            Files.createDirectories(targetDir.toPath())

            // Known translation files to extract
            val langFiles = listOf("commands.lang", "taxes.lang", "protection.lang", "economy.lang", "gui.lang")
            var extractedCount = 0

            for (langFile in langFiles) {
                val resourcePath = "/$LANGUAGES_DIR/$locale/$langFile"
                val resource = TranslationManager::class.java.getResourceAsStream(resourcePath)

                if (resource != null) {
                    try {
                        val targetFile = targetDir.resolve(langFile)
                        resource.use { input ->
                            Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                        extractedCount++
                    } catch (e: IOException) {
                        logger.warning("[Towny3D WARNING] Failed to extract $langFile: ${e.message}")
                    }
                }
            }

            if (extractedCount > 0) {
                logger.info("[Towny3D] Extracted $extractedCount translation files to: $targetDir")
                logger.info("[Towny3D] You can now edit these files to customize translations")
            } else {
                logger.warning("[Towny3D WARNING] No translation files found for locale '$locale' in JAR")
            }
        } catch (e: IOException) {
            logger.warning("[Towny3D WARNING] Failed to extract translation files: ${e.message}")
        }
    }

    /**
     * Get a translated message by key, with optional placeholder replacement.
     */
    fun get(key: String, vararg args: Pair<String, Any>): String {
        var message = translations[key]

        if (message == null) {
            logger.warning("[Towny3D WARNING] Missing translation for key '$key' in locale '$currentLocale'")
            message = key
        }

        return replacePlaceholders(message, args)
    }

    /**
     * Check if a translation key exists.
     */
    fun hasKey(key: String): Boolean = translations.containsKey(key)

    /**
     * Get the current locale.
     */
    fun getCurrentLocale(): String = currentLocale

    /**
     * Replace placeholders in the message with provided values.
     */
    private fun replacePlaceholders(message: String, args: Array<out Pair<String, Any>>): String {
        var result = message

        args.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }

        return result
    }
}
