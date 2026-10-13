package it.cava99.towny3d.storage

import com.google.gson.*
import it.cava99.towny3d.model.ChatMode
import it.cava99.towny3d.model.ChunkPos
import it.cava99.towny3d.model.NationId
import it.cava99.towny3d.model.RegionId
import it.cava99.towny3d.model.Resident
import it.cava99.towny3d.model.ResidentId
import it.cava99.towny3d.model.TaxConfig
import it.cava99.towny3d.model.TaxType
import it.cava99.towny3d.model.TownId
import java.lang.reflect.Type
import java.time.Instant
import java.util.UUID

/**
 * Gson type adapters for Towny3D model serialization.
 *
 * Contains all custom serializers/deserializers for:
 * - ID value classes (RegionId, TownId, NationId, ResidentId)
 * - Position classes (ChunkPos)
 * - Time classes (Instant)
 * - Standard types (UUID, ChatMode)
 * - Complex models (Resident, TaxConfig, Town)
 *
 * This file is intentionally large because:
 * - All adapters are registered together with Gson and must be kept in sync
 * - Each adapter follows the same pattern (serialize/deserialize) and is small
 * - Splitting would require importing Gson in multiple files and managing multiple registrations
 * - The adapters have no dependencies on each other, so they're truly just a collection
 *
 * Future improvements:
 * - Could potentially use annotation-based serialization to reduce boilerplate
 * - Consider generating adapters programmatically for simple value classes
 */

/**
 * Gson adapter for RegionId value class.
 * Serializes as UUID string.
 */
class RegionIdAdapter : JsonSerializer<RegionId>, JsonDeserializer<RegionId> {
    override fun serialize(src: RegionId, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.value.toString())
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): RegionId {
        val uuid = UUID.fromString(json.asString)
        return RegionId(uuid)
    }
}

/**
 * Gson adapter for TownId value class.
 * Serializes as UUID string.
 */
class TownIdAdapter : JsonSerializer<TownId>, JsonDeserializer<TownId> {
    override fun serialize(src: TownId, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.value.toString())
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): TownId {
        val uuid = UUID.fromString(json.asString)
        return TownId(uuid)
    }
}

/**
 * Gson adapter for NationId value class.
 * Serializes as UUID string.
 */
class NationIdAdapter : JsonSerializer<NationId>, JsonDeserializer<NationId> {
    override fun serialize(src: NationId, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.value.toString())
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): NationId {
        val uuid = UUID.fromString(json.asString)
        return NationId(uuid)
    }
}

/**
 * Gson adapter for ResidentId value class.
 * Serializes as UUID string.
 */
class ResidentIdAdapter : JsonSerializer<ResidentId>, JsonDeserializer<ResidentId> {
    override fun serialize(src: ResidentId, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.value.toString())
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): ResidentId {
        val uuid = UUID.fromString(json.asString)
        return ResidentId(uuid)
    }
}

/**
 * Gson adapter for ChunkPos value class.
 * Serializes as packed long.
 */
class ChunkPosAdapter : JsonSerializer<ChunkPos>, JsonDeserializer<ChunkPos> {
    override fun serialize(src: ChunkPos, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.packed)
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): ChunkPos {
        return ChunkPos(json.asLong)
    }
}

/**
 * Gson adapter for Instant (Java 8 time API).
 * Serializes as ISO-8601 string.
 */
class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Instant {
        return Instant.parse(json.asString)
    }
}

/**
 * Gson adapter for UUID.
 * Serializes as string.
 */
class UUIDAdapter : JsonSerializer<UUID>, JsonDeserializer<UUID> {
    override fun serialize(src: UUID, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): UUID {
        return UUID.fromString(json.asString)
    }
}

/**
 * Gson adapter for ChatMode enum.
 * Serializes as string name.
 */
class ChatModeAdapter : JsonSerializer<ChatMode>, JsonDeserializer<ChatMode> {
    override fun serialize(src: ChatMode, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.name)
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): ChatMode {
        return ChatMode.valueOf(json.asString)
    }
}

