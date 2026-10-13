package it.cava99.towny3d.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import it.cava99.towny3d.util.TownyLog
import java.nio.file.Files
import java.nio.file.Path

/**
 * Handles loading and saving TownyConfig to/from JSON.
 * Automatically merges new properties from default config into existing configs.
 */
object TownyConfigLoader {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private const val CONFIG_FILE = "towny-config.json"
    private const val CONFIG_VERSION_KEY = "__configVersion__"

    /**
     * Loads the TownyConfig from the specified directory.
     * If the config file doesn't exist, creates a default one.
     * If the config exists but is missing properties, merges in defaults.
     */
    fun loadConfig(dataDirectory: Path): TownyConfig {
        val configFile = dataDirectory.resolve(CONFIG_FILE)

        return if (Files.exists(configFile)) {
            try {
                val json = String(Files.readAllBytes(configFile))
                val loadedConfig = gson.fromJson(json, TownyConfig::class.java)

                // Merge with default config to add any missing properties
                val defaultConfig = TownyConfig.default()
                val mergedConfig = mergeConfigs(loadedConfig, defaultConfig)

                // Check if config was updated and needs to be saved
                if (configNeedsUpdate(json, defaultConfig)) {
                    TownyLog.info("Config is missing new properties, updating...")
                    saveToFile(mergedConfig, configFile)
                    TownyLog.info("Config updated with new properties")
                } else {
                    TownyLog.info("Loaded config from $configFile")
                }

                mergedConfig
            } catch (e: Exception) {
                TownyLog.error("Failed to load config, using defaults: ${e.message}")
                val defaultConfig = TownyConfig.default()
                saveToFile(defaultConfig, configFile)
                defaultConfig
            }
        } else {
            // Create default config file
            val defaultConfig = TownyConfig.default()
            saveToFile(defaultConfig, configFile)
            TownyLog.info("Created default config at $configFile")
            defaultConfig
        }
    }

    /**
     * Merges the loaded config with the default config.
     * Returns a new config with all properties, preferring loaded values over defaults.
     */
    private fun mergeConfigs(loaded: TownyConfig, default: TownyConfig): TownyConfig {
        return TownyConfig(
            // Composed config classes - merge each nested object
            economy = mergeEconomyConfig(loaded.economy, default.economy),
            tax = mergeTaxSettings(loaded.tax, default.tax),
            world = mergeWorldSettings(loaded.world, default.world),

            // Name validation
            maxTownNameLength = loaded.maxTownNameLength,
            maxNationNameLength = loaded.maxNationNameLength,
            maxResidentNameLength = loaded.maxResidentNameLength,

            // Maintenance settings
            autoSaveIntervalSeconds = loaded.autoSaveIntervalSeconds,
            cacheCleanupIntervalMinutes = loaded.cacheCleanupIntervalMinutes,
            cacheCleanupEntriesRemoved = loaded.cacheCleanupEntriesRemoved,

            // Feature flags
            enableNations = loaded.enableNations,
            version = default.version, // Always use latest version
            defaultLanguage = loaded.defaultLanguage,
            enableTownChat = loaded.enableTownChat,
            enableNationChat = loaded.enableNationChat
        )
    }

    /**
     * Merges loaded economy config with defaults.
     */
    private fun mergeEconomyConfig(loaded: EconomyConfig, default: EconomyConfig): EconomyConfig {
        return EconomyConfig(
            townCost = loaded.townCost,
            layerCost = loaded.layerCost,
            layersPerPlayer = loaded.layersPerPlayer,
            defaultTownBalance = loaded.defaultTownBalance,
            homeRegionHeight = loaded.homeRegionHeight,
            nationCreationCost = loaded.nationCreationCost,
            enableEconomy = loaded.enableEconomy,
            enablePlotSales = loaded.enablePlotSales
        )
    }

    /**
     * Merges loaded tax settings with defaults.
     */
    private fun mergeTaxSettings(loaded: TaxSettings, default: TaxSettings): TaxSettings {
        return TaxSettings(
            townUpkeep = loaded.townUpkeep,
            nationUpkeep = loaded.nationUpkeep,
            enableTownUpkeep = loaded.enableTownUpkeep,
            enableNationUpkeep = loaded.enableNationUpkeep,
            taxCollectionIntervalHours = loaded.taxCollectionIntervalHours,
            defaultTownTaxEnabled = loaded.defaultTownTaxEnabled,
            defaultTownTaxType = loaded.defaultTownTaxType,
            defaultTownTaxAmount = loaded.defaultTownTaxAmount,
            defaultNationTaxEnabled = loaded.defaultNationTaxEnabled,
            defaultNationTaxType = loaded.defaultNationTaxType,
            defaultNationTaxAmount = loaded.defaultNationTaxAmount,
            upkeepTimeOfDay = loaded.upkeepTimeOfDay,
            reclaimFeeMultiplier = loaded.reclaimFeeMultiplier,
            ruinedDeletionDays = loaded.ruinedDeletionDays,
            maxNationTaxPercent = loaded.maxNationTaxPercent,
            maxTownTaxPercent = loaded.maxTownTaxPercent,
            maxPlotTaxPercent = loaded.maxPlotTaxPercent,
            taxWarningMinutesBefore = loaded.taxWarningMinutesBefore,
            taxWarningMessage = loaded.taxWarningMessage,
            taxSummaryMessageEnabled = loaded.taxSummaryMessageEnabled,
            maxNationDebtMultiplier = loaded.maxNationDebtMultiplier,
            allowTownLeaveWithDebt = loaded.allowTownLeaveWithDebt
        )
    }

    /**
     * Merges loaded world settings with defaults.
     */
    private fun mergeWorldSettings(loaded: WorldSettings, default: WorldSettings): WorldSettings {
        return WorldSettings(
            defaultWorldName = loaded.defaultWorldName,
            maxClaimHeight = loaded.maxClaimHeight,
            claimCooldownMs = loaded.claimCooldownMs
        )
    }

    /**
     * Checks if the loaded config JSON is missing any properties from the default config.
     */
    private fun configNeedsUpdate(loadedJson: String, defaultConfig: TownyConfig): Boolean {
        val defaultJson = gson.toJsonTree(defaultConfig) as JsonObject
        val loadedJsonObject = gson.fromJson(loadedJson, JsonObject::class.java)

        // Check for missing keys
        for (key in defaultJson.keySet()) {
            if (!loadedJsonObject.has(key)) {
                return true
            }
        }

        return false
    }

    /**
     * Saves the TownyConfig to the data directory.
     */
    fun saveConfig(config: TownyConfig, dataDirectory: Path) {
        saveToFile(config, dataDirectory.resolve(CONFIG_FILE))
    }

    /**
     * Saves the TownyConfig to a specific file path.
     */
    private fun saveToFile(config: TownyConfig, configFile: Path) {
        try {
            val json = gson.toJson(config)
            Files.write(configFile, json.toByteArray())
            TownyLog.info("Saved config to $configFile")
        } catch (e: Exception) {
            TownyLog.error("Failed to save config", e)
        }
    }
}
