package it.cava99.towny3d.economy

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import it.cava99.towny3d.storage.StorageBackend
import it.cava99.towny3d.util.TownyLog

/**
 * Factory object that detects and creates the appropriate EconomyService implementation.
 *
 * Detection priority order:
 * 1. Cassaforte economy (if available and enabled)
 * 2. VaultUnlocked economy (if available and enabled)
 * 3. InternalEconomyService (fallback)
 *
 * This class uses reflection-safe detection to avoid ClassNotFoundException
 * when economy plugins are not installed.
 */
object EconomyServiceProvider {

    private const val PLUGIN_NAME = "Towny3D"

    /**
     * Detects the available economy service and returns an appropriate implementation.
     *
     * @param storage The storage backend (used for InternalEconomyService fallback)
     * @param plugin The plugin instance (for logging)
     * @return An EconomyService implementation
     */
    fun detectEconomy(storage: StorageBackend, plugin: JavaPlugin): EconomyService {
        // Try Cassaforte first (priority 1)
        if (tryCassaforte()) {
            TownyLog.info("Using Cassaforte economy")
            return CassaforteEconomyAdapter()
        }

        // Try VaultUnlocked second (priority 2)
        if (tryVaultUnlocked()) {
            TownyLog.info("Using VaultUnlocked economy")
            return VaultUnlockedEconomyAdapter()
        }

        // Fallback to internal economy
        TownyLog.info("Using internal economy (no external economy plugin found)")
        return InternalEconomyService.create(storage)
    }

    /**
     * Attempts to detect and use Cassaforte economy.
     *
     * @return true if Cassaforte economy is available and enabled
     */
    private fun tryCassaforte(): Boolean {
        return try {
            // Check if Cassaforte class is available
            val cassaforteClass = Class.forName("it.cassaforte.api.Cassaforte")

            // Get the economy instance
            val getEconomyMethod = cassaforteClass.getMethod("getEconomy")
            val economy = getEconomyMethod.invoke(null)

            // Check if economy is enabled
            if (economy != null) {
                val isEnabledMethod = economy.javaClass.getMethod("isEnabled")
                val isEnabled = isEnabledMethod.invoke(economy) as? Boolean ?: false

                if (isEnabled) {
                    TownyLog.debug("Cassaforte economy detected and enabled")
                    return true
                } else {
                    TownyLog.debug("Cassaforte economy available but not enabled")
                }
            } else {
                TownyLog.debug("Cassaforte available but no economy registered")
            }

            false
        } catch (e: ClassNotFoundException) {
            TownyLog.debug("Cassaforte not installed")
            false
        } catch (e: NoSuchMethodException) {
            TownyLog.debug("Cassaforte API mismatch")
            false
        } catch (e: Exception) {
            TownyLog.debug("Cassaforte detection failed: ${e.message}")
            false
        }
    }

    /**
     * Attempts to detect and use VaultUnlocked economy.
     *
     * @return true if VaultUnlocked economy is available and enabled
     */
    private fun tryVaultUnlocked(): Boolean {
        return try {
            // Check if VaultUnlocked class is available
            val servicesManagerClass = Class.forName("net.cfh.vault.VaultUnlockedServicesManager")

            // Get the singleton instance
            val getInstanceMethod = servicesManagerClass.getMethod("get")
            val servicesManager = getInstanceMethod.invoke(null)

            // Get the economy object
            val getEconomyMethod = servicesManager.javaClass.getMethod("economyObj")
            val economy = getEconomyMethod.invoke(servicesManager)

            // Check if economy is enabled
            if (economy != null) {
                val isEnabledMethod = economy.javaClass.getMethod("isEnabled")
                val isEnabled = isEnabledMethod.invoke(economy) as? Boolean ?: false

                if (isEnabled) {
                    TownyLog.debug("VaultUnlocked economy detected and enabled")
                    return true
                } else {
                    TownyLog.debug("VaultUnlocked economy available but not enabled")
                }
            } else {
                TownyLog.debug("VaultUnlocked available but no economy registered")
            }

            false
        } catch (e: ClassNotFoundException) {
            TownyLog.debug("VaultUnlocked not installed")
            false
        } catch (e: NoSuchMethodException) {
            TownyLog.debug("VaultUnlocked API mismatch")
            false
        } catch (e: Exception) {
            TownyLog.debug("VaultUnlocked detection failed: ${e.message}")
            false
        }
    }

