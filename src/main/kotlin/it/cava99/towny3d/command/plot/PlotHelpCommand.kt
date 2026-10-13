package it.cava99.towny3d.command.plot

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.World
import it.cava99.towny3d.util.MessageFormatter
import javax.annotation.Nonnull

/**
 * /plot help - Show plot command help
 */
class PlotHelpCommand : AbstractWorldCommand {

    constructor() : super("help", "Show plot command help") {
        requirePermission("towny3d.plot")
    }

    protected override fun execute(@Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>) {
        context.sendMessage(Message.raw("=== Plot Commands ===").color("#FFAA00").bold(true))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/plot claim - Claim the plot you're standing in").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot unclaim - Unclaim your plot").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot forsale <price> - Put plot up for sale (alias: /plot fs)").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot notforsale - Remove plot from market (alias: /plot nfs)").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot info - View plot information").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/plot tax <flat|percent> <amount> - Set tax for this plot").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot tax info - View plot tax").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot tax clear - Clear plot tax").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("/plot set embassy - Mark plot as embassy").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot set perm <group> <type> <value> - Set permissions").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot set reset - Reset plot to default").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot toggle <flag> <on/off> - Toggle plot flags").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot name <name> - Name your plot").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot showBorders - Show plot borders (alias: /plot sb)").color("#55FFFF"))
        context.sendMessage(Message.raw("/plot hideBorders - Hide plot borders (alias: /plot hb)").color("#55FFFF"))
        context.sendMessage(Message.raw(""))
        context.sendMessage(Message.raw("Permission groups: res, ally, gen").color("#AAAAAA"))
        context.sendMessage(Message.raw("Permission types: pvp, interact, inventory, build, destroy, drop, pick, move").color("#AAAAAA"))
        context.sendMessage(Message.raw("Plot flags: fire, explosion, friendly_mobs_spawn, hostile_mobs_spawn").color("#AAAAAA"))
    }
}
