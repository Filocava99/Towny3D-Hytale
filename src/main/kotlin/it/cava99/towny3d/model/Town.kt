package it.cava99.towny3d.model

import java.time.Instant

data class Town(
    val id: TownId,
    val name: String,
    val nationId: NationId? = null,
    val mayorId: ResidentId,
    val assistantIds: Set<ResidentId> = emptySet(),
    val residentIds: Set<ResidentId> = emptySet(),
    val balance: Double = 0.0,
    val layers: Int = 0,
    val maxLayers: Int = 256,
    val homeLocation: BlockLocation? = null,
    val board: String = "",
    val isPublic: Boolean = false,
    val isOpen: Boolean = false,
    val mapColor: Int = generateRandomColor(),
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant = Instant.now(),
    // Taxation
    val taxToResidents: TaxConfig = TaxConfig(),
    val nationDebt: Double = 0.0,
    val lastTaxCollection: Instant = Instant.now(),
    // Ruined state
    val isRuined: Boolean = false,
    val ruinedAt: Instant? = null
) {
    init {
        require(name.isNotBlank()) { "Town name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Town name must be $MAX_NAME_LENGTH characters or less, got ${name.length}" }
        require(layers >= 0) { "Layers cannot be negative, got $layers" }
        require(maxLayers > 0) { "Max layers must be positive, got $maxLayers" }
        require(balance >= 0) { "Balance cannot be negative, got $balance" }
    }

    /**
     * Returns the number of residents in this town.
     */
    val residentCount: Int get() = residentIds.size

    /**
     * Returns the number of assistants in this town.
     */
    val assistantCount: Int get() = assistantIds.size

    /**
     * Checks if this town is part of a nation.
     */
    val hasNation: Boolean get() = nationId != null

    /**
     * Checks if a resident is the mayor.
     */
    fun isMayor(residentId: ResidentId): Boolean = mayorId == residentId

    /**
     * Checks if a resident is an assistant.
     */
    fun isAssistant(residentId: ResidentId): Boolean = residentId in assistantIds

    /**
     * Checks if a resident has leadership (mayor or assistant).
     */
    fun hasLeadership(residentId: ResidentId): Boolean =
        isMayor(residentId) || isAssistant(residentId)

    /**
     * Checks if a resident is a member of this town.
     */
    fun isResident(residentId: ResidentId): Boolean = residentId in residentIds

    /**
     * Returns true if the town can claim more layers.
     */
    val canClaimMore: Boolean get() = layers < maxLayers

    /**
     * Returns the number of additional layers that can be claimed.
     */
    val remainingLayers: Int get() = maxLayers - layers

    /**
     * Creates a modified copy of this town with updated modification timestamp.
     */
    fun withModification(
        name: String = this.name,
        nationId: NationId? = this.nationId,
        mayorId: ResidentId = this.mayorId,
        assistantIds: Set<ResidentId> = this.assistantIds,
        residentIds: Set<ResidentId> = this.residentIds,
        balance: Double = this.balance,
        layers: Int = this.layers,
        maxLayers: Int = this.maxLayers,
        homeLocation: BlockLocation? = this.homeLocation,
        board: String = this.board,
        isPublic: Boolean = this.isPublic,
        isOpen: Boolean = this.isOpen,
        mapColor: Int = this.mapColor,
        taxToResidents: TaxConfig = this.taxToResidents,
        nationDebt: Double = this.nationDebt,
        lastTaxCollection: Instant = this.lastTaxCollection,
        isRuined: Boolean = this.isRuined,
        ruinedAt: Instant? = this.ruinedAt
    ): Town = copy(
        name = name,
        nationId = nationId,
        mayorId = mayorId,
        assistantIds = assistantIds,
        residentIds = residentIds,
        balance = balance,
        layers = layers,
        maxLayers = maxLayers,
        homeLocation = homeLocation,
        board = board,
        isPublic = isPublic,
        isOpen = isOpen,
        mapColor = mapColor,
        taxToResidents = taxToResidents,
        nationDebt = nationDebt,
        lastTaxCollection = lastTaxCollection,
        isRuined = isRuined,
        ruinedAt = ruinedAt,
        modifiedAt = Instant.now()
    )

    /**
     * Builder pattern for fluent town modifications.
     */
    fun builder() = TownBuilder(this)

    class TownBuilder(private val base: Town) {
        private var name: String = base.name
        private var nationId: NationId? = base.nationId
        private var mayorId: ResidentId = base.mayorId
        private var assistantIds: Set<ResidentId> = base.assistantIds
        private var residentIds: Set<ResidentId> = base.residentIds
        private var balance: Double = base.balance
        private var layers: Int = base.layers
        private var maxLayers: Int = base.maxLayers
        private var homeLocation: BlockLocation? = base.homeLocation
        private var board: String = base.board
        private var isPublic: Boolean = base.isPublic
        private var isOpen: Boolean = base.isOpen
        private var mapColor: Int = base.mapColor
        private var taxToResidents: TaxConfig = base.taxToResidents
        private var nationDebt: Double = base.nationDebt
        private var lastTaxCollection: Instant = base.lastTaxCollection
        private var isRuined: Boolean = base.isRuined
        private var ruinedAt: Instant? = base.ruinedAt

        fun name(name: String) = apply { this.name = name }
        fun nationId(nationId: NationId?) = apply { this.nationId = nationId }
        fun mayorId(mayorId: ResidentId) = apply { this.mayorId = mayorId }
        fun assistantIds(assistantIds: Set<ResidentId>) = apply { this.assistantIds = assistantIds }
        fun residentIds(residentIds: Set<ResidentId>) = apply { this.residentIds = residentIds }
        fun balance(balance: Double) = apply { this.balance = balance }
        fun layers(layers: Int) = apply { this.layers = layers }
        fun maxLayers(maxLayers: Int) = apply { this.maxLayers = maxLayers }
        fun homeLocation(homeLocation: BlockLocation?) = apply { this.homeLocation = homeLocation }
        fun board(board: String) = apply { this.board = board }
        fun isPublic(isPublic: Boolean) = apply { this.isPublic = isPublic }
        fun isOpen(isOpen: Boolean) = apply { this.isOpen = isOpen }
        fun mapColor(mapColor: Int) = apply { this.mapColor = mapColor }
        fun taxToResidents(taxToResidents: TaxConfig) = apply { this.taxToResidents = taxToResidents }
        fun nationDebt(nationDebt: Double) = apply { this.nationDebt = nationDebt }
        fun lastTaxCollection(lastTaxCollection: Instant) = apply { this.lastTaxCollection = lastTaxCollection }
        fun isRuined(isRuined: Boolean) = apply { this.isRuined = isRuined }
        fun ruinedAt(ruinedAt: Instant?) = apply { this.ruinedAt = ruinedAt }

        fun build(): Town = base.copy(
            name = name,
            nationId = nationId,
            mayorId = mayorId,
            assistantIds = assistantIds,
            residentIds = residentIds,
            balance = balance,
            layers = layers,
            maxLayers = maxLayers,
            homeLocation = homeLocation,
            board = board,
            isPublic = isPublic,
            isOpen = isOpen,
            mapColor = mapColor,
            taxToResidents = taxToResidents,
            nationDebt = nationDebt,
            lastTaxCollection = lastTaxCollection,
            isRuined = isRuined,
            ruinedAt = ruinedAt,
            modifiedAt = Instant.now()
        )
    }

    companion object {
        /** Default maximum layers for a town */
        const val DEFAULT_MAX_LAYERS = 256

        /** Layers granted per resident */
        const val LAYERS_PER_RESIDENT = 3840

        /** Initial layers for a new town */
        const val INITIAL_LAYERS = 384

        /** Maximum town name length */
        const val MAX_NAME_LENGTH = 32

        /**
         * Generates a random RGB color for map rendering.
         * Returns packed RGB integer: 0xRRGGBB
         */
        fun generateRandomColor(): Int {
            val r = (0..255).random()
            val g = (0..255).random()
            val b = (0..255).random()
            return (r shl 16) or (g shl 8) or b
        }

        /**
         * Creates a new town.
         */
        fun create(
            name: String,
            mayorId: ResidentId,
            homeLocation: BlockLocation,
            initialBalance: Double = 0.0,
            initialLayers: Int = INITIAL_LAYERS,
            maxLayers: Int = DEFAULT_MAX_LAYERS + LAYERS_PER_RESIDENT // base + 1 resident
        ): Town = Town(
            id = TownId.random(),
            name = name,
            mayorId = mayorId,
            residentIds = setOf(mayorId),
            balance = initialBalance,
            layers = initialLayers,
            maxLayers = maxLayers,
            homeLocation = homeLocation,
            mapColor = generateRandomColor()
        )
    }
}
