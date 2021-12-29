package net.hoz.api;

import reactor.core.Disposable;

/**
 * Something that can be controlledServices, ie enabled/disabled.
 */
public interface Controlled extends Disposable {

    /**
     * Method that is called on the application start.
     */
    void initialize();
}
