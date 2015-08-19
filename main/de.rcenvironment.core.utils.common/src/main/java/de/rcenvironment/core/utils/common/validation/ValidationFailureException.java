/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.validation;

/**
 * An Exception indicating a validation failure, usually during initialization of a component. Wraps
 * a {@link ValidationResult}; if no instance is provided, it is created internally.
 * 
 * @author Robert Mischke
 * 
 */
public class ValidationFailureException extends Exception {

    private static final long serialVersionUID = -6819580978134634141L;

    private final ValidationResult result;

    public ValidationFailureException() {
        result = new ValidationResult();
    }

    public ValidationFailureException(String singleMessage) {
        result = new ValidationResult(singleMessage);
    }

    public ValidationFailureException(ValidationResult resultObject) {
        this.result = resultObject;
    }

    /**
     * Adds a validation feedback message; see {@link ValidationResult#addMessage(String)}.
     * 
     * @param message the message text to add
     */
    public void addMessage(String message) {
        result.addMessage(message);
    }

    /**
     * @return the internal object containing containing the stored messages
     * 
     * TODO return a thread-safe copy instead?
     */
    public ValidationResult getResultObject() {
        return result;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation Failure: ");
        boolean first = true;
        for (String msg : result.getMessages()) {
            if (first) {
                first = false;
            } else {
                sb.append(" / ");
            }
            sb.append(msg);
        }
        return sb.toString();
    }
}
