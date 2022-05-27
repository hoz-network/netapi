package net.hoz.netapi.api

import reactor.core.Disposable

data class ControlledService(val controlledServices: List<Controlled>) {
    suspend fun enable() = controlledServices.forEach { it.initialize() }

    fun disable() = controlledServices.forEach(Disposable::dispose)
}


