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
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.Town
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch

/**
 * Dialog for purchasing additional town layers.
 * Shows current layers, pricing, and allows quick-add buttons.
 */
class LayerPurchaseDialog(
    playerRef: PlayerRef,
    private val town: Town,
    private val viewerResidentId: ResidentId
) : InteractiveCustomUIPage<LayerPurchaseDialog.LayerPurchaseData>(
    playerRef,
    CustomPageLifetime.CanDismiss,
    LayerPurchaseData.CODEC
) {

    private val plugin = Towny3D.instance
    private var layersToAdd: Int = 0
    private var cachedPlayerBalance: Double = 0.0
    private val layerPrice: Double get() = plugin.getConfig().layerCost

    override fun build(
        ref: Ref<EntityStore>,
        ui: UICommandBuilder,
        ev: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        ui.append("Pages/HytaleTowny_LayerPurchase.ui")

        // Show current layers
        ui.set("#CurrentLayers.Text", "${town.maxLayers}")

        // Show layer price
        ui.set("#LayerPrice.Text", GuiUtils.formatMoney(layerPrice))

        // Fetch player balance asynchronously and update
        plugin.pluginScope.launch {
            try {
                cachedPlayerBalance = plugin.economyService.getBalance(viewerResidentId)
                val updateUi = UICommandBuilder()
                updateUi.set("#PlayerBalance.Text", GuiUtils.formatMoney(cachedPlayerBalance))
                sendUpdate(updateUi, UIEventBuilder(), false)
            } catch (e: Exception) {
                TownyLog.error("Failed to get player balance for layer purchase", e)
            }
        }

        // Set current layers value in field
        if (layersToAdd > 0) {
            ui.set("#LayersField.Value", layersToAdd.toString())
        }

        // Update total cost display
        updateTotalCost(ui)

        // Quick add buttons
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#Quick1",
            EventData.of("QuickAdd", "1"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#Quick5",
            EventData.of("QuickAdd", "5"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#Quick10",
            EventData.of("QuickAdd", "10"),
            false
        )

        // Layers field value change
        ev.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#LayersField",
            EventData.of("@Layers", "#LayersField.Value"),
            false
        )

        // Action buttons
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PurchaseButton",
            EventData.of("Purchase", "true"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelButton",
            EventData.of("Cancel", "true"),
            false
        )
    }

    override fun handleDataEvent(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        data: LayerPurchaseData
    ) {
        super.handleDataEvent(ref, store, data)

        // Handle quick add buttons
        if (data.quickAdd != null) {
            val addAmount = data.quickAdd!!.toIntOrNull() ?: 0
            layersToAdd += addAmount
            if (layersToAdd < 0) layersToAdd = 0
            sendFullUpdate()
            return
        }

        // Handle layers field changes
        if (data.layers != null) {
            layersToAdd = data.layers!!.toIntOrNull() ?: 0
            if (layersToAdd < 0) layersToAdd = 0
            sendFullUpdate()
            return
        }

        // Handle purchase
        if (data.purchase != null) {
            if (layersToAdd <= 0) {
                showError("Please enter a valid number of layers")
                return
            }

            val totalCost = layersToAdd * layerPrice

            if (totalCost > cachedPlayerBalance) {
                showError("Insufficient funds. You have ${GuiUtils.formatMoney(cachedPlayerBalance)}")
                return
            }

            // Perform purchase
            plugin.pluginScope.launch {
                try {
                    // Withdraw from player
                    val withdrawSuccess = plugin.economyService.withdraw(viewerResidentId, totalCost)
                    if (!withdrawSuccess) {
                        TownyLog.warn("Failed to withdraw $$totalCost from $viewerResidentId for layer purchase")
                        showError("Withdrawal failed. Please try again.")
                        return@launch
                    }

                    // Update town maxLayers via service
                    val newMaxLayers = town.maxLayers + layersToAdd
                    plugin.townService.setMaxLayers(town.id, newMaxLayers)

                    TownyLog.info("Layer purchase: $viewerResidentId purchased $layersToAdd layers for ${town.name} (new max: $newMaxLayers)")

                    // Close on success
                    close()
                } catch (e: Exception) {
                    TownyLog.error("Layer purchase failed", e)
                    showError("Purchase failed. Please try again.")
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

    private fun updateTotalCost(ui: UICommandBuilder) {
        val totalCost = layersToAdd * layerPrice
        ui.set("#TotalCost.Text", GuiUtils.formatMoney(totalCost))
    }

    private fun sendFullUpdate() {
        val ui = UICommandBuilder()
        val ev = UIEventBuilder()

        ui.set("#LayersField.Value", layersToAdd.toString())
        updateTotalCost(ui)
        ui.set("#ErrorMessage.Visible", false)

        sendUpdate(ui, ev, false)
    }

    private fun showError(message: String) {
        val ui = UICommandBuilder()
        val ev = UIEventBuilder()

        ui.set("#ErrorMessage.Text", message)
        ui.set("#ErrorMessage.Visible", true)

        sendUpdate(ui, ev, false)
    }

    /**
     * Data class for UI events.
     */
    class LayerPurchaseData {
        companion object {
            val CODEC: BuilderCodec<LayerPurchaseData> = BuilderCodec.builder(LayerPurchaseData::class.java, ::LayerPurchaseData)
                .stringField("QuickAdd", { o -> o.quickAdd }, { o, v -> o.quickAdd = v })
                .stringField("@Layers", { o -> o.layers }, { o, v -> o.layers = v })
                .stringField("Purchase", { o -> o.purchase }, { o, v -> o.purchase = v })
                .stringField("Cancel", { o -> o.cancel }, { o, v -> o.cancel = v })
                .build()
        }

        @JvmField var quickAdd: String? = null
        @JvmField var layers: String? = null
        @JvmField var purchase: String? = null
        @JvmField var cancel: String? = null
    }
}
