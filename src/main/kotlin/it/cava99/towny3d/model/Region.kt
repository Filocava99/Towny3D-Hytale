package it.cava99.towny3d.model

import java.time.Instant

/**
 * Represents a 3D land claim region.
 * Regions are immutable for thread safety. Use copy() to create modified versions.
 *
 * @property id Unique identifier for this region
 * @property chunkPos The chunk position this region occupies (entire 32x32 chunk horizontally)
 * @property y1 The lower Y bound of the region (inclusive, range: -512 to 512)
 * @property y2 The upper Y bound of the region (inclusive, range: -512 to 512)
 * @property townId The ID of the town that owns this region
 * @property ownerId The ID of the resident who owns this specific plot (null if town-owned)
 * @property type The type of region (NORMAL, EMBASSY, HOME)
 * @property name Optional custom name for the region
 * @property permissions Permission set for different player groups
 * @property toggles Environment toggle flags
 * @property forSale Whether this region is for sale
 * @property salePrice The price if for sale
 * @property plotTax Tax configuration for this plot
 * @property createdAt When this region was claimed
 * @property modifiedAt When this region was last modified
 */
data class Region(
    val id: RegionId,
    val chunkPos: ChunkPos,
    val y1: Int,
    val y2: Int,
    val townId: TownId,
    val ownerId: ResidentId? = null,
    val type: RegionType = RegionType.NORMAL,
    val name: String? = null,
    val permissions: PermissionSet = PermissionSet.defaultRegion(),
    val toggles: ToggleFlags = ToggleFlags.default(),
    val forSale: Boolean = false,
    val salePrice: Double = 0.0,
    val plotTax: TaxConfig = TaxConfig(),
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant = Instant.now()
) {
    init {
        require(y1 >= -512 && y1 <= 512) { "y1 must be between -512 and 512, got $y1" }
        require(y2 >= -512 && y2 <= 512) { "y2 must be between -512 and 512, got $y2" }
        require(y1 <= y2) { "y1 must be <= y2, got y1=$y1, y2=$y2" }
    }

    /**
     * Gets the vertical height of this region in blocks.
     */
    val height: Int get() = y2 - y1 + 1

    /**
     * Checks if a specific Y coordinate is within this region.
     */
    fun containsY(y: Int): Boolean = y in y1..y2

    /**
     * Checks if a block location is within this region.
     * Assumes the block is in the same chunk (horizontal check not needed).
     */
    fun contains(location: BlockLocation): Boolean {
        return location.chunkPos == chunkPos && containsY(location.y)
    }

    /**
     * Returns true if this region is owned by a specific player (not town-owned).
     */
    val isOwned: Boolean get() = ownerId != null

    /**
     * Returns true if this region is town-owned (no specific owner).
     */
    val isTownOwned: Boolean get() = ownerId == null

    /**
     * Creates a modified copy of this region with updated modification timestamp.
     */
    fun withModification(
        id: RegionId = this.id,
        chunkPos: ChunkPos = this.chunkPos,
        y1: Int = this.y1,
        y2: Int = this.y2,
        townId: TownId = this.townId,
        ownerId: ResidentId? = this.ownerId,
        type: RegionType = this.type,
        name: String? = this.name,
        permissions: PermissionSet = this.permissions,
        toggles: ToggleFlags = this.toggles,
        pvp: Boolean? = null,
        fire: Boolean? = null,
        explosion: Boolean? = null,
        forSale: Boolean = this.forSale,
        salePrice: Double = this.salePrice,
        plotTax: TaxConfig = this.plotTax
    ): Region = copy(
        id = id,
        chunkPos = chunkPos,
        y1 = y1,
        y2 = y2,
        townId = townId,
        ownerId = ownerId,
        type = type,
        name = name,
        permissions = permissions,
        toggles = toggles.copy(
            pvp = pvp ?: toggles.pvp,
            fire = fire ?: toggles.fire,
            explosion = explosion ?: toggles.explosion
        ),
        forSale = forSale,
        salePrice = salePrice,
        plotTax = plotTax,
        modifiedAt = Instant.now()
    )

    companion object {
        /**
         * Creates a new home region for a town.
         */
        fun createHome(
            townId: TownId,
            chunkPos: ChunkPos,
            y1: Int = -64,
            y2: Int = 320
        ): Region = Region(
            id = RegionId.random(),
            chunkPos = chunkPos,
            y1 = y1,
            y2 = y2,
            townId = townId,
            type = RegionType.HOME,
            permissions = PermissionSet.defaultHome()
        )

        /**
         * Creates a new normal region for a town.
         */
        fun createRegion(
            townId: TownId,
            chunkPos: ChunkPos,
            y1: Int,
            y2: Int
        ): Region = Region(
            id = RegionId.random(),
            chunkPos = chunkPos,
            y1 = y1,
            y2 = y2,
            townId = townId,
            type = RegionType.NORMAL,
            permissions = PermissionSet.defaultRegion()
        )
    }

    /**
     * Builder class for creating modified copies of a Region.
     * Provides a fluent API for setting individual properties.
     */
    class RegionBuilder(private val base: Region) {
        private var chunkPos: ChunkPos = base.chunkPos
        private var y1: Int = base.y1
        private var y2: Int = base.y2
        private var townId: TownId = base.townId
        private var ownerId: ResidentId? = base.ownerId
        private var type: RegionType = base.type
        private var name: String? = base.name
        private var permissions: PermissionSet = base.permissions
        private var toggles: ToggleFlags = base.toggles
        private var pvp: Boolean? = null
        private var fire: Boolean? = null
        private var explosion: Boolean? = null
        private var forSale: Boolean = base.forSale
        private var salePrice: Double = base.salePrice
        private var plotTax: TaxConfig = base.plotTax

        fun chunkPos(chunkPos: ChunkPos) = apply { this.chunkPos = chunkPos }
        fun y1(y1: Int) = apply { this.y1 = y1 }
        fun y2(y2: Int) = apply { this.y2 = y2 }
        fun townId(townId: TownId) = apply { this.townId = townId }
        fun ownerId(ownerId: ResidentId?) = apply { this.ownerId = ownerId }
        fun type(type: RegionType) = apply { this.type = type }
        fun name(name: String?) = apply { this.name = name }
        fun permissions(permissions: PermissionSet) = apply { this.permissions = permissions }
        fun toggles(toggles: ToggleFlags) = apply { this.toggles = toggles }
        fun pvp(pvp: Boolean) = apply { this.pvp = pvp }
        fun fire(fire: Boolean) = apply { this.fire = fire }
        fun explosion(explosion: Boolean) = apply { this.explosion = explosion }
        fun forSale(forSale: Boolean) = apply { this.forSale = forSale }
        fun salePrice(salePrice: Double) = apply { this.salePrice = salePrice }
        fun plotTax(plotTax: TaxConfig) = apply { this.plotTax = plotTax }

        fun build(): Region = base.copy(
            chunkPos = chunkPos,
            y1 = y1,
            y2 = y2,
            townId = townId,
            ownerId = ownerId,
            type = type,
            name = name,
            permissions = permissions,
            toggles = toggles.copy(
                pvp = pvp ?: toggles.pvp,
                fire = fire ?: toggles.fire,
                explosion = explosion ?: toggles.explosion
            ),
            forSale = forSale,
            salePrice = salePrice,
            plotTax = plotTax,
            modifiedAt = Instant.now()
        )
    }

    /**
     * Creates a new builder for modifying this region.
     */
    fun builder() = RegionBuilder(this)
}
