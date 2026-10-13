package it.cava99.towny3d

import it.cava99.towny3d.service.*
import it.cava99.towny3d.cache.*
import it.cava99.towny3d.util.SelectionManager
import it.cava99.towny3d.util.PlotBorderRenderer
import it.cava99.towny3d.translation.TranslationManager
import it.cava99.towny3d.system.TitleTickingSystem

/**
 * Centralized service locator for all Towny3D services, caches, and utilities.
 * Provides a clean separation of dependency management from the main plugin class.
 */
class ServiceLocator {
    // Services
    lateinit var regionService: RegionService
    lateinit var townService: TownService
    lateinit var residentService: ResidentService
    lateinit var nationService: NationService
    lateinit var economyService: it.cava99.towny3d.economy.EconomyService
    lateinit var taxCollectionService: it.cava99.towny3d.service.tax.TaxCollectionService
    lateinit var taxNotificationService: it.cava99.towny3d.service.tax.TaxNotificationService
    lateinit var permissionEvaluator: PermissionEvaluator
    lateinit var chatModeService: ChatModeService
    lateinit var invitationService: InvitationService

    // Utilities
    lateinit var plotBorderRenderer: PlotBorderRenderer
    lateinit var selectionManager: SelectionManager
    lateinit var claimStickManager: it.cava99.towny3d.item.ClaimStickManager
    lateinit var protectionService: it.cava99.towny3d.listener.ProtectionService
    lateinit var translationManager: TranslationManager
    lateinit var titleTickingSystem: TitleTickingSystem

    // Caches (internal)
    internal lateinit var regionCache: RegionCache
    internal lateinit var townCache: TownCache
    internal lateinit var residentCache: ResidentCache
    internal lateinit var nationCache: NationCache
    internal lateinit var spatialIndex: SpatialRegionIndex
}
