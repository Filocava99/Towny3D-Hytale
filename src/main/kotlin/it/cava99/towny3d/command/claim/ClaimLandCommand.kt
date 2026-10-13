package it.cava99.towny3d.command.claim

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import it.cava99.towny3d.Towny3D
import it.cava99.towny3d.command.CommandUtils
import it.cava99.towny3d.util.MessageFormatter
import it.cava99.towny3d.util.TownyLog
import kotlinx.coroutines.launch
import javax.annotation.Nonnull

/**
 * /claim land - Claim current chunk or selected region for your town
 */
class ClaimLandCommand : AbstractWorldCommand {

    constructor() : super("land", "Claim current chunk for your town") {
        requirePermission("towny3d.claim.land")
    }

    public override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        val plugin = Towny3D.instance

        if (!context.isPlayer()) {
            context.sendMessage(MessageFormatter.error("Only players can claim land"))
            return
        }

        val playerRef = context.senderAsPlayerRef()
        if (playerRef == null || !playerRef.isValid()) {
            context.sendMessage(MessageFormatter.error("Failed to get player reference"))
            return
        }

        val playerUuid = CommandUtils.getPlayerUuid(playerRef, store)
        val playerPosition = CommandUtils.getPlayerPosition(playerRef, store)

        if (playerUuid == null || playerPosition == null) {
            context.sendMessage(MessageFormatter.error("Failed to get player info"))
            return
        }

