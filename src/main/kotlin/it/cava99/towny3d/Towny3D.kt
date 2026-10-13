package it.cava99.towny3d

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider
import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.config.TownyConfigLoader
import it.cava99.towny3d.map.TownyWorldMapProvider
import it.cava99.towny3d.service.*
import it.cava99.towny3d.system.TitleTickingSystem
import it.cava99.towny3d.util.SelectionManager
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.*
import java.util.logging.Level

/**
 * Main plugin class for Towny3D.
 *
 * A high-performance 3D territory plugin using Kotlin coroutines and Caffeine cache.
 * Delegates initialization and lifecycle management to specialized manager classes.
 */
class Towny3D(init: JavaPluginInit) : JavaPlugin(init) {

    companion object {
        @JvmStatic
        lateinit var instance: Towny3D
            private set
    }

    // Service locator for all services, caches, and utilities
    private val services = ServiceLocator()

    // Public service accessors for backward compatibility
    val regionService: RegionService get() = services.regionService
    val townService: TownService get() = services.townService
    val residentService: ResidentService get() = services.residentService
    val nationService: NationService get() = services.nationService
    val economyService: it.cava99.towny3d.economy.EconomyService get() = services.economyService
    val taxCollectionService: it.cava99.towny3d.service.tax.TaxCollectionService get() = services.taxCollectionService
    val taxNotificationService: it.cava99.towny3d.service.tax.TaxNotificationService get() = services.taxNotificationService
    val plotBorderRenderer: it.cava99.towny3d.util.PlotBorderRenderer get() = services.plotBorderRenderer

    // Internal cache and utility accessors
    internal val regionCache: it.cava99.towny3d.cache.RegionCache get() = services.regionCache
    internal val townCache: it.cava99.towny3d.cache.TownCache get() = services.townCache
    internal val residentCache: it.cava99.towny3d.cache.ResidentCache get() = services.residentCache
    internal val nationCache: it.cava99.towny3d.cache.NationCache get() = services.nationCache
    internal val spatialIndex: it.cava99.towny3d.cache.SpatialRegionIndex get() = services.spatialIndex
    internal val translationManager: it.cava99.towny3d.translation.TranslationManager get() = services.translationManager
    internal val titleTickingSystem: TitleTickingSystem get() = services.titleTickingSystem
    internal val permissionEvaluator: PermissionEvaluator get() = services.permissionEvaluator
    internal val selectionManager: SelectionManager get() = services.selectionManager
    internal val claimStickManager: it.cava99.towny3d.item.ClaimStickManager get() = services.claimStickManager
    internal val protectionService: it.cava99.towny3d.listener.ProtectionService get() = services.protectionService
    internal val chatModeService: ChatModeService get() = services.chatModeService
    internal val invitationService: InvitationService get() = services.invitationService

    // Configuration
    private var _config: TownyConfig = TownyConfig.default()
    fun getConfig(): TownyConfig = _config
    fun setConfig(config: TownyConfig) { _config = config; saveConfig() }
    fun saveConfig() = try { TownyConfigLoader.saveConfig(_config, dataDirectory) } catch (e: Exception) { TownyLog.error("Failed to save config", e) }

    // Lifecycle and managers
    private val lifecycle = LifecycleManager()
    val pluginScope: CoroutineScope get() = lifecycle.pluginScope

    private lateinit var storageManager: StorageManager
    private lateinit var cacheManager: CacheManager
    private lateinit var serviceFactory: ServiceFactory
    private lateinit var dataManager: DataManager
    private lateinit var economySwitcher: EconomySwitcher

    // ==================== LIFECYCLE METHODS ====================

    override fun setup() {
        instance = this
        TownyLog.info("Setting up Towny3D...")

        try {
            _config = TownyConfigLoader.loadConfig(dataDirectory)
            initializeTranslation()
            initializeStorage()
            initializeCaches()
            initializeServices()
            registerSystemsAndProviders()
            registerWorldEventHandlers()
            TownyLog.info("Towny3D setup complete!")
        } catch (e: Exception) {
            TownyLog.error("Failed to setup Towny3D", e)
            throw e
        }
    }

    override fun start() {
        TownyLog.info("Starting Towny3D...")

        try {
            dataManager.loadData({ TownyLog.info(it) }, { msg, e -> TownyLog.error(msg, e) })
            registerEvents()
            registerCommands()
            startMaintenanceTasks()
            TownyLog.info("Towny3D started successfully!")
        } catch (e: Exception) {
            TownyLog.error("Failed to start Towny3D", e)
            throw e
        }
    }

    override fun shutdown() {
        TownyLog.info("Shutting down Towny3D...")

        try {
            lifecycle.cancelAll()

            if (::storageManager.isInitialized) {
                runBlocking {
                    saveAllData()
                    if (::cacheManager.isInitialized) cacheManager.clearAllCaches()
                    storageManager.shutdown()
                }
            }

            try { services.plotBorderRenderer.shutdown() } catch (_: UninitializedPropertyAccessException) {}

            TownyLog.info("Towny3D shutdown complete!")
        } catch (e: Exception) {
            TownyLog.error("Error during shutdown", e)
        }
    }

    // ==================== INITIALIZATION METHODS ====================

