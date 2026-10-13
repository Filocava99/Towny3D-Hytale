package it.cava99.towny3d

import it.cava99.towny3d.config.TownyConfig
import it.cava99.towny3d.service.*
import it.cava99.towny3d.storage.BatchStorageWriter
import it.cava99.towny3d.storage.StorageBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

/**
 * Factory for creating and wiring all services.
 * Handles the complex dependency injection for the service layer.
 */
class ServiceFactory(
    private val services: ServiceLocator,
    private val config: TownyConfig,
    private val pluginScope: CoroutineScope,
    private val batchWriter: BatchStorageWriter,
    private val storageBackend: StorageBackend,
    private val plugin: Towny3D
) {
    private val economyMutex = Mutex()

    /**
     * Creates all services with their dependencies.
     */
    fun createServices(logInfo: (String) -> Unit) {
        // Create economy adapter with auto-detection (Cassaforte > VaultUnlocked > Internal)
        services.economyService = it.cava99.towny3d.economy.EconomyServiceProvider.detectEconomy(
            storageBackend,
            plugin
        )
        logInfo("Economy service configured")

        // Create permission evaluator
        services.permissionEvaluator = PermissionEvaluator(
            services.residentCache,
            services.townCache
        )

        // Create services with their dependencies (order matters to avoid circular deps)
        services.regionService = RegionService(services.regionCache, services.spatialIndex, services.townCache, batchWriter)
        services.residentService = ResidentService(services.residentCache, services.townCache, batchWriter)
        services.townService = TownService(services.townCache, batchWriter, services.regionService, services.residentService, config)
        services.nationService = NationService(services.nationCache, services.townCache, batchWriter)

        // Create utilities
        services.selectionManager = it.cava99.towny3d.util.SelectionManager()
        services.claimStickManager = it.cava99.towny3d.item.ClaimStickManager(plugin)
        services.protectionService = it.cava99.towny3d.listener.ProtectionService(
            it.cava99.towny3d.listener.ProtectionDependencies(
                pluginScope = pluginScope,
                regionService = services.regionService,
                residentService = services.residentService,
                permissionEvaluator = services.permissionEvaluator,
                spatialIndex = services.spatialIndex,
                residentCache = services.residentCache,
                townCache = services.townCache,
                regionCache = services.regionCache,
                claimStickManager = services.claimStickManager
            )
        )
        services.chatModeService = ChatModeService(plugin)
        services.invitationService = InvitationService(
            InvitationDependencies(
                townService = services.townService,
                nationService = services.nationService,
                residentService = services.residentService,
                translationManager = services.translationManager
            )
        )
        services.plotBorderRenderer = it.cava99.towny3d.util.PlotBorderRenderer(plugin)

        // Create tax collection collectors
        createTaxServices()

        logInfo("Services initialized")
    }

    private fun createTaxServices() {
        val townUpkeepCollector = it.cava99.towny3d.service.tax.TownUpkeepCollector(
            townCache = services.townCache,
            economyService = services.economyService,
            config = config,
            pluginScope = pluginScope,
            batchWriter = batchWriter
        )
        val nationUpkeepCollector = it.cava99.towny3d.service.tax.NationUpkeepCollector(
            nationCache = services.nationCache,
            economyService = services.economyService,
            config = config,
            pluginScope = pluginScope,
            batchWriter = batchWriter
        )
        val nationTaxCollector = it.cava99.towny3d.service.tax.NationTaxCollector(
            nationCache = services.nationCache,
            townCache = services.townCache,
            economyService = services.economyService,
            config = config,
            pluginScope = pluginScope,
            batchWriter = batchWriter
        )
        val townTaxCollector = it.cava99.towny3d.service.tax.TownTaxCollector(
            townCache = services.townCache,
            residentCache = services.residentCache,
            economyService = services.economyService
        )
        val plotTaxCollector = it.cava99.towny3d.service.tax.PlotTaxCollector(
            regionCache = services.regionCache,
            residentCache = services.residentCache,
            townCache = services.townCache,
            economyService = services.economyService,
            pluginScope = pluginScope,
            batchWriter = batchWriter
        )
        val reclaimService = it.cava99.towny3d.service.tax.ReclaimService(
            townCache = services.townCache,
            nationCache = services.nationCache,
            economyService = services.economyService,
            config = config,
            batchWriter = batchWriter
        )

        // Create tax collection service (orchestrates collectors)
        services.taxCollectionService = it.cava99.towny3d.service.tax.TaxCollectionService(
            townUpkeepCollector = townUpkeepCollector,
            nationUpkeepCollector = nationUpkeepCollector,
            nationTaxCollector = nationTaxCollector,
            townTaxCollector = townTaxCollector,
            plotTaxCollector = plotTaxCollector,
            reclaimService = reclaimService,
            config = config
        )

        // Create tax notification service
        services.taxNotificationService = it.cava99.towny3d.service.tax.TaxNotificationService(
            townCache = services.townCache,
            nationCache = services.nationCache,
            residentCache = services.residentCache,
            economyService = services.economyService,
            config = config,
            pluginScope = pluginScope
        )
    }

    /**
     * Gets the economy mutex for thread-safe economy switching.
     */
    fun getEconomyMutex(): Mutex = economyMutex
}
