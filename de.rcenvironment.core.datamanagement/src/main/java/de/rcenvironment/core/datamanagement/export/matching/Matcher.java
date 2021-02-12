/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.export.matching;

/**
 * Generic interface which should be implemented by objects which can match two instances of the same type against each other.
 *
 * @author Tobias Brieden 
 * 
 * @param <T> the type of objects that this Matcher can match against each other.
 */
public interface Matcher<T> {

    /**
     * @param actual The actual object.
     * @param expected The expected object.
     * @return The result of the match.
     */
    MatchResult matches(T actual, T expected);
}
