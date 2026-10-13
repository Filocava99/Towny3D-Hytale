package it.cava99.towny3d.economy

import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import net.cfh.vault.VaultUnlockedServicesManager
import net.milkbowl.vault2.economy.Economy
import net.milkbowl.vault2.economy.EconomyResponse
import java.math.BigDecimal
import java.util.UUID

/**
 * Adapter that bridges VaultUnlocked's Economy interface to Towny's EconomyService interface.
 *
 * This adapter:
 * - Converts ResidentId ↔ UUID for player operations
 * - Converts TownId → UUID for town operations (using name-based UUID)
 * - Converts BigDecimal ↔ Double for amounts
 * - Checks isEnabled() before operations
 * - Returns safe defaults when economy is not available
 */
class VaultUnlockedEconomyAdapter : EconomyService {

    companion object {
        /**
         * Plugin name used for VaultUnlocked API calls (for logging/diagnostics).
         */
        private const val PLUGIN_NAME = "Towny3D"
    }

    /**
     * Gets the currently registered economy from VaultUnlocked.
     * Returns null if no economy is registered.
     */
    private val externalEconomy: Economy?
        get() = try {
            VaultUnlockedServicesManager.get().economyObj()
        } catch (e: Exception) {
            null
        }

    /**
     * Checks if an economy is registered and enabled.
     */
    private val isEnabled: Boolean
        get() = externalEconomy?.isEnabled() == true

    // ========== Player Economy ==========

    override suspend fun getBalance(residentId: ResidentId): Double {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.balance(PLUGIN_NAME, residentId.value).toDouble()
        } else {
            0.0
        }
    }

    override suspend fun withdraw(residentId: ResidentId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.withdraw(PLUGIN_NAME, residentId.value, BigDecimal.valueOf(amount))
        return response.type == EconomyResponse.ResponseType.SUCCESS
    }

    override suspend fun deposit(residentId: ResidentId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.deposit(PLUGIN_NAME, residentId.value, BigDecimal.valueOf(amount))
        return response.type == EconomyResponse.ResponseType.SUCCESS
    }

    override suspend fun transfer(fromId: ResidentId, toId: ResidentId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        // Withdraw from sender
        val withdrawResponse = economy.withdraw(PLUGIN_NAME, fromId.value, BigDecimal.valueOf(amount))
        if (withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) return false

        // Deposit to recipient
        val depositResponse = economy.deposit(PLUGIN_NAME, toId.value, BigDecimal.valueOf(amount))
        return depositResponse.type == EconomyResponse.ResponseType.SUCCESS
    }

    // ========== Town Economy ==========

    /**
     * Converts a TownId to a UUID for town bank accounts.
     * Uses a name-based UUID generation to ensure consistency.
     */
    private fun TownId.toBankUuid(): UUID {
        // Use the TownId's string value to generate a consistent UUID
        // This ensures the same town always maps to the same "bank account"
        return UUID.nameUUIDFromBytes("town-$value".toByteArray())
    }

    override suspend fun getTownBalance(townId: TownId): Double {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.balance(PLUGIN_NAME, townId.toBankUuid()).toDouble()
        } else {
            0.0
        }
    }

    override suspend fun withdrawFromTown(townId: TownId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.withdraw(PLUGIN_NAME, townId.toBankUuid(), BigDecimal.valueOf(amount))
        return response.type == EconomyResponse.ResponseType.SUCCESS
    }

    override suspend fun depositToTown(townId: TownId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.deposit(PLUGIN_NAME, townId.toBankUuid(), BigDecimal.valueOf(amount))
        return response.type == EconomyResponse.ResponseType.SUCCESS
    }

    override suspend fun depositToTownFromPlayer(residentId: ResidentId, townId: TownId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        // Manual transfer: withdraw from player, deposit to town
        val withdrawResponse = economy.withdraw(PLUGIN_NAME, residentId.value, BigDecimal.valueOf(amount))
        if (withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) return false

        val depositResponse = economy.deposit(PLUGIN_NAME, townId.toBankUuid(), BigDecimal.valueOf(amount))
        return depositResponse.type == EconomyResponse.ResponseType.SUCCESS
    }

    // ========== Nation Economy ==========

    /**
     * Converts a NationId to a UUID for nation bank accounts.
     * Uses a name-based UUID generation to ensure consistency.
     */
    private fun NationId.toBankUuid(): UUID {
        // Use the NationId's string value to generate a consistent UUID
        // This ensures the same nation always maps to the same "bank account"
        return UUID.nameUUIDFromBytes("nation-$value".toByteArray())
    }

    override suspend fun getNationBalance(nationId: NationId): Double {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.balance(PLUGIN_NAME, nationId.toBankUuid()).toDouble()
        } else {
            0.0
        }
    }

    override suspend fun withdrawFromNation(nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.withdraw(PLUGIN_NAME, nationId.toBankUuid(), BigDecimal.valueOf(amount))
        return response.type == EconomyResponse.ResponseType.SUCCESS
    }

    override suspend fun depositToNation(nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        val response = economy.deposit(PLUGIN_NAME, nationId.toBankUuid(), BigDecimal.valueOf(amount))
        return response.type == EconomyResponse.ResponseType.SUCCESS
    }

    override suspend fun depositToNationFromTown(townId: TownId, nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        // Manual transfer: withdraw from town, deposit to nation
        val withdrawResponse = economy.withdraw(PLUGIN_NAME, townId.toBankUuid(), BigDecimal.valueOf(amount))
        if (withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) return false

        val depositResponse = economy.deposit(PLUGIN_NAME, nationId.toBankUuid(), BigDecimal.valueOf(amount))
        return depositResponse.type == EconomyResponse.ResponseType.SUCCESS
    }

    override suspend fun depositToNationFromPlayer(residentId: ResidentId, nationId: NationId, amount: Double): Boolean {
        val economy = externalEconomy
        if (economy?.isEnabled() != true) return false

        // Manual transfer: withdraw from player, deposit to nation
        val withdrawResponse = economy.withdraw(PLUGIN_NAME, residentId.value, BigDecimal.valueOf(amount))
        if (withdrawResponse.type != EconomyResponse.ResponseType.SUCCESS) return false

        val depositResponse = economy.deposit(PLUGIN_NAME, nationId.toBankUuid(), BigDecimal.valueOf(amount))
        return depositResponse.type == EconomyResponse.ResponseType.SUCCESS
    }

    // ========== Formatting ==========

    override fun formatAmount(amount: Double): String {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.format(PLUGIN_NAME, BigDecimal.valueOf(amount))
        } else {
            "${"%.2f".format(amount)} coins"
        }
    }

    override fun getCurrencyName(): String {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.defaultCurrencyNameSingular(PLUGIN_NAME)
        } else {
            "coins"
        }
    }

    override fun getCurrencyNamePlural(): String {
        val economy = externalEconomy
        return if (economy?.isEnabled() == true) {
            economy.defaultCurrencyNamePlural(PLUGIN_NAME)
        } else {
            "coins"
        }
    }
}
