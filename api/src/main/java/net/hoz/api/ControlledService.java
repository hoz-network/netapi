package net.hoz.api;

import reactor.core.Disposable;

import java.util.List;

public record ControlledService(List<Controlled> controlledServices) {

    public void enable() {
        controlledServices.forEach(Controlled::initialize);
    }

    public void disable() {
        controlledServices.forEach(Disposable::dispose);
    }
}
