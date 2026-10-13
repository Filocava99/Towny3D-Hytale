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
 * Dialog for depositing money into the town bank.
 * Shows current balances, allows custom amount or quick-deposit buttons.
 */
class DepositDialogGui(
    playerRef: PlayerRef,
    private val town: Town,
    private val viewerResidentId: ResidentId
) : InteractiveCustomUIPage<DepositDialogGui.DepositData>(
    playerRef,
    CustomPageLifetime.CanDismiss,
    DepositData.CODEC
) {

    private val plugin = Towny3D.instance
    private var currentAmount: String = ""
    private var cachedPlayerBalance: Double = 0.0

    override fun build(
        ref: Ref<EntityStore>,
        ui: UICommandBuilder,
        ev: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        ui.append("Pages/HytaleTowny_Deposit.ui")

        // Show balances - use cached value if available, otherwise show town balance synchronously
        ui.set("#TownBalance.Text", GuiUtils.formatMoney(town.balance))

        // Fetch player balance asynchronously and update
        plugin.pluginScope.launch {
            try {
                cachedPlayerBalance = plugin.economyService.getBalance(viewerResidentId)
                val updateUi = UICommandBuilder()
                updateUi.set("#PlayerBalance.Text", GuiUtils.formatMoney(cachedPlayerBalance))
                sendUpdate(updateUi, UIEventBuilder(), false)
            } catch (e: Exception) {
                TownyLog.error("Failed to get player balance", e)
            }
        }

        // Set current amount in field
        if (currentAmount.isNotEmpty()) {
            ui.set("#AmountField.Value", currentAmount)
        }

        // Quick deposit buttons
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#Quick100",
            EventData.of("QuickAmount", "100"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#Quick500",
            EventData.of("QuickAmount", "500"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#Quick1000",
            EventData.of("QuickAmount", "1000"),
            false
        )
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#QuickAll",
            EventData.of("QuickAmount", "all"),
            false
        )

        // Amount field value change
        ev.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#AmountField",
            EventData.of("@Amount", "#AmountField.Value"),
            false
        )

        // Action buttons
        ev.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#DepositButton",
            EventData.of("Deposit", "true"),
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
        data: DepositData
    ) {
        super.handleDataEvent(ref, store, data)

        // Handle quick amount buttons
        if (data.quickAmount != null) {
            when (data.quickAmount) {
                "all" -> {
                    currentAmount = "%.2f".format(cachedPlayerBalance)
                }
                else -> {
                    currentAmount = data.quickAmount!!
                }
            }
            val ui = UICommandBuilder()
            ui.set("#AmountField.Value", currentAmount)
            sendUpdate(ui, UIEventBuilder(), false)
            return
        }

        // Handle amount field changes
        if (data.amount != null) {
            currentAmount = data.amount!!
            return
        }

        // Handle deposit
        if (data.deposit != null) {
            val amount = GuiUtils.parseMoney(currentAmount)

            if (amount == null || amount <= 0) {
                showError("Please enter a valid amount")
                return
            }

            if (amount > cachedPlayerBalance) {
                showError("Insufficient funds. You have ${GuiUtils.formatMoney(cachedPlayerBalance)}")
                return
            }

            // Perform deposit
            plugin.pluginScope.launch {
                try {
                    // Use atomic transfer from player to town
                    val success = plugin.economyService.depositToTownFromPlayer(
                        viewerResidentId,
                        town.id,
                        amount
                    )

                    if (!success) {
                        TownyLog.warn("Failed to transfer $$amount from $viewerResidentId to town ${town.id}")
                        return@launch
                    }

                    TownyLog.info("Deposit: $viewerResidentId deposited $$amount to town ${town.name}")

                    // Close on success
                    close()
                } catch (e: Exception) {
                    TownyLog.error("Deposit failed", e)
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
        val ev = UIEventBuilder()

        ui.set("#ErrorMessage.Text", message)
        ui.set("#ErrorMessage.Visible", true)

        sendUpdate(ui, ev, false)
    }

    /**
     * Data class for UI events.
     */
    class DepositData {
        companion object {
            val CODEC: BuilderCodec<DepositData> = BuilderCodec.builder(DepositData::class.java, ::DepositData)
                .stringField("QuickAmount", { o -> o.quickAmount }, { o, v -> o.quickAmount = v })
                .stringField("@Amount", { o -> o.amount }, { o, v -> o.amount = v })
                .stringField("Deposit", { o -> o.deposit }, { o, v -> o.deposit = v })
                .stringField("Cancel", { o -> o.cancel }, { o, v -> o.cancel = v })
                .build()
        }

        @JvmField var quickAmount: String? = null
        @JvmField var amount: String? = null
        @JvmField var deposit: String? = null
        @JvmField var cancel: String? = null
    }
}
