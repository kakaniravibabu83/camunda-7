package com.example.camunda.exception;

public class InvalidStageTransitionException extends RuntimeException {

    public InvalidStageTransitionException(String message) {
        super(message);
    }
}
