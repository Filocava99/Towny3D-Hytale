package it.cava99.towny3d.economy

import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId

/**
 * Interface for economy operations.
 * This abstraction allows integration with different economy implementations.
 *
 * The plugin ships with an internal implementation that stores balance
 * in the Resident model, but can be replaced with external economy plugins.
 */
interface EconomyService {

    // ========== Player Economy ==========

    /**
     * Gets the balance of a player.
     * @param residentId The player's resident ID
     * @return The current balance
     */
    suspend fun getBalance(residentId: ResidentId): Double

    /**
     * Checks if a player has at least a certain amount.
     * @param residentId The player's resident ID
     * @param amount The amount to check for
     * @return true if the player has sufficient funds
     */
    suspend fun hasBalance(residentId: ResidentId, amount: Double): Boolean {
        return getBalance(residentId) >= amount
    }

    /**
     * Withdraws money from a player.
     * @param residentId The player's resident ID
     * @param amount The amount to withdraw
     * @return true if successful, false if insufficient funds
     */
    suspend fun withdraw(residentId: ResidentId, amount: Double): Boolean

    /**
     * Deposits money to a player.
     * @param residentId The player's resident ID
     * @param amount The amount to deposit
     * @return true if successful
     */
    suspend fun deposit(residentId: ResidentId, amount: Double): Boolean

    /**
     * Transfers money from one player to another.
     * @param fromId The sender's resident ID
     * @param toId The recipient's resident ID
     * @param amount The amount to transfer
     * @return true if successful, false if sender has insufficient funds
     */
    suspend fun transfer(fromId: ResidentId, toId: ResidentId, amount: Double): Boolean {
        val success = withdraw(fromId, amount)
        if (success) {
            deposit(toId, amount)
        }
        return success
    }

    // ========== Town Economy ==========

    /**
     * Gets the balance of a town.
     * @param townId The town's ID
     * @return The current balance
     */
    suspend fun getTownBalance(townId: TownId): Double

    /**
     * Checks if a town has at least a certain amount.
     * @param townId The town's ID
     * @param amount The amount to check for
     * @return true if the town has sufficient funds
     */
    suspend fun hasTownBalance(townId: TownId, amount: Double): Boolean {
        return getTownBalance(townId) >= amount
    }

    /**
     * Withdraws money from a town.
     * @param townId The town's ID
     * @param amount The amount to withdraw
     * @return true if successful, false if insufficient funds
     */
    suspend fun withdrawFromTown(townId: TownId, amount: Double): Boolean

    /**
     * Deposits money to a town.
     * @param townId The town's ID
     * @param amount The amount to deposit
     * @return true if successful
     */
    suspend fun depositToTown(townId: TownId, amount: Double): Boolean

    /**
     * Transfers money from a player to a town.
     * @param residentId The player's resident ID
     * @param townId The town's ID
     * @param amount The amount to transfer
     * @return true if successful, false if player has insufficient funds
     */
    suspend fun depositToTownFromPlayer(residentId: ResidentId, townId: TownId, amount: Double): Boolean {
        val success = withdraw(residentId, amount)
        if (success) {
            depositToTown(townId, amount)
        }
        return success
    }

    // ========== Nation Economy ==========

    /**
     * Gets the balance of a nation.
     * @param nationId The nation's ID
     * @return The current balance
     */
    suspend fun getNationBalance(nationId: NationId): Double

    /**
     * Checks if a nation has at least a certain amount.
     * @param nationId The nation's ID
     * @param amount The amount to check for
     * @return true if the nation has sufficient funds
     */
    suspend fun hasNationBalance(nationId: NationId, amount: Double): Boolean {
        return getNationBalance(nationId) >= amount
    }

    /**
     * Withdraws money from a nation.
     * @param nationId The nation's ID
     * @param amount The amount to withdraw
     * @return true if successful, false if insufficient funds
     */
    suspend fun withdrawFromNation(nationId: NationId, amount: Double): Boolean

    /**
     * Deposits money to a nation.
     * @param nationId The nation's ID
     * @param amount The amount to deposit
     * @return true if successful
     */
    suspend fun depositToNation(nationId: NationId, amount: Double): Boolean

    /**
     * Transfers money from a town to its nation.
     * @param townId The town's ID
     * @param nationId The nation's ID
     * @param amount The amount to transfer
     * @return true if successful, false if town has insufficient funds
     */
    suspend fun depositToNationFromTown(townId: TownId, nationId: NationId, amount: Double): Boolean {
        val success = withdrawFromTown(townId, amount)
        if (success) {
            depositToNation(nationId, amount)
        }
        return success
    }

    /**
     * Transfers money from a player to a nation.
     * @param residentId The player's resident ID
     * @param nationId The nation's ID
     * @param amount The amount to transfer
     * @return true if successful, false if player has insufficient funds
     */
    suspend fun depositToNationFromPlayer(residentId: ResidentId, nationId: NationId, amount: Double): Boolean {
        val success = withdraw(residentId, amount)
        if (success) {
            depositToNation(nationId, amount)
        }
        return success
    }

    // ========== Formatting ==========

    /**
     * Formats an amount as a currency string.
     * @param amount The amount to format
     * @return The formatted string (e.g., "$100.00")
     */
    fun formatAmount(amount: Double): String

    /**
     * Gets the currency symbol/name.
     * @return The currency symbol or name
     */
    fun getCurrencyName(): String

    /**
     * Gets the plural form of the currency name.
     * @return The plural currency name
     */
    fun getCurrencyNamePlural(): String
}
