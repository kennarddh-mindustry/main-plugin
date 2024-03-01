package com.github.kennarddh.mindustry.genesis.core

import arc.ApplicationListener
import arc.Core
import com.github.kennarddh.mindustry.genesis.core.commands.CommandRegistry
import com.github.kennarddh.mindustry.genesis.core.commons.AbstractPlugin
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.EventRegistry
import com.github.kennarddh.mindustry.genesis.core.extensions.getEnabledAbstractPluginsOrdered
import com.github.kennarddh.mindustry.genesis.core.filters.FiltersRegistry
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.logging.Logger
import com.github.kennarddh.mindustry.genesis.core.packets.PacketRegistry
import com.github.kennarddh.mindustry.genesis.core.server.packets.ServerPacketsRegistry
import com.github.kennarddh.mindustry.genesis.core.timers.TimersRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mindustry.Vars

class Genesis : AbstractPlugin() {
    private val backingHandlers: MutableList<Handler> = mutableListOf()

    private val registerHandlerMutex = Mutex()

    val handlers: List<Handler>
        get() = backingHandlers.toList()

    internal val commandRegistry = CommandRegistry()
    internal val eventRegistry = EventRegistry()
    internal val packetRegistry = PacketRegistry()
    internal val serverPacketsRegistry = ServerPacketsRegistry()
    internal val filtersRegistry = FiltersRegistry()
    internal val timersRegistry = TimersRegistry()

    override suspend fun onInit() {
        Logger.info("Init")

        commandRegistry.init()
        eventRegistry.init()
        packetRegistry.init()
        serverPacketsRegistry.init()
        filtersRegistry.init()
        timersRegistry.init()

        Logger.info("Registries initialized")

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                runBlocking {
                    Logger.info("Gracefully shutting down")

                    handlers.forEach {
                        launch {
                            it.onDispose()
                        }
                    }

                    Vars.mods.getEnabledAbstractPluginsOrdered().reversed().forEach {
                        launch {
                            it.onDispose()
                        }
                    }
                }

                Logger.info("Stopped")
            }
        })

        Logger.info("Dispose listener added")

        Vars.mods.getEnabledAbstractPluginsOrdered().forEach {
            withContext(CoroutineScopes.Main.coroutineContext) {
                launch {
                    it.onAsyncInit()
                }
            }
        }

        Logger.info("onAsyncInit for AbstractPlugin invoked")

        Vars.mods.getEnabledAbstractPluginsOrdered().reversed().forEach {
            it.onGenesisInit()
        }

        Logger.info("onGenesisInit for AbstractPlugin invoked")

        Logger.info("Loaded")
    }

    internal suspend fun registerHandler(handler: Handler) {
        registerHandlerMutex.withLock {
            Logger.info("Registering handler: ${handler::class.simpleName}, ${handler::class.qualifiedName}")

            backingHandlers.add(handler)

            handler.onInit()

            commandRegistry.registerHandler(handler)
            eventRegistry.registerHandler(handler)
            packetRegistry.registerHandler(handler)
            serverPacketsRegistry.registerHandler(handler)
            filtersRegistry.registerHandler(handler)
            timersRegistry.registerHandler(handler)

            handler.onRegistered()

            Logger.info("Registered handler:  ${handler::class.simpleName}, ${handler::class.qualifiedName}")
        }
    }
}