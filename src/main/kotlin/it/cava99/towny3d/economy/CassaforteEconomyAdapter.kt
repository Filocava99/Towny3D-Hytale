package it.cava99.towny3d.economy

import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cassaforte.api.Cassaforte
import it.cassaforte.api.economy.Economy

/**
 * Adapter that bridges Cassaforte's Economy interface to Towny's EconomyService interface.
 *
 * This adapter:
 * - Converts ResidentId ↔ UUID for player operations
 * - Converts TownId → bank name ("town-{townId}") for town operations
 * - Returns safe defaults when no economy is registered
 * - Checks isEnabled() and hasBankSupport() before operations
 *
 * All plugin code uses this adapter via `plugin.economyService`, which delegates
 * to whatever economy is registered with Cassaforte (external or internal).
 */
class CassaforteEconomyAdapter : EconomyService {

    /**
     * Gets the currently registered economy from Cassaforte.
     * Returns null if no economy is registered.
     */
    private val externalEconomy: Economy?
        get() = Cassaforte.getEconomy()

    /**
     * Checks if an economy is registered and enabled.
     */
    private val isEnabled: Boolean
        get() = externalEconomy?.isEnabled() == true

    /**
     * Checks if the registered economy has bank support (for town operations).
     */
    private val hasBankSupport: Boolean
        get() = externalEconomy?.hasBankSupport() == true

    // ========== Player Economy ==========

    override suspend fun getBalance(residentId: ResidentId): Double {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.getBalance(residentId.value)
        } else {
            0.0
        }
    }

    override suspend fun withdraw(residentId: ResidentId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.withdrawPlayer(residentId.value, amount)
        return response.transactionSuccess()
    }

    override suspend fun deposit(residentId: ResidentId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.depositPlayer(residentId.value, amount)
        return response.transactionSuccess()
    }

    override suspend fun transfer(fromId: ResidentId, toId: ResidentId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        // Withdraw from sender
        val withdrawResp = economy.withdrawPlayer(fromId.value, amount)
        if (!withdrawResp.transactionSuccess()) return false

        // Deposit to recipient
        val depositResp = economy.depositPlayer(toId.value, amount)
        return depositResp.transactionSuccess()
    }

    // ========== Town Economy (via Banks) ==========

    /**
     * Converts a TownId to a bank name format.
     */
    private fun TownId.toBankName(): String = "town-$value"

    override suspend fun getTownBalance(townId: TownId): Double {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true && hasBankSupport) {
            val response = economy.bankBalance(townId.toBankName())
            if (response.transactionSuccess()) response.balance else 0.0
        } else {
            0.0
        }
    }

    override suspend fun withdrawFromTown(townId: TownId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true || !hasBankSupport) return false

        val response = economy.bankWithdraw(townId.toBankName(), amount)
        return response.transactionSuccess()
    }

    override suspend fun depositToTown(townId: TownId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true || !hasBankSupport) return false

        val response = economy.bankDeposit(townId.toBankName(), amount)
        return response.transactionSuccess()
    }

    override suspend fun depositToTownFromPlayer(residentId: ResidentId, townId: TownId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true || !hasBankSupport) return false

        // Manual transfer: withdraw from player, deposit to bank
        // Note: There's no bankTransfer method in Cassaforte's Economy interface
        val withdrawResp = economy.withdrawPlayer(residentId.value, amount)
        if (!withdrawResp.transactionSuccess()) return false

        val depositResp = economy.bankDeposit(townId.toBankName(), amount)
        return depositResp.transactionSuccess()
    }

    // ========== Nation Economy (via Banks) ==========

    /**
     * Converts a NationId to a bank name format.
     */
    private fun NationId.toBankName(): String = "nation-$value"

    override suspend fun getNationBalance(nationId: NationId): Double {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true && hasBankSupport) {
            val response = economy.bankBalance(nationId.toBankName())
            if (response.transactionSuccess()) response.balance else 0.0
        } else {
            0.0
        }
    }

    override suspend fun withdrawFromNation(nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true || !hasBankSupport) return false

        val response = economy.bankWithdraw(nationId.toBankName(), amount)
        return response.transactionSuccess()
    }

    override suspend fun depositToNation(nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true || !hasBankSupport) return false

        val response = economy.bankDeposit(nationId.toBankName(), amount)
        return response.transactionSuccess()
    }

    override suspend fun depositToNationFromTown(townId: TownId, nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true || !hasBankSupport) return false

        // Manual transfer: withdraw from town bank, deposit to nation bank
        val withdrawResp = economy.bankWithdraw(townId.toBankName(), amount)
        if (!withdrawResp.transactionSuccess()) return false

        val depositResp = economy.bankDeposit(nationId.toBankName(), amount)
        return depositResp.transactionSuccess()
    }

    override suspend fun depositToNationFromPlayer(residentId: ResidentId, nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true || !hasBankSupport) return false

        // Manual transfer: withdraw from player, deposit to nation bank
        val withdrawResp = economy.withdrawPlayer(residentId.value, amount)
        if (!withdrawResp.transactionSuccess()) return false

        val depositResp = economy.bankDeposit(nationId.toBankName(), amount)
        return depositResp.transactionSuccess()
    }

    // ========== Formatting ==========

    override fun formatAmount(amount: Double): String {
        // Use simple formatting - external economy.format() is broken
        return "${"%.2f".format(amount)} coins"
    }

    override fun getCurrencyName(): String {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.currencyNameSingular()
        } else {
            "coins"
        }
    }

    override fun getCurrencyNamePlural(): String {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.currencyNamePlural()
        } else {
            "coins"
        }
    }
}
