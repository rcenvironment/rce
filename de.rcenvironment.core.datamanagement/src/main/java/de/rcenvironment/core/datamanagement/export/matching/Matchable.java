/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import java.util.Map;

import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Generic interface that should be implemented by classes that can be matched.
 *
 * @author Tobias Brieden
 * @param <T> the type of objects that this object may be matched against
 */
public interface Matchable<T> {

    /**
     * Matches two objects agains each other.
     * 
     * TODO currently the matchers map contains only matchers used for the TypedDatums, however this could be made more generic to make the
     * whole matching process more configurable, e.g. to allow the specification of a matcher used to match two component runs.
     * 
     * @param matchers For each data type this map should contain a corresponding Matcher which will be used to perform the match.
     * @param expected Another object for the comparison.
     * @return Returns a {@link MatchResult} object containing the information if the objects did match or failure causes if they do not
     *         match.
     */
    MatchResult matches(Map<DataType, Matcher> matchers, T expected);
}
