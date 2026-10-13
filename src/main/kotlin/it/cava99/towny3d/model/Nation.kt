package it.cava99.towny3d.model

import java.time.Instant

data class Nation(
    val id: NationId,
    val name: String,
    val kingId: ResidentId,
    val capitalId: TownId,
    val townIds: Set<TownId> = emptySet(),
    val allyNationIds: Set<NationId> = emptySet(),
    val enemyNationIds: Set<NationId> = emptySet(),
    val board: String = "",
    val isPublic: Boolean = false,
    val mapColor: Int = generateRandomColor(),
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant = Instant.now(),
    // Economy and taxation
    val balance: Double = 0.0,
    val taxToTowns: TaxConfig = TaxConfig(),
    val lastTaxCollection: Instant = Instant.now(),
    // Ruined state
    val isRuined: Boolean = false,
    val ruinedAt: Instant? = null
) {
    init {
        require(name.isNotBlank()) { "Nation name cannot be blank" }
        require(name.length <= 32) { "Nation name must be 32 characters or less, got ${name.length}" }
        require(townIds.contains(capitalId)) { "Capital town must be in townIds" }
    }

    /**
     * Returns the number of towns in this nation.
     */
    val townCount: Int get() = townIds.size

    /**
     * Checks if a town is part of this nation.
     */
    fun hasTown(townId: TownId): Boolean = townId in townIds

    /**
     * Checks if another nation is allied with this nation.
     */
    fun isAlly(nationId: NationId): Boolean = nationId in allyNationIds

    /**
     * Checks if another nation is an enemy of this nation.
     */
    fun isEnemy(nationId: NationId): Boolean = nationId in enemyNationIds

    /**
     * Creates a modified copy of this nation with updated modification timestamp.
     */
    fun withModification(
        name: String = this.name,
        kingId: ResidentId = this.kingId,
        capitalId: TownId = this.capitalId,
        townIds: Set<TownId> = this.townIds,
        allyNationIds: Set<NationId> = this.allyNationIds,
        enemyNationIds: Set<NationId> = this.enemyNationIds,
        board: String = this.board,
        isPublic: Boolean = this.isPublic,
        balance: Double = this.balance,
        taxToTowns: TaxConfig = this.taxToTowns,
        lastTaxCollection: Instant = this.lastTaxCollection,
        isRuined: Boolean = this.isRuined,
        ruinedAt: Instant? = this.ruinedAt
    ): Nation = copy(
        name = name,
        kingId = kingId,
        capitalId = capitalId,
        townIds = townIds,
        allyNationIds = allyNationIds,
        enemyNationIds = enemyNationIds,
        board = board,
        isPublic = isPublic,
        balance = balance,
        taxToTowns = taxToTowns,
        lastTaxCollection = lastTaxCollection,
        isRuined = isRuined,
        ruinedAt = ruinedAt,
        modifiedAt = Instant.now()
    )

    companion object {
        /**
         * Generates a random RGB color for map rendering.
         * Returns packed RGB integer: 0xRRGGBB
         */
        private fun generateRandomColor(): Int {
            val r = (0..255).random()
            val g = (0..255).random()
            val b = (0..255).random()
            return (r shl 16) or (g shl 8) or b
        }

        /**
         * Creates a new nation.
         */
        fun create(
            name: String,
            kingId: ResidentId,
            capitalId: TownId
        ): Nation = Nation(
            id = NationId.random(),
            name = name,
            kingId = kingId,
            capitalId = capitalId,
            townIds = setOf(capitalId),
            mapColor = generateRandomColor()
        )
    }

    /**
     * Builder class for creating modified copies of a Nation.
     * Provides a fluent API for setting individual properties.
     */
    class NationBuilder(private val base: Nation) {
        private var name: String = base.name
        private var kingId: ResidentId = base.kingId
        private var capitalId: TownId = base.capitalId
        private var townIds: Set<TownId> = base.townIds
        private var allyNationIds: Set<NationId> = base.allyNationIds
        private var enemyNationIds: Set<NationId> = base.enemyNationIds
        private var board: String = base.board
        private var isPublic: Boolean = base.isPublic
        private var balance: Double = base.balance
        private var taxToTowns: TaxConfig = base.taxToTowns
        private var lastTaxCollection: Instant = base.lastTaxCollection
        private var isRuined: Boolean = base.isRuined
        private var ruinedAt: Instant? = base.ruinedAt

        fun name(name: String) = apply { this.name = name }
        fun kingId(kingId: ResidentId) = apply { this.kingId = kingId }
        fun capitalId(capitalId: TownId) = apply { this.capitalId = capitalId }
        fun townIds(townIds: Set<TownId>) = apply { this.townIds = townIds }
        fun allyNationIds(allyNationIds: Set<NationId>) = apply { this.allyNationIds = allyNationIds }
        fun enemyNationIds(enemyNationIds: Set<NationId>) = apply { this.enemyNationIds = enemyNationIds }
        fun board(board: String) = apply { this.board = board }
        fun isPublic(isPublic: Boolean) = apply { this.isPublic = isPublic }
        fun balance(balance: Double) = apply { this.balance = balance }
        fun taxToTowns(taxToTowns: TaxConfig) = apply { this.taxToTowns = taxToTowns }
        fun lastTaxCollection(lastTaxCollection: Instant) = apply { this.lastTaxCollection = lastTaxCollection }
        fun isRuined(isRuined: Boolean) = apply { this.isRuined = isRuined }
        fun ruinedAt(ruinedAt: Instant?) = apply { this.ruinedAt = ruinedAt }

        fun build(): Nation = base.copy(
            name = name,
            kingId = kingId,
            capitalId = capitalId,
            townIds = townIds,
            allyNationIds = allyNationIds,
            enemyNationIds = enemyNationIds,
            board = board,
            isPublic = isPublic,
            balance = balance,
            taxToTowns = taxToTowns,
            lastTaxCollection = lastTaxCollection,
            isRuined = isRuined,
            ruinedAt = ruinedAt,
            modifiedAt = Instant.now()
        )
    }

    /**
     * Creates a new builder for modifying this nation.
     */
    fun builder() = NationBuilder(this)
}
