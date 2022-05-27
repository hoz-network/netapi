package net.hoz.netapi.api

import reactor.core.Disposable

/**
 * Something that can be controlledServices, ie enabled/disabled.
 */
interface Controlled : Disposable {
    /**
     * Method that is called on the application start.
     */
    suspend fun initialize()
}