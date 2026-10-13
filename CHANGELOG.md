# Changelog

### Clarified
- **Town creation cost formula**: Documented that the total cost to create a town is `townCost + (homeRegionHeight × layerCost)`.
  - With default values: 1000 + (384 × 10) = **$4840**
  - The `townCost` config value is the BASE cost, not the total
  - This covers both the town creation fee AND the initial home region claim (384 layers from bedrock to sky limit)
  - Updated README.md, TownyConfig.kt comments, and added this changelog entry

### Fixed
- **Cache invalidation bug**: Removed the periodic `invalidateAll()` task that was causing town membership data loss.
  - The cache cleanup task was clearing all in-memory data every 10 minutes
  - If the async batch writer hadn't flushed new data to disk yet, old data would be reloaded
  - Caffeine cache already handles TTL-based expiration via `expireAfterAccess`, so manual invalidation was unnecessary and harmful
- **Invitation notification**: Players now receive a message when invited to a town.
  - Previously: Invitation was created but target player received no notification (TODO was never implemented)
  - Now: Target player sees "You have been invited to join town 'TownName'! Type /accept <inviteId> to accept"
  - Added translation keys: `invite.received`, `invite.how_to_accept`

### Added
- **Data reconciliation command**: `/ta reconcile` - Repairs broken data relationships between residents and towns
  - Synchronizes Resident.townId with Town.residentIds bidirectional refere/clearnces
  - Removes invalid townId references from residents
  - Adds missing resident IDs to town member lists
  - Permission: `towny3d.admin.reconcile`
- **Town deletion cleanup**: Proper cascade deletion when towns are removed
  - All regions owned by the town are now deleted
  - Residents are unlinked from deleted towns
  - Prevents orphaned region data

### Fixed
- **Resident town membership persistence bug**: Residents could lose their town membership after server restart if the town wasn't in cache. Fixed by ensuring lazy-loading of town data from storage.
- **Town data missing error**: Fixed crash when accessing town data for residents whose town wasn't cached. Added proper storage fallback loading.
- **Private town join bypass**: Fixed security issue where players could join private towns without invitation. Added proper `isOpen` check before allowing `/town join`.
- **ClaimStick spam logs**: Removed excessive debug logging from ClaimStickSystem that was flooding console on every player move.
- **Compilation errors**: Fixed multiple compilation issues
  - Changed service visibility from `private` to `internal` in Towny3D.kt (selectionManager, invitationService, translationManager, etc.)
  - Fixed TaxCollectionService to use BatchStorageWriter instead of private .storage access
  - Added missing nation economy methods to CassaforteEconomyAdapter and VaultUnlockedEconomyAdapter
  - Fixed TaxNotificationService imports and simplified getServer() calls
  - Fixed TownAdapter serialization issues in GsonAdapters.kt
  - Fixed extra closing brace syntax error in TownCommand.kt
  - Added remove() method to SpatialRegionCache for proper cleanup

### Changed
- **README feature list**: Moved 13 commands from "Planned Features" to "Implemented Features" that were already coded but not documented
  - All nation commands (create, delete, join, leave, list, info, kick, invite, deposit, withdraw, set capital, etc.)
  - Tax-related commands (/town tax, /nation tax, /plot tax, /tax)
  - Nation capital transfer system
  - Plot sale system (/plot forsale, /plot notforsale, /plot buy)
  - Plot permissions system (/plot set <permission> <mode>)
  - Town spawn points (/town sethome, /town spawn)

### Added
- **Claim Stick System Fix**: Fixed claim stick interaction detection using Hytale's UseBlockEvent.Pre ECS system
  - Left-click (InteractionType.Primary) now correctly sets Position 1
  - F key (InteractionType.Use) now correctly sets Position 2
  - Proper event cancellation to prevent default block interactions
  - Enhanced error handling with try-catch blocks and detailed console logging
  - ClaimStickProductionSystem for distributing claim sticks to players
  - Deprecated old PlayerInteractionListener click methods (non-functional)
- **Testing documentation**: Comprehensive testing checklist for claim stick functionality (see `docs/testing/claim-stick-testing.md`)
- **Complete permission system**: All commands now use `towny3d.` prefix permissions (79 unique permissions). Uses Hytale's built-in `requirePermission()` system for automatic permission checking.
  - General permissions: `towny3d.balance`, `towny3d.resident.*`
  - Town permissions: `towny3d.town.*` (22 permissions)
  - Nation permissions: `towny3d.nation.*` (9 permissions)
  - Plot permissions: `towny3d.plot.*` (13 permissions)
  - Claim permissions: `towny3d.claim.*` (5 permissions)
  - Chat permissions: `towny3d.chat.*` (4 permissions)
  - Invite permissions: `towny3d.invite.*` (5 permissions)
  - Toggle permissions: `towny3d.toggle.*` (5 permissions)
  - Economy permissions: `towny3d.eco.*` (5 permissions)
  - Admin permissions: `towny3d.admin` and subcommands
- **Comprehensive permission documentation**: README now includes complete permission reference with example setups for PermissionsBukkit, LuckPerms, and UltraPermissions
- **Upkeep and taxation system**: Full economic balance system with configurable tax types
  - Town Upkeep: Periodic cost towns pay to server (configurable, defaults to disabled)
  - Nation Upkeep: Periodic cost nations pay to server (configurable, defaults to disabled)
  - Nation Taxes: Nations can tax member towns (flat amount or percentage)
  - Town Taxes: Towns can tax residents (flat amount or percentage)
  - Plot Taxes: Towns can set taxes on individual plots (flat amount or percentage)
- **Tax commands**:
  - `/town tax <flat|percent> <amount>` - Set resident tax
  - `/nation tax <flat|percent> <amount>` - Set nation tax
  - `/plot tax <flat|percent> <amount>` - Set plot tax
  - `/tax` - Show personal tax burden
  - `/ta collecttaxes`, `/ta nexttax`, `/ta reclaim`, `/ta ruinate`, `/ta settaxtime`, `/ta toggle upkeep`
- **Ruination system**: Towns/Nations that can't pay upkeep fall to ruins (can be reclaimed for a fee)
- **Tax consequences**: Plot ownership loss on non-payment, town debt to nations, payment skipping for residents

### Fixed
- **Claim stick interaction detection**: Claim stick now properly detects left-click and F key interactions using UseBlockEvent.Pre instead of non-functional PlayerInteractionListener
- **Event cancellation**: Claim stick now properly cancels default block interactions to prevent breaking blocks while using the claim stick
- **Input mapping documentation**: Clarified that Hytale uses InteractionType.Use (F key) for secondary interactions, not InteractionType.Secondary (right-click)

### Changed
- **Build artifact**: JAR file size increased to 9.1 MB (Towny3D-1.0.0.jar) - includes all claim stick components and systems
- **Max layers consolidation**: Town maxLayers now calculated as `residentCount × 3840` (pure per-resident, no base amount). Existing towns with `maxLayers = 384` or `256` will be automatically migrated on load.
- **Lazy economy loading**: Economy initialization deferred to first transaction instead of plugin startup. Improves startup time and handles missing economies more gracefully.
- **Permission system refactor**: Removed manual permission checking from `TownyAsyncCommand`. Now uses Hytale's native `requirePermission()` and `hasPermission()` system.
- **BaseCommand.kt update**: `TownyAdminCommand` now calls `requirePermission("towny3d.admin")` instead of hardcoded `"hytale.towny.admin"`. Removed `permission` constructor parameter from `TownyAsyncCommand`.
