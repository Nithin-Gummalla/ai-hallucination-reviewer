package com.reviewer.exception;

public class OversizedRepoException extends RuntimeException {
    public OversizedRepoException(String message) {
        super(message);
    }
}
