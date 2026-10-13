package it.cava99.towny3d.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a player (resident) in the plugin.
 * Residents are immutable for thread safety. Use copy() to create modified versions.
 *
 * @property id Unique identifier (matches player UUID)
 * @property name The player's name
 * @property townId The ID of the town this resident belongs to (null if wilderness)
 * @property chatMode The current chat mode (GLOBAL, TOWN, NATION)
 * @property isSpy Whether this resident is an admin spy (sees all chat)
 * @property balance The player's money balance
 * @property lastOnline When this player was last online
 * @property joinedAt When this player first joined the server
 * @property modifiedAt When this resident was last modified
 */
data class Resident(
    val id: ResidentId,
    val name: String,
    val townId: TownId? = null,
    val chatMode: ChatMode = ChatMode.GLOBAL,
    val isSpy: Boolean = false,
    val balance: Double = 0.0,
    val lastOnline: Instant = Instant.now(),
    val joinedAt: Instant = Instant.now(),
    val modifiedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Resident name cannot be blank" }
        require(balance >= 0) { "Balance cannot be negative, got $balance" }
    }

    /**
     * Returns the player's UUID.
     */
    val uuid: UUID get() = id.value

    /**
     * Checks if this resident is in a town.
     */
    val hasTown: Boolean get() = townId != null

    /**
     * Checks if this resident is in wilderness (no town).
     */
    val isWilderness: Boolean get() = townId == null

    /**
     * Checks if this resident is currently spying on chat.
     */
    val isSpying: Boolean get() = isSpy

    /**
     * Checks if chat messages should be sent to town chat.
     */
    val isTownChat: Boolean get() = chatMode == ChatMode.TOWN

    /**
     * Checks if chat messages should be sent to nation chat.
     */
    val isNationChat: Boolean get() = chatMode == ChatMode.NATION

    /**
     * Returns true if this resident has sufficient balance.
     */
    fun hasBalance(amount: Double): Boolean = balance >= amount

    /**
     * Creates a modified copy of this resident with updated modification timestamp.
     */
    fun withModification(
        name: String = this.name,
        townId: TownId? = this.townId,
        chatMode: ChatMode = this.chatMode,
        isSpy: Boolean = this.isSpy,
        balance: Double = this.balance,
        lastOnline: Instant = this.lastOnline
    ): Resident = copy(
        name = name,
        townId = townId,
        chatMode = chatMode,
        isSpy = isSpy,
        balance = balance,
        lastOnline = lastOnline,
        modifiedAt = Instant.now()
    )

    /**
     * Creates a copy with town chat enabled.
     */
    fun withTownChat(): Resident = copy(chatMode = ChatMode.TOWN, modifiedAt = Instant.now())

    /**
     * Creates a copy with nation chat enabled.
     */
    fun withNationChat(): Resident = copy(chatMode = ChatMode.NATION, modifiedAt = Instant.now())

    /**
     * Creates a copy with global chat enabled.
     */
    fun withGlobalChat(): Resident = copy(chatMode = ChatMode.GLOBAL, modifiedAt = Instant.now())

    /**
     * Creates a copy with updated balance.
     */
    fun withBalance(newBalance: Double): Resident = copy(balance = newBalance, modifiedAt = Instant.now())

    /**
     * Creates a copy with balance modified (adds or subtracts).
     */
    fun withBalanceChange(delta: Double): Resident {
        val newBalance = balance + delta
        require(newBalance >= 0) { "Balance cannot go negative, current: $balance, delta: $delta" }
        return copy(balance = newBalance, modifiedAt = Instant.now())
    }

    companion object {
        /**
         * Creates a new resident from a player join.
         */
        fun create(uuid: UUID, name: String): Resident = Resident(
            id = ResidentId.fromPlayerUuid(uuid),
            name = name,
            joinedAt = Instant.now(),
            lastOnline = Instant.now()
        )

        /**
         * Creates a new resident with explicit ResidentId.
         */
        fun create(id: ResidentId, name: String): Resident = Resident(
            id = id,
            name = name,
            joinedAt = Instant.now(),
            lastOnline = Instant.now()
        )
    }
}
