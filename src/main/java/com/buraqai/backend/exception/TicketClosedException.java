package com.buraqai.backend.exception;

public class TicketClosedException extends RuntimeException {

    public TicketClosedException(String message) {
        super(message);
    }
}