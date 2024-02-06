package com.github.kennarddh.mindustry.genesis.core.events

import arc.Events
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.events.exceptions.InvalidEventHandlerMethodException
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible

class EventRegistry {
    internal fun init() {
    }

    fun registerHandler(handler: Handler) {
        for (function in handler::class.declaredFunctions) {
            function.isAccessible = true

            if (!function.hasAnnotation<EventHandler>()) continue

            val functionParameters = function.parameters.drop(1)

            if (functionParameters.size != 1)
                throw InvalidEventHandlerMethodException("Method ${handler::class.qualifiedName}.${function.name} must accept exactly one parameter with the event type")

            val eventType = functionParameters[0].type.classifier as KClass<*>

            Events.on(eventType.java) {
                CoroutineScopes.Main.launch {
                    function.callSuspend(handler, it)
                }
            }
        }
    }
}