/**
 * Gson adapter for Resident data class.
 * Manually serializes/deserializes to ensure balance is always saved.
 */
class ResidentAdapter : JsonSerializer<Resident>, JsonDeserializer<Resident> {
    override fun serialize(src: Resident, type: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        
        // Serialize all fields explicitly to ensure balance is saved
        obj.addProperty("id", src.id.value.toString())
        obj.addProperty("name", src.name)
        obj.add("townId", src.townId?.let { JsonPrimitive(it.value.toString()) })
        obj.addProperty("chatMode", src.chatMode.name)
        obj.addProperty("isSpy", src.isSpy)
        obj.addProperty("balance", src.balance)
        obj.addProperty("lastOnline", src.lastOnline.toString())
        obj.addProperty("joinedAt", src.joinedAt.toString())
        obj.addProperty("modifiedAt", src.modifiedAt.toString())
        
        return obj
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Resident {
        val obj = json.asJsonObject
        
        val id = UUID.fromString(obj.get("id").asString)
        val name = obj.get("name").asString
        val townIdStr = obj.get("townId")?.asString
        val chatModeStr = obj.get("chatMode")?.asString ?: "GLOBAL"
        val isSpy = obj.get("isSpy")?.asBoolean ?: false
        val balance = obj.get("balance")?.asDouble ?: 0.0
        val lastOnline = Instant.parse(obj.get("lastOnline").asString)
        val joinedAt = Instant.parse(obj.get("joinedAt").asString)
        val modifiedAt = Instant.parse(obj.get("modifiedAt").asString)
        
        return Resident(
            id = ResidentId(id),
            name = name,
            townId = townIdStr?.let { TownId(UUID.fromString(it)) },
            chatMode = ChatMode.valueOf(chatModeStr),
            isSpy = isSpy,
            balance = balance,
            lastOnline = lastOnline,
            joinedAt = joinedAt,
            modifiedAt = modifiedAt
        )
    }
}

/**
 * Gson adapter for TaxConfig data class.
 * Properly serializes/deserializes TaxConfig with its TaxType enum.
 */
class TaxConfigAdapter : JsonSerializer<TaxConfig>, JsonDeserializer<TaxConfig> {
    override fun serialize(src: TaxConfig, type: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("enabled", src.enabled)
        obj.addProperty("type", src.type.name)
        obj.addProperty("amount", src.amount)
        return obj
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): TaxConfig {
        val obj = json.asJsonObject
        val enabled = obj.get("enabled")?.asBoolean ?: false
        val typeStr = obj.get("type")?.asString ?: TaxType.FLAT.name
        val amount = obj.get("amount")?.asDouble ?: 0.0

        return TaxConfig(
            enabled = enabled,
            type = TaxType.valueOf(typeStr),
            amount = amount
        )
    }
}

/**
 * Gson adapter for Town data class.
 * Manually serializes/deserializes to ensure critical fields like isOpen, isPublic, isRuined are always saved.
 */
class TownAdapter : JsonSerializer<it.cava99.towny3d.model.Town>, JsonDeserializer<it.cava99.towny3d.model.Town> {
    override fun serialize(src: it.cava99.towny3d.model.Town, type: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        // Serialize all fields explicitly to ensure critical settings are saved
        obj.addProperty("id", src.id.value.toString())
        obj.addProperty("name", src.name)
        obj.add("nationId", src.nationId?.let { com.google.gson.JsonPrimitive(it.value.toString()) })
        obj.addProperty("mayorId", src.mayorId.value.toString())
        obj.add("assistantIds", context.serialize(src.assistantIds))
        obj.add("residentIds", context.serialize(src.residentIds))
        obj.addProperty("balance", src.balance)
        obj.addProperty("layers", src.layers)
        obj.addProperty("maxLayers", src.maxLayers)
        obj.add("homeLocation", context.serialize(src.homeLocation))
        obj.addProperty("board", src.board)
        obj.addProperty("isPublic", src.isPublic)
        obj.addProperty("isOpen", src.isOpen)
        obj.addProperty("mapColor", src.mapColor)
        obj.addProperty("createdAt", src.createdAt.toString())
        obj.addProperty("modifiedAt", src.modifiedAt.toString())

        // Taxation fields
        obj.add("taxToResidents", context.serialize(src.taxToResidents))
        obj.addProperty("nationDebt", src.nationDebt)
        obj.addProperty("lastTaxCollection", src.lastTaxCollection.toString())

        // Ruined state
        obj.addProperty("isRuined", src.isRuined)
        obj.add("ruinedAt", src.ruinedAt?.let { com.google.gson.JsonPrimitive(it.toString()) })

        return obj
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): it.cava99.towny3d.model.Town {
        val obj = json.asJsonObject

        val id = java.util.UUID.fromString(obj.get("id").asString)
        val name = obj.get("name").asString
        val nationIdStr = obj.get("nationId")?.asString
        val mayorId = java.util.UUID.fromString(obj.get("mayorId").asString)
        val assistantIds = context.deserialize<Set<it.cava99.towny3d.model.ResidentId>>(
            obj.get("assistantIds"),
            object : com.google.gson.reflect.TypeToken<Set<it.cava99.towny3d.model.ResidentId>>() {}.type
        ) ?: emptySet()
        val residentIds = context.deserialize<Set<it.cava99.towny3d.model.ResidentId>>(
            obj.get("residentIds"),
            object : com.google.gson.reflect.TypeToken<Set<it.cava99.towny3d.model.ResidentId>>() {}.type
        ) ?: emptySet()
        val balance = obj.get("balance")?.asDouble ?: 0.0
        val layers = obj.get("layers")?.asInt ?: 0
        val maxLayers = obj.get("maxLayers")?.asInt ?: 256
        val homeLocation = context.deserialize<it.cava99.towny3d.model.BlockLocation>(
            obj.get("homeLocation"),
            it.cava99.towny3d.model.BlockLocation::class.java
        )
        val board = obj.get("board")?.asString ?: ""
        val isPublic = obj.get("isPublic")?.asBoolean ?: false
        val isOpen = obj.get("isOpen")?.asBoolean ?: false
        val mapColor = obj.get("mapColor")?.asInt ?: it.cava99.towny3d.model.Town.generateRandomColor()
        val createdAt = java.time.Instant.parse(obj.get("createdAt").asString)
        val modifiedAt = java.time.Instant.parse(obj.get("modifiedAt").asString)

        // Taxation fields
        val taxToResidents = context.deserialize<it.cava99.towny3d.model.TaxConfig>(
            obj.get("taxToResidents"),
            it.cava99.towny3d.model.TaxConfig::class.java
        ) ?: it.cava99.towny3d.model.TaxConfig()
        val nationDebt = obj.get("nationDebt")?.asDouble ?: 0.0
        val lastTaxCollection = java.time.Instant.parse(obj.get("lastTaxCollection").asString)

        // Ruined state
        val isRuined = obj.get("isRuined")?.asBoolean ?: false
        val ruinedAtStr = obj.get("ruinedAt")?.asString
        val ruinedAt = ruinedAtStr?.let { java.time.Instant.parse(it) }

        return it.cava99.towny3d.model.Town(
            id = it.cava99.towny3d.model.TownId(id),
            name = name,
            nationId = nationIdStr?.let { uuid -> it.cava99.towny3d.model.NationId(java.util.UUID.fromString(uuid)) },
            mayorId = it.cava99.towny3d.model.ResidentId(mayorId),
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
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            taxToResidents = taxToResidents,
            nationDebt = nationDebt,
            lastTaxCollection = lastTaxCollection,
            isRuined = isRuined,
            ruinedAt = ruinedAt
        )
    }
}
