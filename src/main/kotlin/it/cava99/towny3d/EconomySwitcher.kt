package it.cava99.towny3d

import it.cava99.towny3d.storage.StorageBackend
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles lazy economy detection and migration after all worlds are loaded.
 * Supports switching from internal economy to external providers (Cassaforte, VaultUnlocked).
 */
class EconomySwitcher(
    private val services: ServiceLocator,
    private val storageBackend: StorageBackend,
    private val plugin: Towny3D,
    private val economyMutex: Mutex
) {
    /**
     * Checks for better economy providers and migrates balances if needed.
     */
    suspend fun handleLazyEconomyDetection(
        logInfo: (String) -> Unit,
        logWarning: (String) -> Unit,
        logError: (String, Throwable?) -> Unit
    ) {
        logInfo("Running lazy economy detection...")

        val currentType = it.cava99.towny3d.economy.EconomyServiceProvider.detectEconomyType(services.economyService)
        val newEconomy = it.cava99.towny3d.economy.EconomyServiceProvider.detectEconomy(storageBackend, plugin)
        val newType = it.cava99.towny3d.economy.EconomyServiceProvider.detectEconomyType(newEconomy)

        val currentPriority = it.cava99.towny3d.economy.EconomyServiceProvider.getPriorityScore(currentType)
        val newPriority = it.cava99.towny3d.economy.EconomyServiceProvider.getPriorityScore(newType)

        if (newPriority > currentPriority) {
            logInfo("Better economy provider detected: $newType (priority $newPriority) > $currentType (priority $currentPriority)")

            if (currentType == it.cava99.towny3d.economy.EconomyServiceProvider.EconomyType.INTERNAL) {
                val internalEconomy = services.economyService as? it.cava99.towny3d.economy.InternalEconomyService

                if (internalEconomy != null) {
                    logInfo("Migrating balances from internal economy to $newType...")

                    val result = it.cava99.towny3d.economy.EconomyServiceProvider.EconomyMigrator.migrateBalances(
                        internalEconomy = internalEconomy,
                        externalEconomy = newEconomy,
                        storage = storageBackend
                    )

                    if (result.success) {
                        logInfo("Balance migration successful: ${result.playersMigrated} players, ${result.townsMigrated} towns, ${"%.2f".format(result.totalAmount)} total")

                        economyMutex.withLock {
                            services.economyService = newEconomy
                        }

                        logInfo("Switched to $newType economy service")
                        internalEconomy.clearAllCaches()
                    } else {
                        logError("Balance migration failed: ${result.errors.joinToString(", ")}", null)
                        logWarning("Continuing with internal economy due to migration failure")
                    }
                } else {
                    logWarning("Current economy is not InternalEconomyService, skipping migration")
                    economyMutex.withLock {
                        services.economyService = newEconomy
                    }
                }
            } else {
                logInfo("Switching from $currentType to $newType (no migration needed)")
                economyMutex.withLock {
                    services.economyService = newEconomy
                }
            }
        } else {
            logInfo("No better economy provider found (current: $currentType at priority $currentPriority)")
        }
    }
}
