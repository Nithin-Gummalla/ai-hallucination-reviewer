package com.reviewer.exception;

public class InvalidRepoUrlException extends RuntimeException {
    public InvalidRepoUrlException(String message) {
        super(message);
    }
}
