/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.component.api;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Base class for string-based identifiers. The contained string id can not be null; if a null id is semantically required, the wrapper
 * object should be null instead.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 */
@SuppressWarnings("serial") // serialization ids should be defined for each subclass
public abstract class StringIdentifier implements Serializable {

    // ids are frequently created, so use a static logger
    protected static final Log sharedLogger = LogFactory.getLog(StringIdentifier.class);

    // semantically final, but cannot be marked as such as it would be ignored in serialization
    private String identifier;

    /**
     * Deserialization constructor - do not use this manually.
     */
    protected StringIdentifier() {
        this.identifier = null;
    }

    public StringIdentifier(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Provided string ids can not be null");
        }
        this.identifier = identifier;
    }

    @Override
    public final boolean equals(Object other) {
        if (other == null) {
            logWarning("Comparing to a null object; returning false");
            return false;
        }
        if (other.getClass() != this.getClass()) {
            throw new IllegalArgumentException("Attempted to compare a " + getClass().getSimpleName() + " to an instance of "
                + other.getClass().getSimpleName());
        }

        final String otherIdentifier = ((StringIdentifier) other).identifier;

        if (identifier == null) {
            // should never happen
            logWarning("equals() called while containing a null string id");
            return otherIdentifier == null;
        }

        if (otherIdentifier == null) {
            // should never happen either
            logWarning("equals() called with a same-class parameter containing a null string id");
            return false;
        }

        return identifier.equals(otherIdentifier);
    }

    @Override
    public final int hashCode() {
        if (identifier != null) {
            return identifier.hashCode();
        } else {
            // should never happen
            logWarning("hashCode() called while containing a null string id");
            return getClass().hashCode();
        }
    }

    @Override
    public String toString() {
        return identifier;
    }

    private void logWarning(String text) {
        sharedLogger.warn(StringUtils.format("[%s: %s] %s", getClass().getSimpleName(), this.identifier, text));
    }
}
