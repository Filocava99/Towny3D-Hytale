package it.cava99.towny3d.service

import it.cava99.towny3d.model.Nation
import it.cava99.towny3d.model.Town

/**
 * Exception thrown when an operation is attempted on a ruined town.
 */
class TownRuinedException(message: String) : IllegalStateException(message)

/**
 * Exception thrown when an operation is attempted on a ruined nation.
 */
class NationRuinedException(message: String) : IllegalStateException(message)

/**
 * Service for checking and managing ruined states of towns and nations.
 * Provides validation and messaging for ruined entity operations.
 */
object RuinRecoveryService {

    /**
     * Checks if a town is ruined and throws an exception if so.
     * @param town The town to check
     * @param operation The operation being attempted (for error message)
     * @throws TownRuinedException if the town is ruined
     */
    fun requireTownNotRuined(town: Town, operation: String = "perform this action") {
        if (town.isRuined) {
            throw TownRuinedException(
                "Town '${town.name}' is ruined and cannot $operation. " +
                "Use /town reclaim to restore the town."
            )
        }
    }

    /**
     * Checks if a nation is ruined and throws an exception if so.
     * @param nation The nation to check
     * @param operation The operation being attempted (for error message)
     * @throws NationRuinedException if the nation is ruined
     */
    fun requireNationNotRuined(nation: Nation, operation: String = "perform this action") {
        if (nation.isRuined) {
            throw NationRuinedException(
                "Nation '${nation.name}' is ruined and cannot $operation. " +
                "Use /nation reclaim to restore the nation."
            )
        }
    }

    /**
     * Checks if a town is ruined and returns a result indicating the check.
     * @param town The town to check
     * @return true if the town is NOT ruined (operation can proceed), false otherwise
     */
    fun isTownOperational(town: Town): Boolean = !town.isRuined

    /**
     * Checks if a nation is ruined and returns a result indicating the check.
     * @param nation The nation to check
     * @return true if the nation is NOT ruined (operation can proceed), false otherwise
     */
    fun isNationOperational(nation: Nation): Boolean = !nation.isRuined

    /**
     * Gets a user-friendly message explaining why a ruined town cannot perform an action.
     * @param townName The name of the ruined town
     * @param action The action that was attempted
     * @return A formatted error message
     */
    fun getTownRuinedMessage(townName: String, action: String): String {
        return "Town '$townName' is ruined and cannot $action. " +
               "Use /town reclaim to restore the town."
    }

    /**
     * Gets a user-friendly message explaining why a ruined nation cannot perform an action.
     * @param nationName The name of the ruined nation
     * @param action The action that was attempted
     * @return A formatted error message
     */
    fun getNationRuinedMessage(nationName: String, action: String): String {
        return "Nation '$nationName' is ruined and cannot $action. " +
               "Use /nation reclaim to restore the nation."
    }
}
