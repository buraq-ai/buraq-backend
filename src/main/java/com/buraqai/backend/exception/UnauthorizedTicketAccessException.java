package com.buraqai.backend.exception;

public class UnauthorizedTicketAccessException extends RuntimeException {

    public UnauthorizedTicketAccessException(String message) {
        super(message);
    }
}