    private fun initializeTranslation() {
        val translationLogger = java.util.logging.Logger.getLogger(Towny3D::class.java.name)
        try {
            services.translationManager = it.cava99.towny3d.translation.TranslationManager(dataDirectory.toFile(), translationLogger)
            services.translationManager.loadLocale(_config.defaultLanguage)
            TownyLog.info("TranslationManager initialized with locale: ${_config.defaultLanguage}")
        } catch (e: Exception) {
            TownyLog.error("Failed to load locale '${_config.defaultLanguage}', falling back to en-US", e)
            services.translationManager = it.cava99.towny3d.translation.TranslationManager(dataDirectory.toFile(), translationLogger)
            services.translationManager.loadLocale("en-US")
        }
        it.cava99.towny3d.util.MessageFormatter.setTranslationManager(services.translationManager)
        TownyLog.info("MessageFormatter configured with TranslationManager")
    }

    private fun initializeStorage() {
        storageManager = StorageManager(dataDirectory, pluginScope, _config)
        runBlocking { storageManager.initialize() }
        TownyLog.info("Storage manager initialized: ${dataDirectory.resolve("data")}")
    }

    private fun initializeCaches() {
        cacheManager = CacheManager(storageManager.getStorageBackend(), pluginScope, services)
        cacheManager.initializeCaches()
        TownyLog.info("Caches initialized")
        cacheManager.warmUpCaches({ TownyLog.info(it) }, { msg, e -> TownyLog.error(msg, e) })

        dataManager = DataManager(storageManager.getStorageBackend(), pluginScope, services)
    }

    private fun initializeServices() {
        serviceFactory = ServiceFactory(services, _config, pluginScope, storageManager.getBatchWriter(), storageManager.getStorageBackend(), this)
        serviceFactory.createServices({ TownyLog.info(it) })

        economySwitcher = EconomySwitcher(services, storageManager.getStorageBackend(), this, serviceFactory.getEconomyMutex())
    }

    private fun registerSystemsAndProviders() {
        services.titleTickingSystem = TitleTickingSystem()
        getEntityStoreRegistry().registerSystem(services.titleTickingSystem)
        TownyLog.info("Registered TitleTickingSystem")

        it.cava99.towny3d.system.ClaimStickSystem.register(this)
        TownyLog.info("Registered ClaimStickSystem PlayerMouseButtonEvent handler")

        getChunkStoreRegistry().registerSystem(it.cava99.towny3d.system.WorldMapUpdateTickingSystem())
        TownyLog.info("Registered WorldMapUpdateTickingSystem")

        IWorldMapProvider.CODEC.register(TownyWorldMapProvider.ID, TownyWorldMapProvider::class.java, TownyWorldMapProvider.CODEC)
        TownyLog.info("Registered TownyWorldMapProvider codec")

        try {
            it.cava99.towny3d.event.HytaleServerEventRegistrar.registerAllEvents()
            TownyLog.info("Registered HytaleServer event listeners")
        } catch (e: Exception) {
            TownyLog.error("Failed to register HytaleServer events", e)
        }
    }

    private fun registerWorldEventHandlers() {
        getEventRegistry().registerGlobal(AddWorldEvent::class.java) { event ->
            val world = event.world
            TownyLog.info("World loaded: ${world.name}")
            if (!world.worldConfig.isDeleteOnRemove) {
                world.worldConfig.setWorldMapProvider(TownyWorldMapProvider())
                TownyLog.info("Set TownyWorldMapProvider for world: ${world.name}")
            }
        }

        getEventRegistry().registerGlobal(RemoveWorldEvent::class.java) { event ->
            TownyLog.info("World removed: ${event.world.name}")
        }

        getEventRegistry().registerGlobal(AllWorldsLoadedEvent::class.java) {
            pluginScope.launch {
                try { economySwitcher.handleLazyEconomyDetection({ TownyLog.info(it) }, { TownyLog.warn(it) }, { msg, e -> TownyLog.error(msg, e) }) }
                catch (e: Exception) { TownyLog.error("Failed during lazy economy detection", e) }
            }
        }
        TownyLog.info("Registered lazy economy detection handler")
    }

    private fun registerEvents() {
        TownyLog.info("Registering events...")
        it.cava99.towny3d.event.EventRegistrar(this).registerAllEvents()
    }

    private fun registerCommands() {
        it.cava99.towny3d.command.CommandRegistrar(this).registerAllCommands()
    }

    private fun startMaintenanceTasks() {
        val scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2)

        // Auto-save task
        lifecycle.addTask(scheduler.scheduleAtFixedRate({
            pluginScope.launch {
                try { saveAllData(); TownyLog.info("Auto-save complete") }
                catch (e: Exception) { TownyLog.error("Auto-save failed", e) }
            }
        }, _config.autoSaveIntervalSeconds * 1000L, _config.autoSaveIntervalSeconds * 1000L, java.util.concurrent.TimeUnit.MILLISECONDS))

        // Tax collection task (every minute)
        lifecycle.addTask(scheduler.scheduleAtFixedRate({
            pluginScope.launch {
                try {
                    val nextCollection = services.taxCollectionService.getNextCollectionTime()
                    services.taxNotificationService.sendWarningIfNeeded(nextCollection)
                    val result = services.taxCollectionService.checkAndCollectIfNeeded()
                    if (result != null) {
                        TownyLog.info("Tax collection completed: ${result.totalCollected} collected")
                        services.taxNotificationService.sendPostCollectionSummary(result)
                    }
                } catch (e: Exception) { TownyLog.error("Tax collection check failed", e) }
            }
        }, 60_000L, 60_000L, java.util.concurrent.TimeUnit.MILLISECONDS))

        TownyLog.info("Maintenance tasks started")
    }

    // ==================== PUBLIC API ====================

    suspend fun saveAllData() {
        TownyLog.info("Saving all data...")
        storageManager.flush()
    }
}
