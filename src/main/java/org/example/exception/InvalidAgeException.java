package org.example.exception;

public class InvalidAgeException extends ValidationException {
    public InvalidAgeException(String message) {
        super(message);
    }
}