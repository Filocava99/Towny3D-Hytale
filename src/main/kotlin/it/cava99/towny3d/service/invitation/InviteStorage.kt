package it.cava99.towny3d.service.invitation

import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage for active invitations with expiration tracking.
 */
class InviteStorage {
    private val townInvites = ConcurrentHashMap<UUID, TownInvite>()
    private val nationInvites = ConcurrentHashMap<UUID, NationInvite>()

    companion object {
        private const val INVITE_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    data class TownInvite(
        val inviteId: UUID,
        val townId: TownId,
        val inviterId: ResidentId,
        val targetName: String,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > INVITE_EXPIRATION_MS
    }

    data class NationInvite(
        val inviteId: UUID,
        val nationId: NationId,
        val inviterId: ResidentId,
        val targetTownId: TownId,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > INVITE_EXPIRATION_MS
    }

    // Town invite operations
    fun putTownInvite(id: UUID, invite: TownInvite) { townInvites[id] = invite }
    fun getTownInvite(id: UUID): TownInvite? = townInvites[id]
    fun removeTownInvite(id: UUID) { townInvites.remove(id) }
    fun getAllTownInvites(): List<TownInvite> = townInvites.values.toList()

    // Nation invite operations
    fun putNationInvite(id: UUID, invite: NationInvite) { nationInvites[id] = invite }
    fun getNationInvite(id: UUID): NationInvite? = nationInvites[id]
    fun removeNationInvite(id: UUID) { nationInvites.remove(id) }
    fun getAllNationInvites(): List<NationInvite> = nationInvites.values.toList()

    // Cleanup
    fun cleanupExpired() {
        townInvites.entries.removeIf { it.value.isExpired() }
        nationInvites.entries.removeIf { it.value.isExpired() }
    }

    fun clearAll() {
        townInvites.clear()
        nationInvites.clear()
    }

    // Stats
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
