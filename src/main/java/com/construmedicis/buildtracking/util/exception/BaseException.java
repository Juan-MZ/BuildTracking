package com.construmedicis.buildtracking.util.exception;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ResourceBundle;

public class BaseException extends RuntimeException {

    // ~ Constants
    // ====================================================================

    /**
     * The exceptions resource bundle.
     */
    private static final ResourceBundle messages = ResourceBundle.getBundle("exceptions",
            LocaleContextHolder.getLocale());

    // ~ Constructors
    // ====================================================================

    /**
     * Constructs a BaseException with the specified detail key message.
     *
     * @param key the detailed key message
     */
    BaseException(String key) {
        super(getMessage(key));
    }

    /**
     * Constructs a BaseException with the specified detail message.
     *
     * @param message the detailed message
     * @param rabbit  to specific if it is a rabbit operation
     */
    BaseException(String message, Boolean rabbit) {
        super(message);
    }

    // ~ Methods
    // ====================================================================

    /**
     * Gets a string for the given key from the exceptions resource bundle.
     *
     * @param key the key for the desired string
     * @return the string value for the given key
     */
    private static String getMessage(String key) {
        return messages.getString(key);
    }
}