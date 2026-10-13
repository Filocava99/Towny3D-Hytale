![alt text](https://i.imgur.com/DzMqjTD.jpeg)

# Towny3D

A high-performance 3D land protection plugin for Hytale servers featuring town/nation management, plot claims, PvP control, and advanced permission systems with Kotlin coroutines and spatial indexing.

> **Status: Alpha** - This plugin is experimental and under active development. It has been ported from the Spigot API to the Hytale API with extensive use of Claude Code, relying primarily on decompiled source code of the Hytale server.

## Table of Contents

- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Languages / Translations](#languages--translations)
- [Key Features](#key-features)
- [Permissions](#permissions)
- [Architecture](#architecture)
- [Development Status](#development-status)

## Dependencies

**Required:**

*   [Cassaforte](https://www.curseforge.com/hytale/mods/cassaforte) - Service registry plugin for economy, permissions, and chat integration

Towny3D requires Cassaforte to be installed and loaded first. Cassaforte provides the service registry that Towny3D uses for economy integration (will fall back to internal economy if none available)

## Configuration

Towny3D stores its configuration in `mods/it.cava99.towny3d_Towny3D/towny-config.json`. On first startup, a default configuration file is created automatically.

### Configuration Properties

#### Economy Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `townCost` | Double | `1000.0` | **BASE** cost to create a new town. The **TOTAL** cost is: `townCost + (homeRegionHeight × layerCost)`. With defaults: 1000 + (384 × 10) = **$4840** |
| `layerCost` | Double | `10.0` | Cost per claimed layer (Y-range) |
| `layersPerPlayer` | Int | `3840` | Bonus layers per resident (vertical claim height) |
| `defaultTownBalance` | Double | `0.0` | Starting balance for new towns |
| `homeRegionHeight` | Int | `384` | Default height for home regions (-64 to 320) |

#### Nation Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `nationCreationCost` | Double | `10000.0` | Cost to create a new nation |

#### Claim Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `maxClaimHeight` | Int | `256` | Maximum height of a single claim |
| `claimCooldownMs` | Long | `1000` | Cooldown between claim attempts (milliseconds) |

#### Name Validation

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `maxTownNameLength` | Int | `32` | Maximum characters in town names |
| `maxNationNameLength` | Int | `32` | Maximum characters in nation names |
| `maxResidentNameLength` | Int | `16` | Maximum characters in resident names |

#### Maintenance Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `autoSaveIntervalSeconds` | Int | `300` | Interval between auto-saves (seconds, default: 5 minutes) |
| `cacheCleanupIntervalMinutes` | Int | `10` | Interval between cache cleanups (minutes) |
| `cacheCleanupEntriesRemoved` | Int | `100` | Number of entries to remove during cleanup |

#### World Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `defaultWorldName` | String | `"world"` | Default world name |

#### Feature Flags

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enableNations` | Boolean | `true` | Enable/disable nation system |
| `enableEconomy` | Boolean | `true` | Enable economy features |
| `enablePlotSales` | Boolean | `true` | Enable plot selling/buying |
| `enableTownChat` | Boolean | `true` | Enable town chat channel |
| `enableNationChat` | Boolean | `true` | Enable nation chat channel |

#### Language Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `defaultLanguage` | String | `"en-US"` | Default language locale (BCP 47 format) |

**Supported values:** `en-US`, `it-IT`, or any custom locale you create

**Format:** Language code must follow BCP 47 format: `ll-CC` where `ll` is lowercase ISO 639-1 language code and `CC` is uppercase ISO 3166-1 country code.

#### Upkeep Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `townUpkeep` | Double | `0.0` | Daily cost towns must pay to stay alive |
| `nationUpkeep` | Double | `0.0` | Daily cost nations must pay to stay alive |
| `enableTownUpkeep` | Boolean | `false` | Whether towns must pay daily upkeep |
| `enableNationUpkeep` | Boolean | `false` | Whether nations must pay daily upkeep |

**Note:** When upkeep is enabled, towns/nations that cannot pay will fall to ruins and can be reclaimed.

#### Tax Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `taxCollectionIntervalHours` | Int | `24` | How often taxes are collected (hours) |
| `defaultTownTaxEnabled` | Boolean | `false` | Default tax enabled status for new towns |
| `defaultTownTaxType` | String | `"FLAT"` | Default tax type for new towns |
| `defaultTownTaxAmount` | Double | `0.0` | Default tax amount for new towns |
| `defaultNationTaxEnabled` | Boolean | `false` | Default tax enabled status for new nations |
| `defaultNationTaxType` | String | `"FLAT"` | Default tax type for new nations |
| `defaultNationTaxAmount` | Double | `0.0` | Default tax amount for new nations |
| `upkeepTimeOfDay` | String | `"18:00"` | Time of day when upkeep is collected (HH:MM format) |
| `reclaimFeeMultiplier` | Double | `2.0` | Multiplier for town/nation reclamation costs |
| `ruinedDeletionDays` | Int | `30` | Days before ruined towns/nations are deleted |
| `maxNationTaxPercent` | Double | `10.0` | Maximum nation tax percentage (0-100) |
| `maxTownTaxPercent` | Double | `25.0` | Maximum town tax percentage (0-100) |
| `maxPlotTaxPercent` | Double | `50.0` | Maximum plot tax percentage (0-100) |
| `taxWarningMinutesBefore` | Int | `30` | Minutes before tax to send warning |
| `taxWarningMessage` | String | Warning message template with {time} placeholder |
| `taxSummaryMessageEnabled` | Boolean | `true` | Whether to show post-tax collection summaries |
| `maxNationDebtMultiplier` | Double | `5.0` | Multiplier for max nation debt limit |
| `allowTownLeaveWithDebt` | Boolean | `false` | Whether residents can leave towns with debt |

**Tax Types:**
- `"FLAT"` - Fixed amount tax (e.g., 100 coins)
- `"PERCENTAGE"` - Percentage-based tax (e.g., 10% of balance, must be 0-100)

### Auto-Update Behavior

When you add new properties to the config file in future plugin versions, Towny3D will automatically:
1. Detect missing properties when loading your existing config
2. Merge in the new properties with default values
3. Save the updated config file
4. Preserve all your existing settings

Your custom values are never overwritten - only missing properties are added with their defaults.

### Example Config

```json
{
  "townCost": 1000.0,
  "layerCost": 10.0,
  "layersPerPlayer": 3840,
  "defaultTownBalance": 0.0,
  "homeRegionHeight": 384,
  "nationCreationCost": 10000.0,
  "maxClaimHeight": 256,
  "claimCooldownMs": 1000,
  "maxTownNameLength": 32,
  "maxNationNameLength": 32,
  "maxResidentNameLength": 16,
  "autoSaveIntervalSeconds": 300,
  "cacheCleanupIntervalMinutes": 10,
  "cacheCleanupEntriesRemoved": 100,
  "defaultWorldName": "world",
  "enableNations": true,
  "version": "1.0.0",
  "defaultLanguage": "en-US",
  "enableEconomy": true,
  "enablePlotSales": true,
  "enableTownChat": true,
  "enableNationChat": true,
  "townUpkeep": 0.0,
  "nationUpkeep": 0.0,
  "enableTownUpkeep": false,
  "enableNationUpkeep": false,
  "taxCollectionIntervalHours": 24,
  "defaultTownTaxEnabled": false,
  "defaultTownTaxType": "FLAT",
  "defaultTownTaxAmount": 0.0,
  "defaultNationTaxEnabled": false,
  "defaultNationTaxType": "FLAT",
  "defaultNationTaxAmount": 0.0,
  "upkeepTimeOfDay": "18:00",
  "reclaimFeeMultiplier": 2.0,
  "ruinedDeletionDays": 30,
  "maxNationTaxPercent": 10.0,
  "maxTownTaxPercent": 25.0,
  "maxPlotTaxPercent": 50.0,
  "taxWarningMinutesBefore": 30,
  "taxWarningMessage": "&e[Tax] Taxes will be collected in {time}!",
  "taxSummaryMessageEnabled": true,
  "maxNationDebtMultiplier": 5.0,
  "allowTownLeaveWithDebt": false
}
```

## Languages / Translations

Towny3D supports multiple languages through translation files. Users can create and share their own language packs.

### How Translations Work

On first startup, Towny3D automatically extracts translation files to:
```
mods/it.cava99.towny3d_Towny3D/Server/Languages/{locale}/
```

You can edit these files directly to customize messages without rebuilding the plugin.

### Default Language

The default language is set in `towny-config.json`:
```json
{
  "defaultLanguage": "en-US"
}
```

### Changing Language

Administrators can change the server language without restart:

```
/ta setlang <locale>
```

**Permission:** `towny3d.admin`

**Examples:**
- `/ta setlang en-US` - English
- `/ta setlang it-IT` - Italian
- `/ta setlang es-ES` - Spanish (if you create it)

### Editing Translation Files

Translation files are located at:
```
mods/it.cava99.towny3d_Towny3D/Server/Languages/{locale}/
```

Each language has multiple files:
- `commands.lang` - Command-related messages
- `taxes.lang` - Tax and upkeep messages
- `protection.lang` - Protection and warning messages
- `economy.lang` - Economy-related messages

#### Translation File Format

Translation files use a simple `key = value` format:

```
# Comments start with #
key = Translated text with {placeholders}
```

#### Example

**Original (en-US/commands.lang):**
```
town.created = Town '{name}' created successfully!
town.name_taken = Town name '{name}' is already taken
town.joined = You have joined '{town}'!
```

**Italian translation (it-IT/commands.lang):**
```
town.created = Città '{name}' creata con successo!
town.name_taken = Il nome della città '{name}' è già in uso
town.joined = Sei entrato in '{town}'!
```

#### Creating a New Language

1. Create a new directory in `Server/Languages/` with your locale code:
   ```
   mkdir "mods/it.cava99.towny3d_Towny3D/Server/Languages/es-ES"
   ```

2. Copy translation files from `en-US` to your new directory:
   ```
   copy mods\it.cava99.towny3d_Towny3D\Server\Languages\en-US\*.lang mods\it.cava99.towny3d_Towny3D\Server\Languages\es-ES\
   ```

3. Edit the files to translate the messages

4. Restart the server or use `/ta setlang es-ES`

#### Customizing Existing Translations

Simply edit the `.lang` files in your language directory. Changes take effect on:
- Next server restart, OR
- When you use `/ta setlang <locale>` to reload the language

### Available Translations

- **en-US** - English (default)
- **it-IT** - Italian

### Translation File Structure

```
Server/Languages/
├── en-US/
│   ├── commands.lang    # All command messages
│   ├── taxes.lang       # Tax/upkeep messages
│   ├── protection.lang  # Protection warnings
│   └── economy.lang     # Economy messages
└── it-IT/
    ├── commands.lang
    ├── taxes.lang
    ├── protection.lang
    └── economy.lang
```

## Key Features

### 3D Claiming System

Towny3D introduces true 3D land claims - you can have multiple plots in the same chunk at different vertical levels! Simply select the lower and upper points of your desired plot area using the claim stick, then run the claim command.

*   **Vertical granularity**: Claim specific Y-axis ranges rather than entire chunks
*   **Spatial indexing**: Fast region lookups with optimized spatial cache
*   **Claim Stick**: Use `/claim gcs` (get claim stick) to set plot boundaries

### Implemented Features

#### Town Management

*   `/town create <townName>` - Create a new town
*   `/town delete` - Disband your town
*   `/town info [townName]` - View town information
*   `/town list` - List all towns
*   `/town join <townName>` - Join an open town
*   `/town leave` - Leave your current town
*   `/town mayor` - Mayor management submenu
*   `/town addAssistant <playerName>` - Add town assistant
*   `/town removeAssistant <playerName>` - Remove town assistant
*   `/town setHome` - Set town spawn point
*   `/town spawn` - Teleport to town spawn
*   `/town online` - List online residents
*   `/town here` - Show info for town you're standing in
*   `/town kick <playerName>` - Kick resident from town
*   `/town deposit <amount>` - Deposit to town bank
*   `/town withdraw <amount>` - Withdraw from town bank

#### Nation Management

*   `/nation create <nationName>` - Create a new nation
*   `/nation delete` - Disband your nation
*   `/nation info [nationName]` - View nation information
*   `/nation list` - List all nations
*   `/nation join <nationName>` - Join an open nation
*   `/nation leave` - Leave your current nation
*   `/nation invite <townName>` - Invite a town to your nation
*   `/nation kick <townName>` - Kick a town from your nation

#### Plot Management

*   `/plot claim` - Claim the plot you're standing in
*   `/plot unclaim` - Unclaim the plot you're standing in
*   `/plot forsale <price>` (alias: `/plot fs`) - Put a plot up for sale
*   `/plot notforsale` (alias: `/plot nfs`) - Remove plot from market
*   `/plot info` - View plot information
*   `/plot name <name>` - Name your plot
*   `/plot showBorders` (alias: `/plot sb`) - Show plot borders visually
*   `/plot hideBorders` (alias: `/plot hb`) - Hide plot borders
*   `/plot toggle <flag> [on/off]` - Toggle plot flags (pvp, explosion, fire)
*   `/plot set perm <permGroup> <permType> <value>` - Set plot permissions
*   `/plot set embassy` - Mark plot as embassy

#### Claiming Commands

*   `/claim gcs` (get claim stick) - Get the item to set lower and upper plot points
*   `/claim land` - Claim land at selected 3D region
*   `/claim auto` - Automatically claim adjacent land
*   `/claim unclaim` - Unclaim town land

#### Selection Commands (WorldEdit-style)

*   `/p1` - Set first position (lower corner) for region selection
*   `/p2` - Set second position (upper corner) for region selection
*   `/pos1` - Alias for `/p1`
*   `/pos2` - Alias for `/p2`

### Claim Stick Usage

The claim stick (`Ingredient_Stick`) is used to select regions for claiming:

1.  **Left-click** on a block to set Position 1
2.  Press **F key** while looking at another block to set Position 2
3.  Run `/claim land` to claim the selected region

**Note:** Hytale uses the F key for secondary interactions, not right-click. The claim stick works with Hytale's `UseBlockEvent.Pre` system which correctly maps:
- `InteractionType.Primary` (0) = Left click -> Position 1
- `InteractionType.Use` (5) = F key -> Position 2

#### Selection Visualization

When both positions are set, you'll see:
-   Selection volume coordinates
-   Selection size (dimensions)
-   Estimated claim cost

#### Town Toggles

*   `/toggle pvp [on/off]` - Toggle PvP in town
*   `/toggle public [on/off]` - Toggle public status
*   `/toggle explosions [on/off]` - Toggle explosion protection
*   `/toggle open [on/off]` - Toggle open joining

#### Chat System

*   `/tc <message>` (alias: `/townchat`) - Send message to town chat
*   `/nc <message>` (alias: `/nationchat`) - Send message to nation chat
*   `/g <message>` (alias: `/globalchat`) - Send message to global chat
*   `/chat town|nation|global` - Switch default chat mode

#### Economy

*   `/balance` (alias: `/bal`) - Check your balance (only with internal economy)
*   `/resident balance` - Check resident balance
*   `/town balance` - Check town bank balance
*   `/nation balance` - Check nation bank balance
*   `/eco give <player> <amount>` - Give money to player
*   `/eco take <player> <amount>` - Take money from player
*   `/eco set <player> <amount>` - Set player balance
*   `/eco balance <player>` - Check player balance

> **Note:** Economy commands are only available when using Towny3D's internal economy. If an external economy (like Cassaforte's built-in or another plugin) is available, use that economy's commands instead. Towny3D automatically falls back to internal economy if no external economy is available.

#### Upkeep and Taxation

Towny3D features a comprehensive upkeep and taxation system to maintain economic balance and encourage active participation.

**Tax Types:**

*   **Town Upkeep** - Periodic cost towns pay to the server (configurable, defaults to disabled)
*   **Nation Upkeep** - Periodic cost nations pay to the server (configurable, defaults to disabled)
*   **Nation Taxes** - Nations can tax member towns (flat amount or percentage of town balance)
*   **Town Taxes** - Towns can tax residents (flat amount or percentage of resident balance)
*   **Plot Taxes** - Towns can set taxes on individual plots (flat amount or percentage)

**Tax Commands:**

*   `/town tax <flat|percent> <amount>` - Set resident tax for your town
*   `/town tax clear` - Clear resident tax
*   `/town tax info` - View town tax settings
*   `/nation tax <flat|percent> <amount>` - Set tax on member towns
*   `/nation tax clear` - Clear nation tax
*   `/nation tax info` - View nation tax settings
*   `/nation debt [town]` - View town debts to nation
*   `/plot tax <flat|percent> <amount>` - Set tax for current plot
*   `/plot tax clear` - Clear plot tax
*   `/plot tax info` - View plot tax info
*   `/tax` - Show your personal tax burden
*   `/tax town <name>` - View town tax info
*   `/tax nation <name>` - View nation tax info

**Admin Tax Commands:**

*   `/ta collecttaxes` - Force immediate tax collection
*   `/ta nexttax` - Show when next collection occurs
*   `/ta reclaim <town|nation> <name>` - Reclaim a ruined entity
*   `/ta ruinate <town|nation> <name>` - Force entity into ruined state
*   `/ta settaxtime <HH:mm>` - Change tax collection time
*   `/ta toggle upkeep` - Enable/disable upkeep system

**Consequences:**

*   **Town/Nation can't pay upkeep** → Falls to ruins (can be reclaimed for a fee)
*   **Resident can't pay plot tax** → Loses plot ownership
*   **Resident can't pay resident tax** → Payment skipped (no debt)
*   **Town can't pay nation tax** → Goes into debt, blocked from leaving nation

#### Invitations

*   `/accept` - Accept pending town/nation invitation
*   `/decline` - Decline pending invitation

#### Admin Commands (`/townyadmin`)

*   `/ta reload` - Reload plugin configuration
*   `/ta save` - Force save all data
*   `/ta stats` - View plugin statistics
*   `/ta unclaim [radius]` - Admin force unclaim
*   `/ta reset <player>` - Reset player's town data
*   `/ta setlang <locale>` - Change server language
*   `/ta reconcile` - Fix town membership data inconsistencies

#### Resident Commands

*   `/res` (alias: `/resident`) - Resident command root
*   `/res info [player]` - View resident information
*   `/res list` - List all residents
*   `/balance` - Check balance

### Protection Systems

*   **Entity Protection**: Control entity spawning and interactions
*   **Explosion Protection**: Prevent damage from explosions
*   **Player Interactions**: Control block placement, breaking, and item usage
*   **PvP Control**: Fine-grained PvP toggle per region
*   **Chunk Enter Notifications**: Display town/region info on entry

## Languages / Translations

Towny3D supports multiple languages through translation files. Users can create and share their own language packs.

### Default Language

The default language is set in the server config (`config.json`):

```json
{
  "defaultLanguage": "en-US"
}
```

### Changing Language

Administrators can change the server language without restart:

```
/ta setlang <locale>
```

**Permission:** `towny3d.admin`

**Examples:**
- `/ta setlang en-US` - English
- `/ta setlang it-IT` - Italian

### Creating Language Packs

Create translation files in `Server/Languages/{locale}/`:

```
Server/Languages/
├── en-US/
│   ├── commands.lang
│   ├── taxes.lang
│   └── protection.lang
└── your-locale/
    ├── commands.lang
    ├── taxes.lang
    └── protection.lang
```

Translation files use the format:
```
key = Translated text with {placeholders}
```

Example:
```
town.created = Town '{name}' created successfully!
town.name_taken = Town name '{name}' is already taken
```

### Available Translations

- **en-US** - English (default)
- **it-IT** - Italian

## Planned Features (Not Yet Implemented)

### Town Commands

*   `/town set perm <permGroup> <permType> <value>` - Set town permissions

### Plot Commands

*   `/plot reset` - Reset plot to default settings

### Town Permission & Flag System

The flag system is partially implemented for plot toggling. Permission groups are planned.

**Implemented Flags (via `/plot toggle`):**

*   `pvp` - Player vs Player combat
*   `explosion` - Explosion damage
*   `fire` - Fire spread and damage

**Planned Flags:**

*   `friendly_mobs_spawn` - Friendly mob spawning
*   `hostile_mobs_spawn` - Hostile mob spawning

**Planned Permissions (for groups: res/ally/gen):**

*   `pvp` - Player vs Player combat
*   `inventory` - Inventory access
*   `interact` - Block interaction
*   `build` - Place blocks
*   `destroy` - Break blocks
*   `drop` - Drop items
*   `pick` - Pick up items
*   `move` - Movement

### Admin Features

*   `/townyadmin chat spy` - Spy on town/nation chats

## Permissions

All Towny3D permissions use the `towny3d.` prefix. Permissions are checked automatically by the Hytale server's permission system.

### Default Permissions

| Permission Node | Description | Default |
|----------------|-------------|---------|
| `towny3d.admin` | Full admin access to all Towny3D commands | OP only |
| `towny3d.*` | Wildcard for all Towny3D permissions | OP only |

### Command Permissions

#### General Commands

| Permission | Description |
|------------|-------------|
| `towny3d.balance` | Check account balance |
| `towny3d.resident` | Base resident command access |
| `towny3d.resident.info` | View resident information |
| `towny3d.resident.list` | List all residents |
| `towny3d.resident.balance` | Check resident balance |

#### Town Commands (`/town`, `/t`)

| Permission | Description |
|------------|-------------|
| `towny3d.town` | Base town command access |
| `towny3d.town.create` | Create a new town |
| `towny3d.town.delete` | Delete your town |
| `towny3d.town.join` | Join a town |
| `towny3d.town.leave` | Leave your town |
| `towny3d.town.list` | List all towns |
| `towny3d.town.info` | View town information |
| `towny3d.town.claim` | Claim land for town |
| `towny3d.town.unclaim` | Unclaim town land |
| `towny3d.town.set` | Change town settings |
| `towny3d.town.toggle` | Toggle town flags |
| `towny3d.town.deposit` | Deposit to town bank |
| `towny3d.town.withdraw` | Withdraw from town bank |
| `towny3d.town.kick` | Kick residents from town |
| `towny3d.town.invite` | Invite players to town |
| `towny3d.town.add_assistant` | Promote resident to assistant |
| `towny3d.town.remove_assistant` | Demote assistant to resident |
| `towny3d.town.spawn` | Teleport to town spawn |
| `towny3d.town.set_home` | Set town spawn location |
| `towny3d.town.online` | List online town members |
| `towny3d.town.here` | Show town at current location |
| `towny3d.town.tax` | Access town tax commands |
| `towny3d.town.balance` | View town bank balance |

#### Nation Commands (`/nation`)

| Permission | Description |
|------------|-------------|
| `towny3d.nation` | Base nation command access |
| `towny3d.nation.create` | Create a new nation |
| `towny3d.nation.delete` | Delete your nation |
| `towny3d.nation.join` | Join a nation with your town |
| `towny3d.nation.leave` | Leave your nation |
| `towny3d.nation.list` | List all nations |
| `towny3d.nation.info` | View nation information |
| `towny3d.nation.kick` | Kick town from nation |
| `towny3d.nation.invite` | Invite town to nation |
| `towny3d.nation.tax` | Access nation tax commands |
| `towny3d.nation.debt` | View town debts to nation |
| `towny3d.nation.balance` | View nation bank balance |

#### Plot Commands (`/plot`)

| Permission | Description |
|------------|-------------|
| `towny3d.plot` | Base plot command access |
| `towny3d.plot.claim` | Claim a plot |
| `towny3d.plot.unclaim` | Unclaim a plot |
| `towny3d.plot.forsale` | Put plot up for sale |
| `towny3d.plot.notforsale` | Remove plot from market |
| `towny3d.plot.set` | Set plot permissions (collection) |
| `towny3d.plot.set.perm` | Set plot permissions |
| `towny3d.plot.set.reset` | Reset plot to default settings |
| `towny3d.plot.toggle` | Toggle plot flags |
| `towny3d.plot.name` | Name a plot |
| `towny3d.plot.show_borders` | Show plot boundary markers |
| `towny3d.plot.hide_borders` | Hide plot boundary markers |
| `towny3d.plot.info` | View plot information |
| `towny3d.plot.tax` | Access plot tax commands |

#### Claim Commands (`/claim`)

| Permission | Description |
|------------|-------------|
| `towny3d.claim` | Base claim command access |
| `towny3d.claim.stick` | Get claim stick tool |
| `towny3d.claim.auto` | Toggle automatic claiming |
| `towny3d.claim.unclaim` | Unclaim land at position |
| `towny3d.claim.land` | Manual land claim |

#### Chat Commands (`/chat`, `/tc`, `/nc`, `/g`)

| Permission | Description |
|------------|-------------|
| `towny3d.chat` | Base chat command access |
| `towny3d.chat.town` | Use town chat channel |
| `towny3d.chat.nation` | Use nation chat channel |
| `towny3d.chat.global` | Switch to global chat |

#### Invite Commands (`/invite`, `/accept`, `/decline`)

| Permission | Description |
|------------|-------------|
| `towny3d.invite` | Base invite command access |
| `towny3d.invite.accept` | Accept pending invitations |
| `towny3d.invite.decline` | Decline pending invitations |
| `towny3d.invite.list` | List pending invitations |
| `towny3d.invite.send` | Send invitations |

#### Toggle Commands (`/toggle`)

| Permission | Description |
|------------|-------------|
| `towny3d.toggle` | Base toggle command access |
| `towny3d.toggle.pvp` | Toggle PvP protection |
| `towny3d.toggle.explosions` | Toggle explosion protection |
| `towny3d.toggle.public` | Toggle public access |
| `towny3d.toggle.open` | Toggle open status |

#### Tax Commands (`/tax`)

| Permission | Description |
|------------|-------------|
| `towny3d.tax` | Base tax command access |

#### Admin Commands (`/ta`)

| Permission | Description |
|------------|-------------|
| `towny3d.admin` | All admin commands (use this for full admin access) |
| `towny3d.admin.reload` | Reload plugin configuration |
| `towny3d.admin.save` | Force save all data |
| `towny3d.admin.stats` | View server statistics |
| `towny3d.admin.unclaim` | Admin force unclaim |
| `towny3d.admin.reset` | Reset town/player data |
| `towny3d.admin.setlang` | Change server language |
| `towny3d.admin.chat` | Admin chat options |
| `towny3d.admin.spy` | Spy on chat channels |
| `towny3d.admin.reconcile` | Fix town membership data inconsistencies |
| `towny3d.admin.collecttaxes` | Force immediate tax collection |
| `towny3d.admin.nexttax` | Show next tax collection time |
| `towny3d.admin.reclaim` | Reclaim ruined towns/nations |
| `towny3d.admin.ruinate` - Force entities into ruined state |
| `towny3d.admin.settaxtime` | Change tax collection time |
| `towny3d.admin.toggle_upkeep` | Enable/disable upkeep system |

#### Economy Admin Commands (`/eco`)

| Permission | Description |
|------------|-------------|
| `towny3d.eco` | Base economy admin access |
| `towny3d.eco.give` | Give money to player |
| `towny3d.eco.take` | Take money from player |
| `towny3d.eco.set` | Set player balance |
| `towny3d.eco.balance` | Check player balance |

### Setting Permissions

Permissions in Hytale are typically configured through the server's permission system or a permissions plugin. Configure permissions according to your chosen permission plugin's format.

For basic setups, ensure that:
- All players have access to basic commands: `towny3d.town`, `towny3d.town.info`, `towny3d.town.join`, `towny3d.resident`, `towny3d.balance`
- Mayors/leaders have creation and management permissions: `towny3d.town.create`, `towny3d.town.claim`, `towny3d.plot`, `towny3d.toggle`
- Admins have full access: `towny3d.admin` or `towny3d.*`


## Architecture

### Performance Optimizations

*   **Kotlin Coroutines**: Asynchronous operations for database I/O and network calls
*   **Caffeine Caching**: High-performance caching for towns, nations, residents, and regions
*   **Spatial Indexing**: Efficient 3D region lookups with spatial cache
*   **Batch Storage**: Periodic batch writes to reduce I/O operations

### Storage

*   JSON-based storage backend
*   Automatic saving with configurable intervals
*   Batch writer for efficient writes

## Development Status

This is an **alpha** release. Many core features work, but expect bugs and missing functionality. The plugin is being developed without official Hytale documentation, relying on decompiled server code.

### Known Limitations

*   API may change significantly as Hytale server updates
*   Some features from original Towny not yet ported
*   Testing limited by available server versions

## Contributing

This is a personal project developed to bring Towny-like functionality to Hytale. Contributions welcome once the plugin stabilizes.

## Credits

*   Inspired by [Towny3D](https://www.spigotmc.org/resources/towny3d.64829/) for Minecraft/Spigot of which I am the author
*   Ported to Hytale API through reverse engineering