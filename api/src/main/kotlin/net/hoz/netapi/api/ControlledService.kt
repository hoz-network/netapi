package net.hoz.netapi.api

import reactor.core.Disposable

data class ControlledService(val controlledServices: List<Controlled>) {
    fun enable() = controlledServices.forEach(Controlled::initialize)

    fun disable() = controlledServices.forEach(Disposable::dispose)
}


