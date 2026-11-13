package com.shakil.pulsequeue.exception;

public class InvalidQueueOperationException extends RuntimeException {
    public InvalidQueueOperationException(String message) {
        super(message);
    }
}
