package com.costroom.aitoolsservice.exception;

import java.util.UUID;

public class ToolNotFoundException extends RuntimeException {
    public ToolNotFoundException(UUID id) {
        super("AI tool not found: " + id);
    }
}
