package com.buraqai.backend.exception;

import com.buraqai.backend.model.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    private final TicketStatus fromStatus;
    private final TicketStatus toStatus;

    public InvalidStatusTransitionException(TicketStatus fromStatus, TicketStatus toStatus) {
        super(String.format("Invalid status transition from %s to %s", fromStatus, toStatus));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    public TicketStatus getToStatus() {
        return toStatus;
    }
}