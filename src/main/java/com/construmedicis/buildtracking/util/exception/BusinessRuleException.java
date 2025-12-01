package com.construmedicis.buildtracking.util.exception;

public class BusinessRuleException extends BaseException {

    /**
     * Constructs a BaseException with the specified detail key message.
     *
     * @param key the detailed key message
     */
    public BusinessRuleException(String key) {
        super(key);
    }

    /**
     * Constructs a BaseException with the specified detail message.
     *
     * @param message the detailed message
     * @param rabbit  to specific if it is a rabbit operation
     */
    public BusinessRuleException(String message, Boolean rabbit) {
        super(message, rabbit);
    }
}
