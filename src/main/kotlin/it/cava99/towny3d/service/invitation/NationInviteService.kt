package it.cava99.towny3d.service.invitation

import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.NationService
import it.cava99.towny3d.service.TownService
import it.cava99.towny3d.service.ResidentService
import it.cava99.towny3d.util.MessageFormatter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Handles nation invitation operations.
 */
class NationInviteService(
    private val nationService: NationService,
    private val townService: TownService,
    private val residentService: ResidentService,
    private val inviteStorage: InviteStorage
) {
    private val mutex = Mutex()

    suspend fun inviteTownToNation(
        nationId: NationId,
        inviterId: ResidentId,
        targetTownId: TownId
    ): UUID = mutex.withLock {
        val nation = nationService.getNation(nationId)
            ?: throw IllegalArgumentException("Nation not found")

        if (nation.kingId != inviterId) {
            throw IllegalArgumentException("Only the king can invite towns")
        }

        val targetTown = townService.getTown(targetTownId)
            ?: throw IllegalArgumentException("Town not found")

        if (targetTown.hasNation) {
            throw IllegalArgumentException("Town is already in a nation")
        }

        val inviteId = UUID.randomUUID()
        val invite = InviteStorage.NationInvite(inviteId, nationId, inviterId, targetTownId)
        inviteStorage.putNationInvite(inviteId, invite)

        val mayor = residentService.getResident(targetTown.mayorId)
        if (mayor != null) {
            val mayorPlayerRef = getPlayerRef(mayor.id.value)
            if (mayorPlayerRef != null) {
                mayorPlayerRef.sendMessage(MessageFormatter.success("nation.invite_received", "nation" to nation.name))
                mayorPlayerRef.sendMessage(MessageFormatter.info("nation.invite_how_to_accept",
                    "inviteId" to inviteId.toString(), "nation" to nation.name))
            }
        }

        return inviteId
    }

    suspend fun acceptNationInvite(townId: TownId, inviteId: UUID): AcceptResult {
        mutex.withLock {
            val invite = inviteStorage.getNationInvite(inviteId) ?: return AcceptResult.NOT_FOUND

            if (invite.isExpired()) {
                inviteStorage.removeNationInvite(inviteId)
                return AcceptResult.EXPIRED
            }

            if (townId != invite.targetTownId) return AcceptResult.NOT_TARGET

            return try {
                nationService.addTown(invite.nationId, townId)
                townService.setNation(townId, invite.nationId)
                inviteStorage.removeNationInvite(inviteId)
                AcceptResult.SUCCESS
            } catch (e: Exception) {
                AcceptResult.ERROR
            }
        }
    }

    suspend fun rejectNationInvite(townId: TownId, inviteId: UUID): Boolean {
        mutex.withLock {
            val invite = inviteStorage.getNationInvite(inviteId) ?: return false
            if (townId != invite.targetTownId) return false
            inviteStorage.removeNationInvite(inviteId)
            return true
        }
    }

    suspend fun getNationInvitesForTown(townId: TownId): List<InviteStorage.NationInvite> {
        inviteStorage.cleanupExpired()
        return inviteStorage.getAllNationInvites().filter { it.targetTownId == townId }
    }

    private fun getPlayerRef(playerUuid: UUID): com.hypixel.hytale.server.core.universe.PlayerRef? {
        return try {
            val universe = com.hypixel.hytale.server.core.universe.Universe.get()
            val world = universe.getDefaultWorld() ?: return null
            var playerRef: com.hypixel.hytale.server.core.universe.PlayerRef? = null
            world.execute {
                val entityStore = world.getEntityStore()
                val ref = entityStore.getRefFromUUID(playerUuid) ?: return@execute
                val store = entityStore.getStore()
                playerRef = store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType())
            }
            playerRef
        } catch (e: Exception) { null }
    }
}
