# Towny3D - CLAUDE.md

A high-performance 3D land protection plugin for Hytale servers.

<!-- AUTO-MANAGED: project-description -->
## Project Overview

Towny3D is a Kotlin-based Hytale server plugin featuring:
- Town/nation management with hierarchical permissions
- 3D plot claims with spatial indexing
- Economy integration (Cassaforte, VaultUnlocked, internal fallback)
- Tax collection and upkeep systems
- Interactive GUI system (town info, region map, deposit, plot settings)
- Translation system (en-US, it-IT)
<!-- END AUTO-MANAGED -->

<!-- AUTO-MANAGED: build-commands -->
## Build Commands

```bash
# Build the plugin
./gradlew build

# Run tests (if available)
./gradlew test

# Clean build
./gradlew clean build
```
<!-- END AUTO-MANAGED -->

<!-- AUTO-MANAGED: architecture -->
## Architecture

```
src/main/kotlin/it/cava99/towny3d/
  Towny3D.kt          # Main plugin class (entry point, delegates to managers)
  LifecycleManager.kt # Coroutine scope and scheduled task lifecycle
  StorageManager.kt   # Storage backend and batch writer lifecycle
  ServiceLocator.kt   # Service and cache references holder
  CacheManager.kt     # Cache initialization and warmup
  DataManager.kt      # Data loading from storage into caches
  ServiceFactory.kt   # Service creation and dependency wiring
  EconomySwitcher.kt  # Lazy economy detection and migration
  cache/              # Caffeine-based entity caches (EntityCache base class + SpatialRegionIndex for spatial queries)
  command/            # Root command collections (thin wrappers)
    admin/            # Admin commands (TaReload, TaSave, TaStats, etc.)
    chat/             # Chat mode commands (ChatTown, ChatNation, ChatGlobal, etc.)
    claim/            # Claim commands (ClaimAuto, ClaimLand, ClaimGcs, etc.)
    invite/           # Invite commands (Accept, Decline, InviteList, etc.)
    nation/           # Nation commands (NationCreate, NationDelete, etc.)
    plot/             # Plot commands (PlotClaim, PlotUnclaim, etc.)
    tax/              # Tax commands (TaxPersonal, TaxTown, TaxNation)
    toggle/           # Toggle commands (TogglePvP, TogglePublic, etc.)
    town/             # Town commands (TownCreate, TownDelete, etc.)
  config/             # Configuration loading and defaults
  economy/            # Economy service providers and adapters
  event/              # Event registration and handling
  exception/          # Custom exception classes
  gui/                # GUI implementations (InteractiveCustomUIPage-based)
    common/           # Shared GUI utilities (GuiUtils with TooltipBuilder)
    TownInfoGui.kt    # Town info hub with deposit/expand/spawn buttons
    RegionMapGui.kt   # 17x17 chunk grid for viewing/claiming regions
    DepositDialogGui.kt # Deposit money dialog with quick-deposit buttons
    LayerPurchaseDialog.kt # Purchase additional Y-layers for town
    PlotSettingsGui.kt # Configure plot settings (forSale, tax, permissions)
  item/               # Custom items (ClaimStick)
  listener/           # Event listeners for protection
  map/                # World map rendering
  model/              # Data models and IDs
  repository/         # Data access abstraction (EntityRepository, TownRepository, etc.)
  service/            # Business logic services (coordinator facades + focused sub-services)
    invitation/       # Invitation handling (TownInviteService, NationInviteService, InviteStorage)
    nation/           # Nation operations (NationCrudService, NationFinancialService)
    region/           # Region/plot operations (RegionCrudService, RegionIndexService, PlotService)
    tax/              # Tax collection (TaxCollectionService + focused collectors)
    town/             # Town operations (TownCrudService, TownFinancialService, TownResidentService, TownSettingsService)
  storage/            # JSON-based persistence
  system/             # Game systems (ClaimStick, TitleTicking)
  translation/        # i18n support
  util/               # Utilities
```

### Layer Architecture
```
presentation (command/gui/listener)
    -> service (business logic)
    -> repository (data access abstraction)
    -> cache (Caffeine)
    -> storage (JSON)
    -> model
```
<!-- END AUTO-MANAGED -->

<!-- AUTO-MANAGED: conventions -->
## Conventions

### Naming
- Classes: PascalCase (e.g., `TownService`, `RegionCache`)
- Functions: camelCase (e.g., `getTownSync`, `loadAllTowns`)
- Properties: camelCase (e.g., `townCache`, `pluginScope`)
- Files: Match class name (e.g., `TownService.kt` contains `class TownService`)