        plugin.pluginScope.launch {
            try {
                TownyLog.debug("/claim land: Starting claim process")
                // Get resident
                val resident = plugin.residentService.getResidentByUuid(playerUuid)
                if (resident == null) {
                    context.sendMessage(MessageFormatter.error("Resident not found"))
                    return@launch
                }
                TownyLog.debug("/claim land: Resident found: ${resident.name}")

                // Check if player has a town
                if (resident.townId == null) {
                    context.sendMessage(MessageFormatter.error("You must be a member of a town to claim land"))
                    return@launch
                }
                TownyLog.debug("/claim land: Town ID: ${resident.townId}")

                // Get player's town
                val town = plugin.townService.getTown(resident.townId!!)
                if (town == null) {
                    context.sendMessage(MessageFormatter.error("Town not found"))
                    return@launch
                }
                TownyLog.debug("/claim land: Town found: ${town.name}")

                // Check if player is mayor or assistant
                if (!town.hasLeadership(resident.id)) {
                    context.sendMessage(MessageFormatter.error("Only the mayor or assistants can claim land for the town"))
                    return@launch
                }

                // Check if player has a selection (from /p1 and /p2) - use sync method to avoid mutex deadlock
                TownyLog.debug("/claim land: Checking for complete selection...")
                val hasSelection = plugin.selectionManager.hasCompleteSelectionSync(playerUuid)
                TownyLog.debug("/claim land: Has selection: $hasSelection")

                if (hasSelection) {
                    // Claim the selected region (multi-chunk 3D claim)
                    TownyLog.debug("/claim land: Claiming selected region")
                    claimSelectedRegion(plugin, context, resident, town, playerUuid)
                } else {
                    // Claim just the current chunk
                    TownyLog.debug("/claim land: No selection, claiming current chunk")
                    claimCurrentChunk(plugin, context, resident, town, playerPosition)
                }

            } catch (e: Exception) {
                TownyLog.error("Failed to claim: ${e.message}", e)
                context.sendMessage(MessageFormatter.error("Failed to claim: ${e.message}"))
            }
        }
    }

    private suspend fun claimSelectedRegion(
        plugin: Towny3D,
        context: CommandContext,
        resident: it.cava99.towny3d.model.Resident,
        town: it.cava99.towny3d.model.Town,
        playerUuid: java.util.UUID
    ) {
        try {
            TownyLog.debug("claimSelectedRegion: Starting for ${resident.name}")

            // Use synchronous methods to avoid mutex deadlock
            val volume = plugin.selectionManager.getSelectedVolumeSync(playerUuid)
            TownyLog.debug("claimSelectedRegion: Volume retrieved: $volume")

            val selectedChunks = plugin.selectionManager.getSelectedChunksSync(playerUuid)
            TownyLog.debug("claimSelectedRegion: Selected chunks: ${selectedChunks.size}")

            if (selectedChunks.isEmpty()) {
                context.sendMessage(MessageFormatter.error("Selection contains no chunks"))
                return
            }

            TownyLog.debug("claimSelectedRegion: Checking for overlaps...")
            // Check for overlaps with existing claims
            val overlappingChunks = mutableListOf<it.cava99.towny3d.model.ChunkPos>()
            for (chunk in selectedChunks) {
                TownyLog.debug("claimSelectedRegion: Checking chunk $chunk")
                val existing = plugin.regionService.getRegionsAt(chunk)
                TownyLog.debug("claimSelectedRegion: Chunk $chunk has ${existing.size} existing regions")
                if (existing.isNotEmpty()) {
                    if (existing.first().townId != town.id) {
                        overlappingChunks.add(chunk)
                        TownyLog.debug("claimSelectedRegion: Chunk $chunk overlaps with different town")
                    }
                }
            }
            TownyLog.debug("claimSelectedRegion: Overlap check complete, overlapping: ${overlappingChunks.size}")

            if (overlappingChunks.isNotEmpty()) {
                context.sendMessage(MessageFormatter.error("Cannot claim: overlaps with existing territories"))
                return
            }

            TownyLog.debug("claimSelectedRegion: Calculating cost...")
            // Calculate cost using synchronous methods to avoid mutex deadlock
            val cost = try {
                val yRange = plugin.selectionManager.getSelectedYRangeSync(playerUuid)
                val height = if (yRange != null) yRange.last - yRange.first + 1 else 1
                val chunkCount = selectedChunks.size
                height * chunkCount * plugin.getConfig().layerCost
            } catch (e: Exception) {
                TownyLog.error("Failed to calculate cost: ${e.message}", e)
                0.0
            }
            TownyLog.debug("claimSelectedRegion: Claim cost: $cost")

            // Check if town can afford it
            if (plugin.getConfig().enableEconomy && town.balance < cost) {
                context.sendMessage(
                    MessageFormatter.error("Town cannot afford this claim. Need: ")
                        .insert(Message.raw(plugin.economyService.formatAmount(cost)).color("#FFAA00"))
                )
                return
            }

            TownyLog.debug("claimSelectedRegion: Checking layer limits...")
            // Check layer limits using sync method
            val yRange = plugin.selectionManager.getSelectedYRangeSync(playerUuid)
            val regionHeight = if (yRange != null) yRange.last - yRange.first + 1 else 512 - (-512) + 1
            val totalLayers = regionHeight * selectedChunks.size
            TownyLog.debug("Region height: $regionHeight, Total layers: $totalLayers")

            if (town.layers + totalLayers > town.maxLayers) {
                context.sendMessage(
                    MessageFormatter.error("Insufficient layers. Need: $totalLayers, have available: ${town.maxLayers - town.layers}")
                )
                return
            }

            TownyLog.debug("claimSelectedRegion: Deducting cost...")
            // Deduct cost if economy enabled
            if (plugin.getConfig().enableEconomy && cost > 0) {
                plugin.townService.withdraw(town.id, cost)
            }

            // Create regions for all chunks (using full Y range if no Y selection was made)
            val minY = if (yRange != null) yRange.first else -512
            val maxY = if (yRange != null) yRange.last else 512

            TownyLog.debug("claimSelectedRegion: Creating regions...")
            var claimedCount = 0
            for (chunk in selectedChunks) {
                TownyLog.debug("claimSelectedRegion: Creating region for chunk $chunk")
                plugin.regionService.createRegion(
                    townId = town.id,
                    chunkPos = chunk,
                    y1 = minY,
                    y2 = maxY,
                    type = it.cava99.towny3d.model.RegionType.NORMAL
                )
                claimedCount++
                TownyLog.debug("claimSelectedRegion: Created region $claimedCount/${selectedChunks.size}")
            }

            TownyLog.debug("claimSelectedRegion: Updating town layers...")
            // Update town layers
            plugin.townService.updateLayers(town.id, town.layers + totalLayers)

            TownyLog.debug("claimSelectedRegion: Sending success messages...")
            context.sendMessage(
                MessageFormatter.success("Claimed $claimedCount chunks ($totalLayers layers) for ${town.name}")
            )
            if (volume != null) {
                context.sendMessage(
                    MessageFormatter.info("Region: (${volume.minX}, $minY, ${volume.minZ}) to (${volume.maxX}, $maxY, ${volume.maxZ})")
                )
            }
            if (plugin.getConfig().enableEconomy && cost > 0) {
                try {
                    val costStr = plugin.economyService.formatAmount(cost)
                    context.sendMessage(
                        MessageFormatter.info("Cost: ")
                            .insert(Message.raw(costStr).color("#FFAA00"))
                    )
                } catch (e: Exception) {
                    TownyLog.warn("Failed to format cost: ${e.message}")
                }
            }

            TownyLog.info("${resident.name} claimed $claimedCount chunks for town ${town.name}")

            TownyLog.debug("claimSelectedRegion: Clearing selection...")
            // Clear selection after claiming using sync method
            plugin.selectionManager.clearSelectionSync(playerUuid)
            TownyLog.debug("claimSelectedRegion: Complete!")
        } catch (e: Exception) {
            TownyLog.error("Exception in claimSelectedRegion: ${e.message}", e)
            context.sendMessage(MessageFormatter.error("Failed to claim: ${e.message}"))
        }
    }

    private suspend fun claimCurrentChunk(
        plugin: Towny3D,
        context: CommandContext,
        resident: it.cava99.towny3d.model.Resident,
        town: it.cava99.towny3d.model.Town,
        playerPosition: org.joml.Vector3d
    ) {
        try {
            TownyLog.debug("Starting claimCurrentChunk for ${resident.name}")

            // Get current chunk position
            val blockX = playerPosition.x.toInt()
            val blockZ = playerPosition.z.toInt()
            val chunkX = blockX shr 5  // Divide by 32 for chunk coordinate
            val chunkZ = blockZ shr 5
            val chunkPos = it.cava99.towny3d.model.ChunkPos(chunkX, chunkZ)
            TownyLog.debug("Claiming chunk ($chunkX, $chunkZ)")

            // Check if chunk is already claimed
            val existingRegions = plugin.regionService.getRegionsAt(chunkPos)
            if (existingRegions.isNotEmpty()) {
                val existing = existingRegions.first()
                if (existing.townId == town.id) {
                    context.sendMessage(MessageFormatter.error("This chunk already belongs to your town"))
                } else {
                    context.sendMessage(MessageFormatter.error("This chunk is already claimed by another town"))
                }
                return
            }

            // Check if town has enough layers (using full Y range)
            val regionHeight = 512 - (-512) + 1  // Full world Y range
            TownyLog.debug("Region height: $regionHeight, Town layers: ${town.layers}, Max: ${town.maxLayers}")

            if (town.layers + regionHeight > town.maxLayers) {
                context.sendMessage(MessageFormatter.error("Town has insufficient layers. Current: ${town.layers}, this claim needs: $regionHeight, max: ${town.maxLayers}"))
                return
            }

            // Create the region (using full Y range since no selection was made)
            plugin.regionService.createRegion(
                townId = town.id,
                chunkPos = chunkPos,
                y1 = -512,
                y2 = 512,
                type = it.cava99.towny3d.model.RegionType.NORMAL
            )

            // Update town layers
            plugin.townService.updateLayers(town.id, town.layers + regionHeight)

            context.sendMessage(MessageFormatter.success("Claimed chunk ($chunkX, $chunkZ) for ${town.name}"))
            TownyLog.info("${resident.name} claimed chunk ($chunkX, $chunkZ) for town ${town.name}")
        } catch (e: Exception) {
            TownyLog.error("Exception in claimCurrentChunk: ${e.message}", e)
            context.sendMessage(MessageFormatter.error("Failed to claim: ${e.message}"))
        }
    }
}
