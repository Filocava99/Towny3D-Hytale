package it.cava99.towny3d.exception

/**
 * Base exception for all Towny-related errors.
 */
sealed class TownyException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when a required region cannot be found.
 */
class RegionNotFoundException(regionId: String) : TownyException("Region '$regionId' not found")

/**
 * Exception thrown when a required town cannot be found.
 */
class TownNotFoundException(townId: String) : TownyException("Town '$townId' not found")

/**
 * Exception thrown when a required nation cannot be found.
 */
class NationNotFoundException(nationId: String) : TownyException("Nation '$nationId' not found")

/**
 * Exception thrown when a required resident cannot be found.
 */
class ResidentNotFoundException(residentId: String) : TownyException("Resident '$residentId' not found")

/**
 * Exception thrown when an operation cannot be completed due to insufficient funds.
 */
class InsufficientFundsException(
    required: Double,
    available: Double,
    currencyName: String = "coins"
) : TownyException("Insufficient funds. Required: $required $currencyName, Available: $available $currencyName")

/**
 * Exception thrown when a player lacks permission to perform an action.
 */
class PermissionDeniedException(
    action: String,
    reason: String = ""
) : TownyException("Permission denied: $action${if (reason.isNotEmpty()) " ($reason)" else ""}")

/**
 * Exception thrown when a claim selection is invalid.
 */
class InvalidSelectionException(reason: String) : TownyException("Invalid selection: $reason")

/**
 * Exception thrown when a name validation fails.
 */
class InvalidNameException(
    name: String,
    reason: String
) : TownyException("Invalid name '$name': $reason")

/**
 * Exception thrown when a town operation fails due to the town's state.
 */
class TownOperationException(
    townName: String,
    reason: String
) : TownyException("Town '$townName' operation failed: $reason")

/**
 * Exception thrown when a region operation fails due to overlap or conflict.
 */
class RegionConflictException(
    reason: String
) : TownyException("Region conflict: $reason")

/**
 * Exception thrown when a nation operation fails due to the nation's state.
 */
class NationOperationException(
    nationName: String,
    reason: String
) : TownyException("Nation '$nationName' operation failed: $reason")

/**
 * Exception thrown when an economy operation fails.
 */
class EconomyException(message: String, cause: Throwable? = null) : TownyException(message, cause)

/**
 * Exception thrown when a storage operation fails.
 */
class StorageException(message: String, cause: Throwable? = null) : TownyException("Storage error: $message", cause)

/**
 * Exception thrown when a configuration value is invalid.
 */
class ConfigurationException(
    key: String,
    reason: String
) : TownyException("Invalid configuration value for '$key': $reason")