### Kotlin Patterns
- Use `data class` for models with `withModification()` for immutable updates
- Use `suspend` functions for async operations
- Use `Mutex.withLock` for thread-safe cache operations
- Prefer `lateinit var` for dependency injection in plugin class

### Service Layer
- Services should not take entire plugin reference - use dependency objects
- Services coordinate between caches and storage
- GUIs and commands should use services, not caches directly

### Logging
- Use `TownyLog` utility (`TownyLog.info()`, `TownyLog.warn()`, `TownyLog.error()`)
- Never use `println()` or `printStackTrace()` directly in production code
<!-- END AUTO-MANAGED -->

<!-- AUTO-MANAGED: patterns -->
## Patterns

### Cache Pattern
All caches extend `EntityCache<ID, T>` base class:
- Configured via `CacheConfig` data class (maxSize, expireAfterAccess, expireAfterWrite, refreshAfterWrite)
- Caffeine `AsyncCache` with repository-based loading
- Cache-specific expiry strategies:
  - TownCache: `expireAfterAccess` only (30 min) - no refresh to prevent stale reloads
  - ResidentCache: `expireAfterAccess` only (15 min) - no refresh to prevent stale reloads
  - NationCache: `expireAfterWrite` (1 hr) + `refreshAfterWrite` (15 min) - nations change infrequently
  - RegionCache: `expireAfterAccess` (10 min) + `refreshAfterWrite` (5 min)
- Thread pool sizes from `CacheConstants` object
- `getSync(id)` for blocking reads, `get(id)` for suspend reads
- `put(entity)`, `putAll(entities)` for writes
- `invalidate(id)`, `invalidateAll()` for removal
- `warmUp()` for cache pre-population at startup
- NULL sentinel pattern for "not found" caching
- Uses `TownyLog` for error logging

### Model Modification Pattern
Models use `withModification()` builder pattern:
```kotlin
town.withModification(
    name = "NewName",
    balance = 1000.0
)
```

### Command Pattern
- Root commands (`TownCommand`, `NationCommand`, etc.) extend `AbstractCommandCollection` and register subcommands via `addSubCommand()`
- Individual commands extend `AbstractWorldCommand` and implement `execute(context, world, store)`
- Player validation: Use `CommandValidation.validatePlayer()` returns `PlayerContext` (with position) or `validatePlayerBasic()` returns `BasicPlayerContext` (no position)
- Legacy validation: `context.isPlayer()`, `context.senderAsPlayerRef()`, `CommandUtils.getPlayerUuid()`
- Async operations: wrap service calls in `lifecycle.pluginScope.launch { ... }`
- Permission check: `requirePermission("towny3d.xxx")` in constructor

### Spatial Index Pattern
`SpatialRegionIndex` is NOT a cache - it's an in-memory spatial index for fast region lookups:
- Chunk-based indexing with Y-coordinate filtering for 3D queries
- `getRegionAt(location)` for position-based lookups
- `getRegionAtIgnoringY(chunkPos)` for map rendering (ignores Y level)
- Mutex-protected rebuilds via `rebuildLock.withLock`
- Does not persist data - rebuilt when regions change

### Manager Pattern
Towny3D.kt delegates to specialized managers for separation of concerns:
- `LifecycleManager` - manages `pluginScope` and scheduled tasks via `addTask()` and `cancelAll()`
- `StorageManager` - initializes `StorageBackend` and `BatchStorageWriter`, handles `flush()` and `shutdown()`
- `CacheManager` - creates caches with repositories via `initializeCaches()`, provides `warmUpCaches()` and `clearAllCaches()`
- `DataManager` - loads all data from storage into caches in parallel via `loadData()`
- `ServiceFactory` - creates services with dependencies via `createServices()`, returns `economyMutex` for thread-safe switching
- `EconomySwitcher` - handles lazy economy detection after `AllWorldsLoadedEvent`, migrates balances from internal to external economy

### StorageIo Pattern
`StorageIo` is a utility object for generic JSON file I/O operations:
- `loadJson(dir, id, gson, clazz)` - loads single entity from JSON file
- `loadAllJson(dir, gson, clazz)` - loads all entities from directory
- `saveJson(dir, id, entity, gson)` - saves entity to JSON file
- `deleteJson(dir, id)` - deletes JSON file
- Used by `JsonStorageBackend` to eliminate DRY violations

