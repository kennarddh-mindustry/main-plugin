package com.github.kennarddh.mindustry.genesis.core.events

import arc.Events
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandlerTrigger
import com.github.kennarddh.mindustry.genesis.core.events.exceptions.InvalidEventHandlerMethodException
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible

class EventRegistry {
    internal fun init() {
    }

    fun registerHandler(handler: Handler) {
        for (function in handler::class.declaredFunctions) {
            function.isAccessible = true

            if (!function.hasAnnotation<EventHandler>()) continue

            val eventHandlerTriggerAnnotation = function.findAnnotation<EventHandlerTrigger>()

            val functionParameters = function.parameters.drop(1)

            if (functionParameters.isEmpty() && eventHandlerTriggerAnnotation != null)
                throw InvalidEventHandlerMethodException("Method ${handler::class.qualifiedName}.${function.name} cannot accept parameter with EventHandlerTriggerAnnotation")
            else if (functionParameters.size != 1)
                throw InvalidEventHandlerMethodException("Method ${handler::class.qualifiedName}.${function.name} must accept exactly one parameter with the event type or use EventHandlerTrigger annotation")

            if (eventHandlerTriggerAnnotation != null) {
                Events.run(eventHandlerTriggerAnnotation.trigger) {
                    CoroutineScopes.Main.launch {
                        function.callSuspend(handler)
                    }
                }
            } else {
                Events.on((functionParameters[0].type.classifier as KClass<*>).java) {
                    CoroutineScopes.Main.launch {
                        function.callSuspend(handler, it)
                    }
                }
            }
        }
    }
}