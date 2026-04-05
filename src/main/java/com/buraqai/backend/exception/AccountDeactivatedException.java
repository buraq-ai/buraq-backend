package com.buraqai.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountDeactivatedException extends RuntimeException {

    public AccountDeactivatedException() {
        super("Your account has been deactivated. Please contact your system administrator.");
    }
}