### Service Decomposition Pattern
Large service classes are decomposed into focused sub-services in subpackages:
- Facade services (`TownService`, `NationService`, `RegionService`, `InvitationService`) delegate to focused sub-services
- Sub-services are organized by responsibility (CRUD, financial, settings, residents, invites)
- Sub-services live in `service/<domain>/` subpackages
- Each sub-service has a single responsibility and minimal dependencies

### Constants Pattern
Magic numbers are centralized in dedicated constant objects:
- `CacheConstants` - cache sizes, thread pools, expiry durations
- `TimeConstants` - time intervals in milliseconds for scheduling and timeouts
- `MapConstants` - map rendering dimensions, colors, zoom levels
- Use `object` singleton pattern for constant containers

### GUI Pattern
All GUIs extend `InteractiveCustomUIPage<T>` where T is a data class for event handling:
- Base classes from Hytale: `InteractiveCustomUIPage` (interactive), `BasicCustomUIPage` (display-only)
- Lifecycle via `CustomPageLifetime` enum: `CanDismiss`, `CantClose`, `CanDismissOrCloseThroughInteraction`
- `build(ref, ui, ev, store)` method constructs UI using `UICommandBuilder` and `UIEventBuilder`
- `handleDataEvent(ref, store, data)` method handles user interactions
- `sendUpdate()` refreshes the UI, `close()` dismisses it
- Data classes define event payload with `BuilderCodec` for serialization
- GUI implementations:
  - `TownInfoGui` - town info hub with deposit, expand, spawn, set spawn, settings, residents, plots buttons
  - `RegionMapGui` - 17x17 chunk grid centered on player; click to claim wilderness, view owned plots
  - `DepositDialogGui` - deposit dialog with TextField and quick-deposit buttons ($100, $500, $1000, All)
  - `LayerPurchaseDialog` - purchase additional Y-layers with live cost calculation
  - `PlotSettingsGui` - configure plot: for-sale price, toggles (PvP, Fire, Explosion, Mobs), permissions

### GUI Utilities Pattern
`GuiUtils` object provides shared utilities for all GUIs:
- `formatMoney(amount)` - formats double as currency string (e.g., "$1,234.56")
- `parseMoney(input)` - parses money string to double (handles $, commas)
- `hexWithAlpha(hexColor, alpha)` - converts hex color to RGBA with alpha
- `coloredText(text, color, bold)` - creates formatted Message objects
- `TooltipBuilder` class for building multi-line tooltips with `append()`, `nl()`, `build()`
<!-- END AUTO-MANAGED -->

<!-- MANUAL -->
## Best Practices

### When Adding New Commands
1. Create separate file in appropriate subpackage (command/town/, command/plot/, command/nation/, command/admin/)
2. Import in parent command collection class (TownCommand.kt, PlotCommand.kt, etc.)
3. Keep individual command files under 150 lines
4. Follow naming convention: TownXxxCommand, PlotXxxCommand, NationXxxCommand, TaXxxCommand

### When Adding New Services
1. Create dependency data class instead of taking plugin reference
2. Use services layer, not caches directly, from commands/GUIs
3. Follow existing service patterns in service/ directory
4. Tax-related services go in service/tax/ subpackage
5. For tax collectors, extend pattern: focused single-responsibility collectors
6. For domain services, decompose into focused sub-services in `service/<domain>/` subpackage
7. Add KDoc documentation to all public service APIs

### When Modifying Caches
1. Prefer repository layer over StorageBackend directly for new code
2. Maintain NULL sentinel pattern for negative caching
3. Keep mutex protection for all write operations

### When Modifying Towny3D.kt
1. Avoid adding new responsibilities - delegate to managers or services
2. Use `lifecycle.pluginScope` for coroutine operations
3. Access services through the existing service layer, not directly

### When Adding New GUIs
1. Extend `InteractiveCustomUIPage<T>` where T is a data class for event handling
2. Define inner data class with `BuilderCodec` for serialization
3. Implement `build()` method using `UICommandBuilder` and `UIEventBuilder`
4. Implement `handleDataEvent()` for button/interaction handling
5. Use `GuiUtils` for formatting (money, colors, tooltips)
6. Store UI templates in `resources/Common/UI/Custom/Pages/HytaleTowny_*.ui`
7. Add translation keys to `resources/Server/Languages/*/gui.lang`
8. Use `sendUpdate()` to refresh UI, `close()` to dismiss
<!-- END MANUAL -->
