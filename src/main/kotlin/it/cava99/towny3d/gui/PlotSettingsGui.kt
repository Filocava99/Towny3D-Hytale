package it.cava99.towny3d.gui

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.gui.common.stringField
import it.cava99.towny3d.gui.common.GuiUtils
import it.cava99.towny3d.model.Region
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch

/**
 * GUI for viewing and editing plot (region) settings.
 * Shows plot location, Y range, owner, toggle flags, and sale settings.
 * Permission-based editing: only mayor and assistants can modify settings.
 */
class PlotSettingsGui(
    playerRef: PlayerRef,
    private val regionId: RegionId,
    private val viewerResidentId: ResidentId
) : InteractiveCustomUIPage<PlotSettingsGui.PlotSettingsData>(
    playerRef,
    CustomPageLifetime.CanDismiss,
    PlotSettingsData.CODEC
) {

    private val plugin = Towny3D.instance
    private var region: Region? = null
    private var canEdit: Boolean = false
    private var plotName: String = ""
    private var pvpEnabled: Boolean = true
    private var fireEnabled: Boolean = true
    private var explosionEnabled: Boolean = true
    private var hostileMobsEnabled: Boolean = true
    private var forSale: Boolean = false
    private var salePrice: String = "0"

    override fun build(
        ref: Ref<EntityStore>,
        ui: UICommandBuilder,
        ev: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        ui.append("Pages/HytaleTowny_PlotSettings.ui")

        // Load region data
        plugin.pluginScope.launch {
            try {
                region = plugin.regionCache.get(regionId)
                val regionData = region

                if (regionData == null) {
                    showError("Plot not found")
                    return@launch
                }

                // Check edit permissions (mayor or assistant of the owning town)
                val town = plugin.townCache.get(regionData.townId)
                if (town != null) {
                    canEdit = town.mayorId == viewerResidentId ||
                              town.assistantIds.contains(viewerResidentId)
                }

                // Initialize state from region data
                plotName = regionData.name ?: ""
                pvpEnabled = regionData.toggles.pvp
                fireEnabled = regionData.toggles.fire
                explosionEnabled = regionData.toggles.explosion
                hostileMobsEnabled = regionData.toggles.hostileMobsSpawn
                forSale = regionData.forSale
                salePrice = regionData.salePrice.toString()

                // Update UI with region data
                val updateUi = UICommandBuilder()
                val updateEv = UIEventBuilder()

                // Location info
                updateUi.set("#ChunkCoords.Text", "(${regionData.chunkPos.x}, ${regionData.chunkPos.z})")
                updateUi.set("#YRange.Text", "${regionData.y1} to ${regionData.y2}")

                // Plot name
                updateUi.set("#PlotNameField.Value", plotName)
                updateUi.set("#PlotNameField.Disabled", !canEdit)

                // Owner section
                val ownerName = if (regionData.ownerId != null) {
                    plugin.residentCache.getSync(regionData.ownerId)?.name ?: "Unknown"
                } else {
                    "Town Owned"
                }
                updateUi.set("#OwnerName.Text", ownerName)

                // Show evict button only if there's an owner and user can edit
                if (regionData.ownerId != null && canEdit) {
                    updateUi.set("#EvictButton.Visible", true)
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#EvictButton",
                        EventData.of("Evict", "true"),
                        false
                    )
                }

                // Toggle flags
                updateUi.set("#PvpToggle.Value", pvpEnabled)
                updateUi.set("#PvpToggle.Disabled", !canEdit)
                updateUi.set("#FireToggle.Value", fireEnabled)
                updateUi.set("#FireToggle.Disabled", !canEdit)
                updateUi.set("#ExplosionToggle.Value", explosionEnabled)
                updateUi.set("#ExplosionToggle.Disabled", !canEdit)
                updateUi.set("#MobsToggle.Value", hostileMobsEnabled)
                updateUi.set("#MobsToggle.Disabled", !canEdit)

                // For sale
                updateUi.set("#ForSaleToggle.Value", forSale)
                updateUi.set("#ForSaleToggle.Disabled", !canEdit)
                updateUi.set("#PriceField.Value", salePrice)
                updateUi.set("#PriceField.Disabled", !canEdit || !forSale)

                // Save button
                updateUi.set("#SaveButton.Disabled", !canEdit)

                // Add toggle event bindings if can edit
                if (canEdit) {
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#PvpToggle",
                        EventData.of("@Pvp", "#PvpToggle.Value"),
                        false
                    )
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#FireToggle",
                        EventData.of("@Fire", "#FireToggle.Value"),
                        false
                    )
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#ExplosionToggle",
                        EventData.of("@Explosion", "#ExplosionToggle.Value"),
                        false
                    )
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#MobsToggle",
                        EventData.of("@Mobs", "#MobsToggle.Value"),
                        false
                    )
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#ForSaleToggle",
                        EventData.of("@ForSale", "#ForSaleToggle.Value"),
                        false
                    )
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#PriceField",
                        EventData.of("@Price", "#PriceField.Value"),
                        false
                    )
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        "#PlotNameField",
                        EventData.of("@Name", "#PlotNameField.Value"),
                        false
                    )
                    updateEv.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#SaveButton",
                        EventData.of("Save", "true"),
                        false
                    )
                }

                updateEv.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#CancelButton",
                    EventData.of("Cancel", "true"),
                    false
                )

                sendUpdate(updateUi, updateEv, true)
            } catch (e: Exception) {
                TownyLog.error("Failed to load plot settings", e)
                showError("Failed to load plot data")
            }
        }
    }

    override fun handleDataEvent(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        data: PlotSettingsData
    ) {
        super.handleDataEvent(ref, store, data)

        // Handle toggle changes
        data.pvp?.let { pvpEnabled = it == "true" }
        data.fire?.let { fireEnabled = it == "true" }
        data.explosion?.let { explosionEnabled = it == "true" }
        data.mobs?.let { hostileMobsEnabled = it == "true" }
        data.forSale?.let {
            forSale = it == "true"
            // Update price field visibility
            val ui = UICommandBuilder()
            ui.set("#PriceField.Disabled", !canEdit || !forSale)
            sendUpdate(ui, UIEventBuilder(), true)
        }
        data.price?.let { salePrice = it }
        data.name?.let { plotName = it }

        // Handle evict
        if (data.evict != null && canEdit) {
            plugin.pluginScope.launch {
                try {
                    val currentRegion = region ?: return@launch
                    plugin.regionService.removeOwner(currentRegion.id)
                    region = currentRegion.withModification(ownerId = null)

                    val ui = UICommandBuilder()
                    ui.set("#OwnerName.Text", "Town Owned")
                    ui.set("#EvictButton.Visible", false)
                    sendUpdate(ui, UIEventBuilder(), true)

                    TownyLog.info("Evicted owner from plot ${currentRegion.id}")
                } catch (e: Exception) {
                    TownyLog.error("Failed to evict plot owner", e)
                    showError("Failed to evict owner")
                }
            }
            return
        }

        // Handle save
        if (data.save != null && canEdit) {
            val price = GuiUtils.parseMoney(salePrice)
            if (forSale && (price == null || price < 0)) {
                showError("Please enter a valid price")
                return
            }

            plugin.pluginScope.launch {
                try {
                    val currentRegion = region ?: return@launch

                    // Update toggle flags
                    val updatedRegion = currentRegion.withModification(
                        name = plotName.ifBlank { null },
                        toggles = currentRegion.toggles.copy(
                            pvp = pvpEnabled,
                            fire = fireEnabled,
                            explosion = explosionEnabled,
                            hostileMobsSpawn = hostileMobsEnabled
                        ),
                        forSale = forSale,
                        salePrice = price ?: 0.0
                    )

                    plugin.regionService.updateRegion(updatedRegion)
                    region = updatedRegion

                    TownyLog.info("Updated plot settings for ${updatedRegion.id}")
                    close()
                } catch (e: Exception) {
                    TownyLog.error("Failed to save plot settings", e)
                    showError("Failed to save changes")
                }
            }
            return
        }

        // Handle cancel
        if (data.cancel != null) {
            close()
            return
        }
    }

    private fun showError(message: String) {
        val ui = UICommandBuilder()
        ui.set("#ErrorMessage.Text", message)
        ui.set("#ErrorMessage.Visible", true)
        sendUpdate(ui, UIEventBuilder(), false)
    }

    /**
     * Data class for UI events.
     */
    class PlotSettingsData {
        companion object {
            val CODEC: BuilderCodec<PlotSettingsData> = BuilderCodec.builder(PlotSettingsData::class.java, ::PlotSettingsData)
                .stringField("@Pvp", { o -> o.pvp }, { o, v -> o.pvp = v })
                .stringField("@Fire", { o -> o.fire }, { o, v -> o.fire = v })
                .stringField("@Explosion", { o -> o.explosion }, { o, v -> o.explosion = v })
                .stringField("@Mobs", { o -> o.mobs }, { o, v -> o.mobs = v })
                .stringField("@ForSale", { o -> o.forSale }, { o, v -> o.forSale = v })
                .stringField("@Price", { o -> o.price }, { o, v -> o.price = v })
                .stringField("@Name", { o -> o.name }, { o, v -> o.name = v })
                .stringField("Evict", { o -> o.evict }, { o, v -> o.evict = v })
                .stringField("Save", { o -> o.save }, { o, v -> o.save = v })
                .stringField("Cancel", { o -> o.cancel }, { o, v -> o.cancel = v })
                .build()
        }

        @JvmField var pvp: String? = null
        @JvmField var fire: String? = null
        @JvmField var explosion: String? = null
        @JvmField var mobs: String? = null
        @JvmField var forSale: String? = null
        @JvmField var price: String? = null
        @JvmField var name: String? = null
        @JvmField var evict: String? = null
        @JvmField var save: String? = null
        @JvmField var cancel: String? = null
    }
}
