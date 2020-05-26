/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.FloatTD;

/**
 * Matches two FloatTD with a configurable allowed difference.
 *
 * @author Tobias Brieden
 */
public class FloatTDMatcher implements Matcher<FloatTD> {

    private static final double EPSILON = 10e-7;

    private final double epsilon;

    public FloatTDMatcher() {
        this(EPSILON);
    }

    public FloatTDMatcher(double epsilon) {
        this.epsilon = epsilon;
    }

    @Override
    public MatchResult matches(FloatTD actual, FloatTD expected) {
        
        MatchResult result = new MatchResult();

        double absDiff = Math.abs(actual.getFloatValue() - expected.getFloatValue());

        if (absDiff > epsilon) {
            result.addFailureCause("The float value differs more than the allowed epsilon.");
        }

        return result;
    }

}
