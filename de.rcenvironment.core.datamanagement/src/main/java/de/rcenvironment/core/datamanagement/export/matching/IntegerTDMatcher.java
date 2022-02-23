/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.IntegerTD;

/**
 * Matches two IntegerTDs.
 *
 * @author Tobias Brieden
 */
public class IntegerTDMatcher implements Matcher<IntegerTD> {

    @Override
    public MatchResult matches(IntegerTD actual, IntegerTD expected) {

        MatchResult result = new MatchResult();

        if (actual.getIntValue() != expected.getIntValue()) {
            result.addFailureCause("The values are not the same.");
        }

        return result;
    }

}
