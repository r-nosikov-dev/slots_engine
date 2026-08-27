package com.slotengine.model;

import java.util.List;

public class GameValidationException extends RuntimeException {

    private final List<String> errors;

    public GameValidationException(List<String> errors) {
        super("Invalid game definition: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
