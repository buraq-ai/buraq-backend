package com.buraqai.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CannotDeactivateSelfException extends RuntimeException {

    public CannotDeactivateSelfException() {
        super("You cannot deactivate your own account");
    }
}