package it.cava99.towny3d.service

import com.hypixel.hytale.server.core.Message
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.model.ChatMode
import it.cava99.towny3d.model.ResidentId
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Service for managing chat mode routing based on player's chat mode.
 * Processes town, nation, and global chat channels.
 */
class ChatModeService(
    private val plugin: Towny3D
) {
    /**
     * Processes a chat message from a player.
     * Returns the formatted message and target audience.
     */
    suspend fun processChatMessage(
        playerUuid: UUID,
        playerName: String,
        message: String,
        chatModeOverride: ChatMode? = null
    ): ChatResult {
        // Get resident
        val resident = plugin.residentService.getResidentByUuid(playerUuid)
        val chatMode = chatModeOverride ?: resident?.chatMode ?: ChatMode.GLOBAL

        return when (chatMode) {
            ChatMode.TOWN -> processTownChat(resident, playerName, message)
            ChatMode.NATION -> processNationChat(resident, playerName, message)
            ChatMode.GLOBAL -> processGlobalChat(playerName, message)
        }
    }

    /**
     * Processes town chat messages.
     */
    private suspend fun processTownChat(
        resident: it.cava99.towny3d.model.Resident?,
        playerName: String,
        message: String
    ): ChatResult {
        if (resident == null || resident.townId == null) {
            // Player not in a town, fall back to global
            return processGlobalChat(playerName, message)
        }

        val town = plugin.townService.getTown(resident.townId!!)
            ?: return processGlobalChat(playerName, message)

        // Get all residents in the town
        val targetResidents = town.residentIds.mapNotNull { plugin.residentService.getResident(it) }
        val targetPlayerUuids = targetResidents.map { it.id.value }.toSet()

        // Check for spies
        val spies = plugin.residentService.getAllResidents()
            .filter { it.isSpy }
            .map { it.id.value }
            .toSet()

        val allTargets = targetPlayerUuids + spies

        val formattedMessage = formatTownChat(playerName, message)

        return ChatResult(
            message = formattedMessage,
            targets = allTargets,
            channel = "town",
            channelColor = "#55FFFF",
            prefix = "[TC]"
        )
    }

    /**
     * Processes nation chat messages.
     */
    private suspend fun processNationChat(
        resident: it.cava99.towny3d.model.Resident?,
        playerName: String,
        message: String
    ): ChatResult {
        if (resident == null || resident.townId == null) {
            return processGlobalChat(playerName, message)
        }

        val town = plugin.townService.getTown(resident.townId!!)
            ?: return processGlobalChat(playerName, message)

        if (!town.hasNation || town.nationId == null) {
            // Town not in a nation, fall back to town chat
            return processTownChat(resident, playerName, message)
        }

        val nation = plugin.nationService.getNation(town.nationId!!)
            ?: return processTownChat(resident, playerName, message)

        // Get all towns in the nation
        val allTownIds = nation.townIds
        val allResidentIds = allTownIds.flatMap { townId ->
            plugin.townService.getTown(townId)?.residentIds ?: emptyList()
        }

        val targetPlayerUuids = allResidentIds.map { it.value }.toSet()

        // Check for spies
        val spies = plugin.residentService.getAllResidents()
            .filter { it.isSpy }
            .map { it.id.value }
            .toSet()

        val allTargets = targetPlayerUuids + spies

        val formattedMessage = formatNationChat(playerName, message)

        return ChatResult(
            message = formattedMessage,
            targets = allTargets,
            channel = "nation",
            channelColor = "#FFFF55",
            prefix = "[NC]"
        )
    }

    /**
     * Processes global chat messages.
     */
    private suspend fun processGlobalChat(
        playerName: String,
        message: String
    ): ChatResult {
        val formattedMessage = Message.raw("<$playerName>: ").insert(Message.raw(message))

        return ChatResult(
            message = formattedMessage,
            targets = null, // null means all players
            channel = "global",
            channelColor = "#AAAAAA",
            prefix = ""
        )
    }

    /**
     * Formats a town chat message.
     */
    private fun formatTownChat(playerName: String, message: String): Message {
        // Light blue [TC] prefix as requested
        return Message.raw("[TC] ").color("#55FFFF")
            .insert(Message.raw("<$playerName>: "))
            .insert(Message.raw(message))
    }

    /**
     * Formats a nation chat message.
     */
    private fun formatNationChat(playerName: String, message: String): Message {
        // Yellow [NC] prefix
        return Message.raw("[NC] ").color("#FFFF55")
            .insert(Message.raw("<$playerName>: "))
            .insert(Message.raw(message))
    }

    /**
     * Checks if a player can see messages from a specific channel.
     */
    suspend fun canSeeChannel(
        viewerUuid: UUID,
        channel: String,
        senderUuid: UUID
    ): Boolean {
        when (channel) {
            "global" -> return true
            "town" -> {
                val viewer = plugin.residentService.getResidentByUuid(viewerUuid)
                val sender = plugin.residentService.getResidentByUuid(senderUuid)

                // Can see if same town or is spy
                if (viewer?.townId != null && viewer.townId == sender?.townId) return true
                if (viewer?.isSpy == true) return true

                return false
            }
            "nation" -> {
                val viewer = plugin.residentService.getResidentByUuid(viewerUuid)
                val sender = plugin.residentService.getResidentByUuid(senderUuid)

                if (viewer?.townId == null || sender?.townId == null) return false

                val viewerTown = plugin.townService.getTown(viewer.townId!!)
                val senderTown = plugin.townService.getTown(sender.townId!!)

                // Can see if same nation or is spy
                if (viewerTown?.nationId != null && viewerTown.nationId == senderTown?.nationId) return true
                if (viewer?.isSpy == true) return true

                return false
            }
            else -> return true
        }
    }

    /**
     * Result of processing a chat message.
     */
    data class ChatResult(
        val message: com.hypixel.hytale.server.core.Message,
        val targets: Set<UUID>?, // null = all players
        val channel: String,
        val channelColor: String,
        val prefix: String
    )
}
