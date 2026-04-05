package com.buraqai.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserAlreadyInactiveException extends RuntimeException {

    public UserAlreadyInactiveException(Long id) {
        super("User with ID " + id + " is already inactive");
    }
}