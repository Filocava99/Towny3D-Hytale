package it.cava99.towny3d.service

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.component.Store
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.translation.TranslationManager
import it.cava99.towny3d.util.MessageFormatter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Dependencies required by InvitationService.
 */
data class InvitationDependencies(
    val townService: TownService,
    val nationService: NationService,
    val residentService: ResidentService,
    val translationManager: TranslationManager
)

/**
 * Manages invitations for towns and nations.
 * Handles invite creation, acceptance, rejection, and expiration.
 */
class InvitationService(
    private val deps: InvitationDependencies
) {
    // Private getters for cleaner access to dependencies
    private val townService get() = deps.townService
    private val nationService get() = deps.nationService
    private val residentService get() = deps.residentService
    private val translationManager get() = deps.translationManager
    private val mutex = Mutex()

    // Active invitations
    private val townInvites = ConcurrentHashMap<UUID, TownInvite>()
    private val nationInvites = ConcurrentHashMap<UUID, NationInvite>()

    // Invitation expiration time (5 minutes)
    private val inviteExpirationMs = 5 * 60 * 1000L

    /**
     * Gets a PlayerRef from a UUID to send messages to the player.
     * Returns null if the player is not online.
     */
    private fun getPlayerRef(playerUuid: UUID): PlayerRef? {
        return try {
            val universe = Universe.get()
            val world = universe.getDefaultWorld() ?: return null

            var playerRef: PlayerRef? = null
            world.execute {
                val entityStore = world.getEntityStore()
                val ref = entityStore.getRefFromUUID(playerUuid) ?: return@execute
                val store: Store<EntityStore> = entityStore.getStore()
                playerRef = store.getComponent(ref, PlayerRef.getComponentType())
            }
            playerRef
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Data class for town invitations.
     */
    data class TownInvite(
        val inviteId: UUID,
        val townId: TownId,
        val inviterId: ResidentId,
        val targetName: String,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > 5 * 60 * 1000L
    }

    /**
     * Data class for nation invitations.
     */
    data class NationInvite(
        val inviteId: UUID,
        val nationId: it.cava99.towny3d.model.NationId,
        val inviterId: ResidentId,
        val targetTownId: TownId,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > 5 * 60 * 1000L
    }

    /**
     * Invites a player to a town.
     * @return The invite ID
     */
    suspend fun inviteToTown(
        townId: TownId,
        inviterId: ResidentId,
        targetName: String
    ): UUID = mutex.withLock {
        // Validate town exists
        val town = townService.getTown(townId)
            ?: throw IllegalArgumentException("Town not found")

        // Validate inviter is mayor or assistant
        if (town.mayorId != inviterId && inviterId !in town.assistantIds) {
            throw IllegalArgumentException("Only mayor and assistants can invite players")
        }

        // Check if town is open (no invite needed)
        if (town.isOpen) {
            throw IllegalArgumentException("Town is open - players can join without invite")
        }

        // Find target resident
        val target = residentService.getResidentByName(targetName)
        if (target != null) {
            // Check if already in a town
            if (target.townId != null) {
                throw IllegalArgumentException("Player is already in a town")
            }
        }

        // Create invite
        val inviteId = UUID.randomUUID()
        val invite = TownInvite(inviteId, townId, inviterId, targetName)
        townInvites[inviteId] = invite

        // Send notification to target player if online
        if (target != null) {
            val targetUuid = target.id.value
            val playerRef = getPlayerRef(targetUuid)
            if (playerRef != null) {
                // Send invitation notification
                playerRef.sendMessage(
                    MessageFormatter.success("town.invite_received", "town" to town.name)
                )
                playerRef.sendMessage(
                    MessageFormatter.info("town.invite_how_to_accept",
                        "inviteId" to inviteId.toString(),
                        "town" to town.name
                    )
                )
            }
        }

        return inviteId
    }

    /**
     * Accepts a town invitation.
     */
    suspend fun acceptTownInvite(
        residentId: ResidentId,
        inviteId: UUID
    ): AcceptResult {
        mutex.withLock {
            val invite = townInvites[inviteId]

            if (invite == null) {
                return AcceptResult.NOT_FOUND
            }

            if (invite.isExpired()) {
                townInvites.remove(inviteId)
                return AcceptResult.EXPIRED
            }

            // Verify resident matches invite target
            val resident = residentService.getResident(residentId)
                ?: return AcceptResult.NOT_RESIDENT

            // Note: We check by name since target might not be loaded yet
            if (resident.name != invite.targetName) {
                return AcceptResult.NOT_TARGET
            }

            // Check if resident already has a town
            if (resident.townId != null) {
                return AcceptResult.ALREADY_IN_TOWN
            }

            // Add to town
            try {
                townService.addResident(invite.townId, residentId)
                residentService.setTown(residentId, invite.townId)

                // Remove invite
                townInvites.remove(inviteId)

                return AcceptResult.SUCCESS
            } catch (e: Exception) {
                return AcceptResult.ERROR
            }
        }
    }

    /**
     * Rejects a town invitation.
     */
    suspend fun rejectTownInvite(residentId: ResidentId, inviteId: UUID): Boolean {
        mutex.withLock {
            val invite = townInvites[inviteId] ?: return false

            // Verify resident matches
            val resident = residentService.getResident(residentId)
                ?: return false

            if (resident.name != invite.targetName) {
                return false
            }

            townInvites.remove(inviteId)
            return true
        }
    }

    /**
     * Invites a town to a nation.
     * @return The invite ID
     */
    suspend fun inviteTownToNation(
        nationId: it.cava99.towny3d.model.NationId,
        inviterId: ResidentId,
        targetTownId: TownId
    ): UUID = mutex.withLock {
        // Validate nation exists
        val nation = nationService.getNation(nationId)
            ?: throw IllegalArgumentException("Nation not found")

        // Validate inviter is king
        if (nation.kingId != inviterId) {
            throw IllegalArgumentException("Only the king can invite towns")
        }

        // Validate target town exists
        val targetTown = townService.getTown(targetTownId)
            ?: throw IllegalArgumentException("Town not found")

        // Check if town is already in a nation
        if (targetTown.hasNation) {
            throw IllegalArgumentException("Town is already in a nation")
        }

        // Create invite
        val inviteId = UUID.randomUUID()
        val invite = NationInvite(inviteId, nationId, inviterId, targetTownId)
        nationInvites[inviteId] = invite

        // Send notification to town mayor
        val mayor = residentService.getResident(targetTown.mayorId)
        if (mayor != null) {
            val mayorPlayerRef = getPlayerRef(mayor.id.value)
            if (mayorPlayerRef != null) {
                mayorPlayerRef.sendMessage(
                    MessageFormatter.success("nation.invite_received", "nation" to nation.name)
                )
                mayorPlayerRef.sendMessage(
                    MessageFormatter.info("nation.invite_how_to_accept",
                        "inviteId" to inviteId.toString(),
                        "nation" to nation.name
                    )
                )
            }
        }

        return inviteId
    }

    /**
     * Accepts a nation invitation.
     */
    suspend fun acceptNationInvite(
        townId: TownId,
        inviteId: UUID
    ): AcceptResult {
        mutex.withLock {
            val invite = nationInvites[inviteId]

            if (invite == null) {
                return AcceptResult.NOT_FOUND
            }

            if (invite.isExpired()) {
                nationInvites.remove(inviteId)
                return AcceptResult.EXPIRED
            }

            // Verify town matches invite target
            if (townId != invite.targetTownId) {
                return AcceptResult.NOT_TARGET
            }

            // Add to nation
            try {
                nationService.addTown(invite.nationId, townId)
                townService.setNation(townId, invite.nationId)

                // Remove invite
                nationInvites.remove(inviteId)

                return AcceptResult.SUCCESS
            } catch (e: Exception) {
                return AcceptResult.ERROR
            }
        }
    }

    /**
     * Rejects a nation invitation.
     */
    suspend fun rejectNationInvite(townId: TownId, inviteId: UUID): Boolean {
        mutex.withLock {
            val invite = nationInvites[inviteId] ?: return false

            if (townId != invite.targetTownId) {
                return false
            }

            nationInvites.remove(inviteId)
            return true
        }
    }

    /**
     * Gets all pending town invites for a player.
     */
    suspend fun getTownInvitesForPlayer(playerName: String): List<TownInvite> {
        cleanupExpired()
        return townInvites.values.filter { it.targetName.equals(playerName, ignoreCase = true) }
    }

    /**
     * Gets all pending nation invites for a town.
     */
    suspend fun getNationInvitesForTown(townId: TownId): List<NationInvite> {
        cleanupExpired()
        return nationInvites.values.filter { it.targetTownId == townId }
    }

    /**
     * Gets a town invite by ID.
     */
    fun getTownInvite(inviteId: UUID): TownInvite? {
        return townInvites[inviteId]
    }

    /**
     * Gets a nation invite by ID.
     */
    fun getNationInvite(inviteId: UUID): NationInvite? {
        return nationInvites[inviteId]
    }

    /**
     * Cleans up expired invitations.
     */
    private fun cleanupExpired() {
        val now = System.currentTimeMillis()

        townInvites.entries.removeIf { it.value.isExpired() }
        nationInvites.entries.removeIf { it.value.isExpired() }
    }

    /**
     * Clears all invitations (for testing or shutdown).
     */
    suspend fun clearAll() = mutex.withLock {
        townInvites.clear()
        nationInvites.clear()
    }

    /**
     * Result of accepting an invitation.
     */
    enum class AcceptResult {
        SUCCESS,
        NOT_FOUND,
        EXPIRED,
        NOT_TARGET,
        ALREADY_IN_TOWN,
        NOT_RESIDENT,
        ERROR
    }

    /**
     * Gets invitation statistics.
     */
    fun getStats(): InvitationStats {
        cleanupExpired()
        return InvitationStats(
            pendingTownInvites = townInvites.size,
            pendingNationInvites = nationInvites.size
        )
    }

    data class InvitationStats(
        val pendingTownInvites: Int,
        val pendingNationInvites: Int
    )
}
