package it.cava99.towny3d.model

/**
 * Represents the chat mode for a resident.
 */
enum class ChatMode {
    /**
     * Global chat - messages sent to all players.
     */
    GLOBAL,

    /**
     * Town chat - messages sent only to town members.
     */
    TOWN,

    /**
     * Nation chat - messages sent only to nation members.
     */
    NATION
}