    // ========== Economy Type Detection ==========

    /**
     * Represents the type of economy service provider.
     */
    enum class EconomyType {
        CASSAFORTE,
        VAULT_UNLOCKED,
        INTERNAL
    }

    /**
     * Detects the type of economy service.
     * @param economy The economy service to identify
     * @return The economy type
     */
    fun detectEconomyType(economy: EconomyService): EconomyType {
        return when (economy) {
            is CassaforteEconomyAdapter -> EconomyType.CASSAFORTE
            is VaultUnlockedEconomyAdapter -> EconomyType.VAULT_UNLOCKED
            is InternalEconomyService -> EconomyType.INTERNAL
            else -> EconomyType.INTERNAL
        }
    }

    /**
     * Gets the priority score of an economy type (higher = better).
     * @param type The economy type
     * @return Priority score (3 = Cassaforte, 2 = VaultUnlocked, 1 = Internal)
     */
    fun getPriorityScore(type: EconomyType): Int {
        return when (type) {
            EconomyType.CASSAFORTE -> 3
            EconomyType.VAULT_UNLOCKED -> 2
            EconomyType.INTERNAL -> 1
        }
    }

    // ========== Balance Migration ==========

    /**
     * Result of a balance migration operation.
     */
    data class MigrationResult(
        val success: Boolean,
        val playersMigrated: Int,
        val townsMigrated: Int,
        val totalAmount: Double,
        val errors: List<String> = emptyList()
    )

    /**
     * Handles migration of balances from internal economy to external economy.
     */
    object EconomyMigrator {

        /**
         * Migrates all balances from internal economy to external economy.
         *
         * @param internalEconomy The internal economy service (source)
         * @param externalEconomy The external economy service (destination)
         * @param storage Storage backend to load all residents and towns
         * @return MigrationResult with statistics and any errors
         */
        suspend fun migrateBalances(
            internalEconomy: InternalEconomyService,
            externalEconomy: EconomyService,
            storage: StorageBackend
        ): MigrationResult {
            TownyLog.info("Starting balance migration from internal to external economy...")

            val errors = mutableListOf<String>()
            var playersMigrated = 0
            var townsMigrated = 0
            var totalAmount = 0.0

            // Load all residents and towns
            val allResidents = storage.loadAllResidents()
            val allTowns = storage.loadAllTowns()

            TownyLog.info("Found ${allResidents.size} residents and ${allTowns.size} towns to migrate")

            // Migrate player balances
            for (resident in allResidents) {
                try {
                    val balance = internalEconomy.getBalance(resident.id)

                    if (balance > 0) {
                        val success = externalEconomy.deposit(resident.id, balance)

                        if (success) {
                            playersMigrated++
                            totalAmount += balance
                        } else {
                            errors.add("Failed to deposit ${"%.2f".format(balance)} to resident ${resident.name}")
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Error migrating resident ${resident.name}: ${e.message}")
                }
            }

            // Migrate town balances
            for (town in allTowns) {
                try {
                    val balance = internalEconomy.getTownBalance(town.id)

                    if (balance > 0) {
                        val success = externalEconomy.depositToTown(town.id, balance)

                        if (success) {
                            townsMigrated++
                            totalAmount += balance
                        } else {
                            errors.add("Failed to deposit ${"%.2f".format(balance)} to town ${town.name}")
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Error migrating town ${town.name}: ${e.message}")
                }
            }

            val success = errors.isEmpty() || (playersMigrated > 0 || townsMigrated > 0)

            if (success) {
                TownyLog.info("Migration complete: $playersMigrated players, $townsMigrated towns, ${"%.2f".format(totalAmount)} total")
            } else {
                TownyLog.error("Migration failed with ${errors.size} errors", null)
            }

            return MigrationResult(
                success = success,
                playersMigrated = playersMigrated,
                townsMigrated = townsMigrated,
                totalAmount = totalAmount,
                errors = errors
            )
        }
    }
}
