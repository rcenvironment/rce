/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import java.util.Objects;
import java.util.Optional;

/**
 * A container class that encapsulates some value that may not be present. This behavior is very similar to an {@link Optional}. While an
 * Optional, however, does not yield any information about the cause of the absence of the object, a ParseResult is either present or it
 * carries some human-readable error message that explains the cause of the absence of the value to the caller.
 * 
 * @author Alexander Weinert
 *
 * @param <T> The type of the expected result of the parsing
 */
final class ParseResult<T> {
    
    private Optional<T> result;
    
    private String errorMessages;
    
    // We make the constructor private in order to enforce use of the builder methods
    private ParseResult() { }

    public static <T> ParseResult<T> createSuccessfulResult(T value) {
        Objects.requireNonNull(value);

        final ParseResult<T> product = new ParseResult<T>();
        product.result = Optional.of(value);
        return product;
    }
    
    public static <T> ParseResult<T> createErrorResult(String displayMessage) {
        Objects.requireNonNull(displayMessage);
        
        final ParseResult<T> product = new ParseResult<T>();
        product.result = Optional.empty();
        product.errorMessages = displayMessage;
        return product;
    }
    
    public boolean isSuccessfulResult() {
        return this.result.isPresent();
    }
    
    public boolean isErrorResult() {
        return !this.result.isPresent();
    }
    
    public T getResult() {
        return this.result.get();
    }
    
    public String getErrorDisplayMessage() {
        return this.errorMessages;
    }
}
