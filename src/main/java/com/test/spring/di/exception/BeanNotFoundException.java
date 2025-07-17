package com.test.spring.di.exception;

/**
 * Exception thrown when a bean is not found in the container.
 */
public class BeanNotFoundException extends RuntimeException {
    public BeanNotFoundException(String message) {
        super(message);
    }

    public BeanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
