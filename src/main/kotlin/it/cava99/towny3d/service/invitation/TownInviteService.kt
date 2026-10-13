package it.cava99.towny3d.service.invitation

import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TownId
import it.cava99.towny3d.service.TownService
import it.cava99.towny3d.service.ResidentService
import it.cava99.towny3d.util.MessageFormatter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles town invitation operations.
 */
class TownInviteService(
    private val townService: TownService,
    private val residentService: ResidentService,
    private val inviteStorage: InviteStorage
) {
    private val mutex = Mutex()

    suspend fun inviteToTown(
        townId: TownId,
        inviterId: ResidentId,
        targetName: String
    ): UUID = mutex.withLock {
        val town = townService.getTown(townId)
            ?: throw IllegalArgumentException("Town not found")

        if (town.mayorId != inviterId && inviterId !in town.assistantIds) {
            throw IllegalArgumentException("Only mayor and assistants can invite players")
        }

        if (town.isOpen) {
            throw IllegalArgumentException("Town is open - players can join without invite")
        }

        val target = residentService.getResidentByName(targetName)
        if (target != null && target.townId != null) {
            throw IllegalArgumentException("Player is already in a town")
        }

        val inviteId = UUID.randomUUID()
        val invite = InviteStorage.TownInvite(inviteId, townId, inviterId, targetName)
        inviteStorage.putTownInvite(inviteId, invite)

        if (target != null) {
            val playerRef = getPlayerRef(target.id.value)
            if (playerRef != null) {
                playerRef.sendMessage(MessageFormatter.success("town.invite_received", "town" to town.name))
                playerRef.sendMessage(MessageFormatter.info("town.invite_how_to_accept",
                    "inviteId" to inviteId.toString(), "town" to town.name))
            }
        }

        return inviteId
    }

    suspend fun acceptTownInvite(residentId: ResidentId, inviteId: UUID): AcceptResult {
        mutex.withLock {
            val invite = inviteStorage.getTownInvite(inviteId) ?: return AcceptResult.NOT_FOUND

            if (invite.isExpired()) {
                inviteStorage.removeTownInvite(inviteId)
                return AcceptResult.EXPIRED
            }

            val resident = residentService.getResident(residentId) ?: return AcceptResult.NOT_RESIDENT

            if (resident.name != invite.targetName) return AcceptResult.NOT_TARGET
            if (resident.townId != null) return AcceptResult.ALREADY_IN_TOWN

            return try {
                townService.addResident(invite.townId, residentId)
                residentService.setTown(residentId, invite.townId)
                inviteStorage.removeTownInvite(inviteId)
                AcceptResult.SUCCESS
            } catch (e: Exception) {
                AcceptResult.ERROR
            }
        }
    }

    suspend fun rejectTownInvite(residentId: ResidentId, inviteId: UUID): Boolean {
        mutex.withLock {
            val invite = inviteStorage.getTownInvite(inviteId) ?: return false
            val resident = residentService.getResident(residentId) ?: return false
            if (resident.name != invite.targetName) return false
            inviteStorage.removeTownInvite(inviteId)
            return true
        }
    }

    suspend fun getTownInvitesForPlayer(playerName: String): List<InviteStorage.TownInvite> {
        inviteStorage.cleanupExpired()
        return inviteStorage.getAllTownInvites().filter { it.targetName.equals(playerName, ignoreCase = true) }
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

enum class AcceptResult {
    SUCCESS, NOT_FOUND, EXPIRED, NOT_TARGET, ALREADY_IN_TOWN, NOT_RESIDENT, ERROR
}
