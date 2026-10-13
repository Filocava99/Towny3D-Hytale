package it.cava99.towny3d.gui

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo
import com.hypixel.hytale.server.core.ui.LocalizableString
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.gui.common.stringField
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * GUI for displaying town information with quick actions.
 * Opens when player runs /town info command.
 *
 * This file is intentionally large because:
 * - It represents a single cohesive UI screen with multiple interactive elements
 * - The TownInfoData inner class contains codec definitions that must stay with the GUI
 * - UI building logic (build method) and event handling (handleDataEvent) are tightly coupled
 * - Splitting would require passing substantial context between files, reducing clarity
 *
 * Future improvements:
 * - Consider extracting TownInfoData to a separate file if codec definitions grow significantly
 * - Could extract common GUI utilities (permission checks, formatting) to helper functions
 */
class TownInfoGui(
    private val myPlayerRef: PlayerRef,
    town: Town,
    private val viewerResidentId: ResidentId? = null
) : InteractiveCustomUIPage<TownInfoGui.TownInfoData>(myPlayerRef, CustomPageLifetime.CanDismiss, TownInfoData.CODEC) {

    private val plugin = Towny3D.instance
    private var town: Town = town  // Make var so we can refresh from cache
    private var isMayor = false
    private var isAssistant = false
    private var canManage = false  // isMayor || isAssistant
    private var world: com.hypixel.hytale.server.core.universe.world.World? = null
    private var playerEntityRef: Ref<EntityStore>? = null

    // Tab state
    private var currentTab: String = "info"  // "info", "settings", "residents"

    // Residents tab state
    private var currentPage: Int = 0
    private val residentsPerPage: Int = 20
    private var selectedResidentId: ResidentId? = null

    // PageManager for GUI-to-GUI navigation - obtained dynamically in handlers

    override fun build(
        ref: Ref<EntityStore>,
        uiCommandBuilder: UICommandBuilder,
        uiEventBuilder: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        // Store world and player ref for later use in handleDataEvent
        this.world = store.getExternalData().getWorld()
        this.playerEntityRef = ref

        // Check viewer permissions
        isMayor = town.mayorId == viewerResidentId
        isAssistant = viewerResidentId != null && town.assistantIds.contains(viewerResidentId)

        // Load UI file
        uiCommandBuilder.append("Pages/HytaleTowny_TownInfo.ui")

        // Add button text descriptions (TextButton does not support tooltips)
        uiCommandBuilder.set("#DepositButton.Text", "Deposit")
        uiCommandBuilder.set("#ExpandButton.Text", "Expand")

        // Set town information
        uiCommandBuilder.set("#TownName.Text", town.name)

        // Get mayor name
        val mayorName = plugin.residentCache.getSync(town.mayorId)?.name ?: "Unknown"
        uiCommandBuilder.set("#MayorName.Text", mayorName)

        // Set resident count
        uiCommandBuilder.set("#ResidentCount.Text", "${town.residentIds.size}")

        // Set balance
        val balanceFormatted = "$%.2f".format(town.balance)
        uiCommandBuilder.set("#Balance.Text", balanceFormatted)

        // Set layers
        uiCommandBuilder.set("#Layers.Text", "${town.layers} / ${town.maxLayers}")

        // Set nation
        if (town.hasNation && town.nationId != null) {
            val nation = plugin.nationCache.getSync(town.nationId!!)
            if (nation != null) {
                uiCommandBuilder.set("#NationRow.Visible", true)
                uiCommandBuilder.set("#NationName.Text", nation.name)
            } else {
                uiCommandBuilder.set("#NationRow.Visible", false)
            }
        } else {
            uiCommandBuilder.set("#NationRow.Visible", false)
        }

        // Set status badges
        uiCommandBuilder.set("#OpenStatus.Text", if (town.isOpen) "Open" else "Closed")
        uiCommandBuilder.set("#PublicStatus.Text", if (town.isPublic) "Public" else "Private")

        // Set board - always show section with placeholder when empty
        if (town.board.isNotBlank()) {
            uiCommandBuilder.set("#BoardText.Text", town.board)
        } else {
            uiCommandBuilder.set("#BoardText.Text", "No board message set.")
        }
        uiCommandBuilder.set("#BoardSection.Visible", true)

        // Set button states based on permissions
        canManage = isMayor || isAssistant

        uiCommandBuilder.set("#DepositButton.Disabled", false)
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#DepositButton",
            EventData.of("Deposit", "open"),
            false
        )

        uiCommandBuilder.set("#ExpandButton.Disabled", !isMayor)
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ExpandButton",
            EventData.of("ExpandLayers", "true"),
            false
        )

        uiCommandBuilder.set("#SpawnButton.Disabled", false)
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SpawnButton",
            EventData.of("Spawn", "true"),
            false
        )

        uiCommandBuilder.set("#SetSpawnButton.Disabled", !isMayor)
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SetSpawnButton",
            EventData.of("SetSpawn", "true"),
            false
        )

        uiCommandBuilder.set("#SettingsButton.Disabled", !canManage)
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SettingsButton",
            EventData.of("SwitchTab", "settings"),
            false
        )

        uiCommandBuilder.set("#ResidentsButton.Disabled", false)
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ResidentsButton",
            EventData.of("SwitchTab", "residents"),
            false
        )

        uiCommandBuilder.set("#PlotsButton.Disabled", !canManage)
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PlotsButton",
            EventData.of("OpenPlots", "true"),
            false
        )

        // Tab event bindings
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#InfoTab",
            EventData.of("SwitchTab", "info"),
            false
        )
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SettingsTab",
            EventData.of("SwitchTab", "settings"),
            false
        )
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ResidentsTab",
            EventData.of("SwitchTab", "residents"),
            false
        )

        // Settings tab - setup event bindings only (values populated when tab is switched to)
        setupSettingsTabEvents(uiEventBuilder, canManage)

        // Residents tab - initialize event bindings only (list populated on tab switch)
        initializeResidentsTab(uiEventBuilder)
    }

    /**
     * Sets up event bindings for Settings tab (runs once in build).
     */
    private fun setupSettingsTabEvents(ev: UIEventBuilder, canManage: Boolean) {
        // Event bindings for toggles (ValueChanged fires on toggle click)
        ev.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#OpenToggle",
            EventData.of("ToggleOpen", "changed"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#PublicToggle",
            EventData.of("TogglePublic", "changed"),
            false
        )

        // Board field binding
        ev.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#BoardField",
            EventData.of("@BoardChange", "#BoardField.Value"),
            false
        )

        // Tax amount field binding
        ev.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#TaxAmountField",
            EventData.of("@TaxChange", "#TaxAmountField.Value"),
            false
        )

        // Tax type dropdown binding
        ev.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#TaxTypeDropdown",
            EventData.of("@TaxTypeChange", "#TaxTypeDropdown.Value"),
            false
        )

        // Save/Cancel buttons
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SaveSettingsButton",
            EventData.of("SaveSettings", "true"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelSettingsButton",
            EventData.of("CancelSettings", "true"),
            false
        )
    }

    /**
     * Populates Settings tab values.
     * Called during build() to set initial values on hidden elements.
     * Elements become visible when tab is switched to.
     */
    private fun populateSettingsTab(ui: UICommandBuilder, canManage: Boolean) {
        // Set current values (use local town object - already fresh when GUI opened)
        ui.set("#OpenToggle.Value", town.isOpen)
        ui.set("#PublicToggle.Value", town.isPublic)
        // TextField values - ensure non-null strings
        val boardValue = if (town.board.isNotBlank()) town.board else ""
        ui.set("#BoardField.Value", boardValue)
        ui.set("#TaxAmountField.Value", town.taxToResidents.amount.toString())

        // Populate tax type dropdown
        val taxTypes = listOf(
            DropdownEntryInfo(LocalizableString.fromString("Flat"), "FLAT"),
            DropdownEntryInfo(LocalizableString.fromString("Percentage"), "PERCENTAGE")
        )
        ui.set("#TaxTypeDropdown.Entries", taxTypes)
        // Set current tax type selection
        val currentType = when (town.taxToResidents.type) {
            it.cava99.towny3d.model.TaxType.FLAT -> "FLAT"
            it.cava99.towny3d.model.TaxType.PERCENTAGE -> "PERCENTAGE"
        }
        ui.set("#TaxTypeDropdown.Value", currentType)

        // Show permission notice if not allowed to edit
        ui.set("#PermissionNotice.Visible", !canManage)

        // Disable fields if not allowed to edit (Toggles support Disabled)
        ui.set("#OpenToggle.Disabled", !canManage)
        ui.set("#PublicToggle.Disabled", !canManage)
        // Note: TextField/NumberField/Dropdown don't support .Disabled property
    }

    /**
     * Initializes the Residents tab with event bindings.
     */
    private fun initializeResidentsTab(ev: UIEventBuilder) {
        // Pagination buttons
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PrevPageButton",
            EventData.of("PrevPage", "true"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#NextPageButton",
            EventData.of("NextPage", "true"),
            false
        )

        // Action buttons
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PromoteButton",
            EventData.of("PromoteResident", "true"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#DemoteButton",
            EventData.of("DemoteResident", "true"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#KickButton",
            EventData.of("KickResident", "true"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TransferMayorButton",
            EventData.of("TransferMayor", "true"),
            false
        )
    }

    /**
     * Updates the visibility of tab content based on currentTab.
     */
    private fun updateTabVisibility(ui: UICommandBuilder) {
        ui.set("#InfoContent.Visible", currentTab == "info")
        ui.set("#SettingsContent.Visible", currentTab == "settings")
        ui.set("#ResidentsContent.Visible", currentTab == "residents")
    }

    /**
     * Refreshes town data from cache and updates info tab display.
     * Call this when switching back to info tab to show updated data.
     */
    private fun refreshTownAndUpdateInfoTab(ui: UICommandBuilder) {
        // Refresh town from cache to get latest data
        try {
            val refreshedTown = runBlocking(Dispatchers.Default) {
                plugin.townCache.get(town.id)
            }
            if (refreshedTown != null) {
                town = refreshedTown
                // Update board display
                if (refreshedTown.board.isNotBlank()) {
                    ui.set("#BoardText.Text", refreshedTown.board)
                } else {
                    ui.set("#BoardText.Text", "No board message set.")
                }

                // Update status badges
                ui.set("#OpenStatus.Text", if (refreshedTown.isOpen) "Open" else "Closed")
                ui.set("#PublicStatus.Text", if (refreshedTown.isPublic) "Public" else "Private")

                // Update layers display
                ui.set("#Layers.Text", "${refreshedTown.layers} / ${refreshedTown.maxLayers}")

                // Update balance
                val balanceFormatted = "$%.2f".format(refreshedTown.balance)
                ui.set("#Balance.Text", balanceFormatted)

                // Update resident count
                ui.set("#ResidentCount.Text", "${refreshedTown.residentIds.size}")
            }
        } catch (e: Exception) {
            TownyLog.error("Failed to refresh town data for info tab", e)
        }
    }

    /**
     * Builds the residents list for current page.
     */
    private fun buildResidentsList(ui: UICommandBuilder) {
        val allResidents = town.residentIds.toList()
        val totalPages = maxOf(1, (allResidents.size + residentsPerPage - 1) / residentsPerPage)

        // Clamp current page
        if (currentPage >= totalPages) currentPage = maxOf(0, totalPages - 1)
        if (currentPage < 0) currentPage = 0

        val startIdx = currentPage * residentsPerPage
        val endIdx = minOf(startIdx + residentsPerPage, allResidents.size)
        val pageResidents = if (startIdx < allResidents.size) allResidents.subList(startIdx, endIdx) else emptyList()

        // Update page info
        ui.set("#ResidentPageInfo.Text", "${currentPage + 1}/$totalPages")

        // Clear and build resident entries
        // Note: UI elements are statically defined, we just update their visibility and content
        for (i in 0 until residentsPerPage) {
            val entryId = "#ResidentEntry$i"
            if (i < pageResidents.size) {
                val residentId = pageResidents[i]
                val resident = plugin.residentCache.getSync(residentId)
                val name = resident?.name ?: "Unknown"

                // Determine role
                val role = when {
                    residentId == town.mayorId -> "MAYOR"
                    town.assistantIds.contains(residentId) -> "ASSISTANT"
                    else -> "RESIDENT"
                }

                val roleColor = when (role) {
                    "MAYOR" -> "#f59e0b"
                    "ASSISTANT" -> "#3b82f6"
                    else -> "#6b7280"
                }

                ui.set("$entryId.Visible", true)
                ui.set("$entryId #ResidentName.Text", name)
                ui.set("$entryId #RoleBadge.Text", role)
                ui.set("$entryId #RoleBadge.Background", "(Color: $roleColor(0.3))")
                ui.set("$entryId #RoleBadge.OutlineColor", roleColor)

                // Bind click to select
                // Note: Event binding added separately in build()
            } else {
                ui.set("$entryId.Visible", false)
            }
        }

        // Update pagination buttons
        ui.set("#PrevPageButton.Disabled", currentPage == 0)
        ui.set("#NextPageButton.Disabled", currentPage >= totalPages - 1)

        // Update action buttons visibility based on selection
        ui.set("#ResidentActions.Visible", selectedResidentId != null)
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: TownInfoData) {
        super.handleDataEvent(ref, store, data)

        val currentWorld = world
        val playerRef = playerEntityRef

        // Handle tab switching first
        if (data.switchTab != null) {
            currentTab = data.switchTab!!

            when (currentTab) {
                "info" -> {
                    // Update visibility and refresh data
                    val ui = UICommandBuilder()
                    updateTabVisibility(ui)
                    runBlocking(Dispatchers.Default) {
                        refreshTownAndUpdateInfoTab(ui)
                    }
                    sendUpdate(ui, UIEventBuilder(), false)
                }
                "settings" -> {
                    // First update: make settings visible (elements get instantiated)
                    val visibilityUi = UICommandBuilder()
                    updateTabVisibility(visibilityUi)
                    sendUpdate(visibilityUi, UIEventBuilder(), false)

                    // Second update: populate values (now elements exist)
                    val valuesUi = UICommandBuilder()
                    populateSettingsTab(valuesUi, canManage)
                    sendUpdate(valuesUi, UIEventBuilder(), false)
                }
                "residents" -> {
                    val ui = UICommandBuilder()
                    updateTabVisibility(ui)
                    buildResidentsList(ui)
                    sendUpdate(ui, UIEventBuilder(), false)
                }
            }
            return
        }

        // Handle settings toggles (flip on click, immediate save)
        if (data.toggleOpen != null) {
            handleToggleOpen(!town.isOpen)
            return
        }

        if (data.togglePublic != null) {
            handleTogglePublic(!town.isPublic)
            return
        }

        // Handle save/cancel settings
        if (data.saveSettings != null) {
            handleSaveSettings()
            return
        }

        if (data.cancelSettings != null) {
            // Switch back to info tab
            currentTab = "info"
            val ui = UICommandBuilder()
            updateTabVisibility(ui)
            sendUpdate(ui, UIEventBuilder(), false)
            return
        }

        // Handle pagination
        if (data.prevPage != null) {
            if (currentPage > 0) currentPage--
            selectedResidentId = null
            val ui = UICommandBuilder()
            buildResidentsList(ui)
            sendUpdate(ui, UIEventBuilder(), false)
            return
        }

        if (data.nextPage != null) {
            val totalPages = (town.residentIds.size + residentsPerPage - 1) / residentsPerPage
            if (currentPage < totalPages - 1) currentPage++
            selectedResidentId = null
            val ui = UICommandBuilder()
            buildResidentsList(ui)
            sendUpdate(ui, UIEventBuilder(), false)
            return
        }

        // Handle resident selection
        if (data.selectResident != null) {
            val idx = data.selectResident!!.toIntOrNull()
            if (idx != null && idx in 0 until residentsPerPage) {
                val allResidents = town.residentIds.toList()
                val startIdx = currentPage * residentsPerPage
                val actualIdx = startIdx + idx
                if (actualIdx < allResidents.size) {
                    selectedResidentId = allResidents[actualIdx]
                }
            }
            val ui = UICommandBuilder()
            buildResidentsList(ui)
            sendUpdate(ui, UIEventBuilder(), false)
            return
        }

        // Handle resident actions
        if (data.promoteResident != null) {
            handlePromoteResident()
            return
        }

        if (data.demoteResident != null) {
            handleDemoteResident()
            return
        }

        if (data.kickResident != null) {
            handleKickResident()
            return
        }

        if (data.transferMayor != null) {
            handleTransferMayor()
            return
        }

        if (data.boardChange != null) {
            handleBoardChange(data.boardChange)
            return
        }

        if (data.taxChange != null) {
            handleTaxChange(data.taxChange)
            return
        }

        if (data.taxTypeChange != null) {
            handleTaxTypeChange(data.taxTypeChange)
            return
        }

        if (data.deposit != null) {
            // Open deposit dialog via PageManager
            val residentId = viewerResidentId
            if (residentId != null) {
                try {
                    val player = store.getComponent(ref, Player.getComponentType())
                    if (player != null) {
                        // Don't call close() before openCustomPage() - it breaks event handling
                        player.getPageManager().openCustomPage(ref, store, DepositDialogGui(myPlayerRef, town, residentId))
                        return
                    }
                } catch (e: Exception) {
                    TownyLog.warn("Could not open deposit dialog: ${e.message}")
                }
            }
            close()
            return
        }

        if (data.expandLayers != null) {
            // Open layer purchase dialog via PageManager
            val residentId = viewerResidentId
            if (residentId != null) {
                try {
                    val player = store.getComponent(ref, Player.getComponentType())
                    if (player != null) {
                        // Don't call close() before openCustomPage() - it breaks event handling
                        player.getPageManager().openCustomPage(ref, store, LayerPurchaseDialog(myPlayerRef, town, residentId))
                        return
                    }
                } catch (e: Exception) {
                    TownyLog.warn("Could not open layer purchase dialog: ${e.message}")
                }
            }
            close()
            return
        }

        if (data.spawn != null) {
            // Teleport to town spawn
            val homeLocation = town.homeLocation
            if (homeLocation == null) {
                // Town has no spawn set - close UI and show message via chat
                close()
                // Message will be shown after UI closes
                return
            }

            if (currentWorld != null && playerRef != null) {
                currentWorld.execute {
                    try {
                        val position = org.joml.Vector3d(
                            homeLocation.x.toDouble(),
                            homeLocation.y.toDouble(),
                            homeLocation.z.toDouble()
                        )

                        // Create Teleport component with proper rotation
                        val teleport = com.hypixel.hytale.server.core.modules.entity.teleport.Teleport(
                            position,
                            com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f)  // No rotation change
                        )

                        // Add the Teleport component to trigger teleportation
                        store.addComponent(playerRef, com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), teleport)
                    } catch (e: Exception) {
                        TownyLog.error("Failed to teleport", e)
                    }
                }
            }
            close()
            return
        }

        if (data.setSpawn != null) {
            // Set current location as spawn (mayor only)
            if (!isMayor) {
                sendUpdate()
                return
            }

            val currentWorldLocal = world
            val playerRefLocal = playerEntityRef
            val pluginRef = plugin

            if (currentWorldLocal != null && playerRefLocal != null) {
                // Variable to store the captured location
                var capturedLocation: it.cava99.towny3d.model.BlockLocation? = null

                // First, get the player's position on the world thread (synchronous)
                currentWorldLocal.execute {
                    try {
                        val transformComponent = store.getComponent(
                            playerRefLocal,
                            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType()
                        ) as? com.hypixel.hytale.server.core.modules.entity.component.TransformComponent

                        if (transformComponent != null) {
                            val pos = transformComponent.position
                            capturedLocation = it.cava99.towny3d.model.BlockLocation(
                                currentWorldLocal.name ?: "world",
                                pos.x.toInt(),
                                pos.y.toInt(),
                                pos.z.toInt()
                            )
                        }
                    } catch (e: Exception) {
                        TownyLog.error("Failed to get player position", e)
                    }
                }

                // Now launch a coroutine to update the database
                if (capturedLocation != null) {
                    pluginRef.pluginScope.launch {
                        try {
                            pluginRef.townService.setHomeLocation(town.id, capturedLocation)
                        } catch (e: Exception) {
                            TownyLog.error("Failed to save spawn", e)
                        }
                    }
                }
            }
            close()
            return
        }

        if (data.openPlots != null) {
            // Open region map via PageManager (SimpleClaims pattern)
            val residentId = viewerResidentId
            if (residentId != null) {
                try {
                    val player = store.getComponent(ref, Player.getComponentType())
                    if (player != null) {
                        val world = store.getExternalData().world

                        // Use CompletableFuture.runAsync with world as executor (SimpleClaims pattern)
                        // This ensures we're on the world thread when getting position
                        java.util.concurrent.CompletableFuture.runAsync({
                            try {
                                val transformComponent = store.getComponent(
                                    ref,
                                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType()
                                ) as? com.hypixel.hytale.server.core.modules.entity.component.TransformComponent

                                if (transformComponent != null) {
                                    val pos = transformComponent.position
                                    val chunkX = pos.x.toInt() shr 5  // Divide by 32
                                    val chunkZ = pos.z.toInt() shr 5

                                    // Open directly - don't call close() before openCustomPage()
                                    // as it breaks event handling on the new page
                                    player.getPageManager().openCustomPage(
                                        ref, store,
                                        RegionMapGui(myPlayerRef, town, residentId, chunkX, chunkZ)
                                    )
                                } else {
                                    TownyLog.warn("Could not get transform component for region map")
                                }
                            } catch (e: Exception) {
                                TownyLog.error("Failed to get player position for region map", e)
                            }
                        }, world)
                        return
                    }
                } catch (e: Exception) {
                    TownyLog.warn("Could not open region map: ${e.message}")
                }
            }
            close()
            return
        }
    }

    /**
     * Handles toggling the isOpen setting.
     */
    private fun handleToggleOpen(newValue: Boolean) {
        if (!isMayor && !isAssistant) return

        // Update local town object immediately for UI consistency
        town = town.withModification(isOpen = newValue)

        plugin.pluginScope.launch {
            try {
                plugin.townService.setOpen(town.id, newValue)
                TownyLog.info("${town.name} set isOpen=$newValue by $viewerResidentId")
            } catch (e: Exception) {
                TownyLog.error("Failed to update isOpen setting", e)
            }
        }
    }

    /**
     * Handles toggling the isPublic setting.
     */
    private fun handleTogglePublic(newValue: Boolean) {
        if (!isMayor && !isAssistant) return

        // Update local town object immediately for UI consistency
        town = town.withModification(isPublic = newValue)

        plugin.pluginScope.launch {
            try {
                plugin.townService.setPublic(town.id, newValue)
                TownyLog.info("${town.name} set isPublic=$newValue by $viewerResidentId")
            } catch (e: Exception) {
                TownyLog.error("Failed to update isPublic setting", e)
            }
        }
    }

    /**
     * Handles saving all settings from the Settings tab.
     */
    private fun handleSaveSettings() {
        if (!isMayor && !isAssistant) return

        // TODO(2026-02-22): Read values from fields and save
        // For now, toggles are saved immediately, so just switch back to info tab
        currentTab = "info"
        val ui = UICommandBuilder()
        updateTabVisibility(ui)
        sendUpdate(ui, UIEventBuilder(), false)
    }

    /**
     * Handles board text change.
     */
    private fun handleBoardChange(newBoard: String?) {
        if (!isMayor && !isAssistant) return
        if (newBoard == null) return

        // Update local town object immediately for UI consistency
        town = town.withModification(board = newBoard)

        plugin.pluginScope.launch {
            try {
                plugin.townService.setBoard(town.id, newBoard)
                TownyLog.info("${town.name} updated board to '$newBoard' by $viewerResidentId")
            } catch (e: Exception) {
                TownyLog.error("Failed to update board", e)
            }
        }
    }

    /**
     * Handles tax amount change.
     */
    private fun handleTaxChange(newTax: String?) {
        if (!isMayor) return
        if (newTax == null) return

        val amount = newTax.toDoubleOrNull() ?: return
        if (amount < 0) return

        // Update local town object immediately for UI consistency
        val currentConfig = town.taxToResidents
        val newConfig = currentConfig.copy(amount = amount)
        town = town.withModification(taxToResidents = newConfig)

        plugin.pluginScope.launch {
            try {
                plugin.townService.setTaxToResidents(town.id, newConfig)
                TownyLog.info("${town.name} updated tax to $amount by $viewerResidentId")
            } catch (e: Exception) {
                TownyLog.error("Failed to update tax", e)
            }
        }
    }

    /**
     * Handles tax type change.
     */
    private fun handleTaxTypeChange(newType: String?) {
        if (!isMayor) return
        if (newType == null) return

        val taxType = when (newType) {
            "FLAT" -> it.cava99.towny3d.model.TaxType.FLAT
            "PERCENTAGE" -> it.cava99.towny3d.model.TaxType.PERCENTAGE
            else -> return
        }

        // Update local town object immediately for UI consistency
        val currentConfig = town.taxToResidents
        val newConfig = currentConfig.copy(type = taxType)
        town = town.withModification(taxToResidents = newConfig)

        plugin.pluginScope.launch {
            try {
                plugin.townService.setTaxToResidents(town.id, newConfig)
                TownyLog.info("${town.name} updated tax type to $taxType by $viewerResidentId")
            } catch (e: Exception) {
                TownyLog.error("Failed to update tax type", e)
            }
        }
    }

    /**
     * Handles promoting a resident to assistant.
     */
    private fun handlePromoteResident() {
        val residentId = selectedResidentId ?: return
        if (!isMayor) return

        plugin.pluginScope.launch {
            try {
                plugin.townService.addAssistant(town.id, residentId)
                TownyLog.info("$viewerResidentId promoted $residentId in ${town.name}")
                selectedResidentId = null
                val ui = UICommandBuilder()
                buildResidentsList(ui)
                sendUpdate(ui, UIEventBuilder(), false)
            } catch (e: Exception) {
                TownyLog.error("Failed to promote resident", e)
            }
        }
    }

    /**
     * Handles demoting an assistant to resident.
     */
    private fun handleDemoteResident() {
        val residentId = selectedResidentId ?: return
        if (!isMayor) return

        plugin.pluginScope.launch {
            try {
                plugin.townService.removeAssistant(town.id, residentId)
                TownyLog.info("$viewerResidentId demoted $residentId in ${town.name}")
                selectedResidentId = null
                val ui = UICommandBuilder()
                buildResidentsList(ui)
                sendUpdate(ui, UIEventBuilder(), false)
            } catch (e: Exception) {
                TownyLog.error("Failed to demote resident", e)
            }
        }
    }

    /**
     * Handles kicking a resident from the town.
     */
    private fun handleKickResident() {
        val residentId = selectedResidentId ?: return
        if (!isMayor && !isAssistant) return
        if (residentId == town.mayorId) return  // Can't kick mayor

        plugin.pluginScope.launch {
            try {
                plugin.townService.removeResident(town.id, residentId)
                TownyLog.info("$viewerResidentId kicked $residentId from ${town.name}")
                selectedResidentId = null
                val ui = UICommandBuilder()
                buildResidentsList(ui)
                sendUpdate(ui, UIEventBuilder(), false)
            } catch (e: Exception) {
                TownyLog.error("Failed to kick resident", e)
            }
        }
    }

    /**
     * Handles transferring mayor to another resident.
     */
    private fun handleTransferMayor() {
        val residentId = selectedResidentId ?: return
        if (!isMayor) return

        plugin.pluginScope.launch {
            try {
                plugin.townService.setMayor(town.id, residentId)
                TownyLog.info("$viewerResidentId transferred mayor to $residentId in ${town.name}")
                selectedResidentId = null
                close()
            } catch (e: Exception) {
                TownyLog.error("Failed to transfer mayor", e)
            }
        }
    }

    /**
     * Data class for UI events.
     * Uses codec to serialize/deserialize UI interactions.
     */
    class TownInfoData {
        companion object {
            val CODEC: BuilderCodec<TownInfoData> = BuilderCodec.builder(TownInfoData::class.java, ::TownInfoData)
                .stringField("SwitchTab", { o -> o.switchTab }, { o, v -> o.switchTab = v })
                .stringField("ToggleOpen", { o -> o.toggleOpen }, { o, v -> o.toggleOpen = v })
                .stringField("TogglePublic", { o -> o.togglePublic }, { o, v -> o.togglePublic = v })
                .stringField("SaveSettings", { o -> o.saveSettings }, { o, v -> o.saveSettings = v })
                .stringField("CancelSettings", { o -> o.cancelSettings }, { o, v -> o.cancelSettings = v })
                .stringField("@BoardChange", { o -> o.boardChange }, { o, v -> o.boardChange = v })
                .stringField("@TaxChange", { o -> o.taxChange }, { o, v -> o.taxChange = v })
                .stringField("@TaxTypeChange", { o -> o.taxTypeChange }, { o, v -> o.taxTypeChange = v })
                .stringField("Deposit", { o -> o.deposit }, { o, v -> o.deposit = v })
                .stringField("ExpandLayers", { o -> o.expandLayers }, { o, v -> o.expandLayers = v })
                .stringField("Spawn", { o -> o.spawn }, { o, v -> o.spawn = v })
                .stringField("SetSpawn", { o -> o.setSpawn }, { o, v -> o.setSpawn = v })
                .stringField("OpenPlots", { o -> o.openPlots }, { o, v -> o.openPlots = v })
                .stringField("PrevPage", { o -> o.prevPage }, { o, v -> o.prevPage = v })
                .stringField("NextPage", { o -> o.nextPage }, { o, v -> o.nextPage = v })
                .stringField("PromoteResident", { o -> o.promoteResident }, { o, v -> o.promoteResident = v })
                .stringField("DemoteResident", { o -> o.demoteResident }, { o, v -> o.demoteResident = v })
                .stringField("KickResident", { o -> o.kickResident }, { o, v -> o.kickResident = v })
                .stringField("TransferMayor", { o -> o.transferMayor }, { o, v -> o.transferMayor = v })
                .stringField("SelectResident", { o -> o.selectResident }, { o, v -> o.selectResident = v })
                .build()
        }

        @JvmField var switchTab: String? = null
        @JvmField var toggleOpen: String? = null
        @JvmField var togglePublic: String? = null
        @JvmField var saveSettings: String? = null
        @JvmField var cancelSettings: String? = null
        @JvmField var boardChange: String? = null
        @JvmField var taxChange: String? = null
        @JvmField var taxTypeChange: String? = null
        @JvmField var deposit: String? = null
        @JvmField var expandLayers: String? = null
        @JvmField var spawn: String? = null
        @JvmField var setSpawn: String? = null
        @JvmField var openPlots: String? = null
        @JvmField var prevPage: String? = null
        @JvmField var nextPage: String? = null
        @JvmField var promoteResident: String? = null
        @JvmField var demoteResident: String? = null
        @JvmField var kickResident: String? = null
        @JvmField var transferMayor: String? = null
        @JvmField var selectResident: String? = null
    }
}
