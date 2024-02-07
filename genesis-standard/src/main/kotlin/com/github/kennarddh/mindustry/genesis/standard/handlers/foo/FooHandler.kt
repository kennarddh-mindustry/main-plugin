package com.github.kennarddh.mindustry.genesis.standard.handlers.foo

import arc.util.serialization.Jval
import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.ArcCommand
import com.github.kennarddh.mindustry.genesis.core.commands.events.CommandsChanged
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.packets.annotations.PacketHandler
import com.github.kennarddh.mindustry.genesis.standard.extensions.clientPacketReliable
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Player


class FooHandler : Handler() {
    private val version by lazy { Vars.mods.getMod(com.github.kennarddh.mindustry.genesis.standard.GenesisStandard::class.java).meta.version }

    val playersWithFoo: MutableList<Player> = mutableListOf()

    @EventHandler
    fun onPlayerLeave(event: EventType.PlayerLeave) {
        playersWithFoo.remove(event.player)
    }

    fun Player.isUsingFooClient() = playersWithFoo.contains(this)

    /** Plugin presence check */
    @PacketHandler(["fooCheck"])
    fun fooCheck(player: Player) {
        playersWithFoo.add(player)

        player.clientPacketReliable("fooCheck", version)

        enableTransmissions(player)
        sendCommands(player)
    }

    /** Client transmission forwarding */
    @PacketHandler(["fooTransmission"])
    fun fooTransmission(player: Player, content: String) {
        Call.clientPacketReliable("fooTransmission", "${player.id} $content")
    }

    @EventHandler
    private fun onCommandsChanged(event: CommandsChanged) {
        sendCommands()
    }

    /** Informs clients of the transmission forwarding state. When [player] is null, the status is sent to everyone */
    private fun enableTransmissions(player: Player? = null) {
        val enabled = true

        if (player != null)
            player.clientPacketReliable("fooTransmissionEnabled", enabled.toString())
        else
            Call.clientPacketReliable("fooTransmissionEnabled", enabled.toString())
    }

    /** Sends the list of commands to a player */
    private fun sendCommands(player: Player? = null) {
        with(Jval.newObject()) {
            add("prefix", GenesisAPI.commandRegistry.clientPrefix)

            add("commands", Jval.newObject().apply {
                GenesisAPI.commandRegistry.clientCommands.forEach {
                    val name = if (it is ArcCommand) it.realName else it.text
                    val usage = if (it is ArcCommand) it.usage else it.paramText

                    add(name, usage)
                }
            })

            if (player == null)
                Call.clientPacketReliable("commandList", this.toString())
            else
                player.clientPacketReliable("commandList", this.toString())
        }
    }
}
