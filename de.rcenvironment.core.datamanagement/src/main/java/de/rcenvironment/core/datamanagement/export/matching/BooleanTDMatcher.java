/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.BooleanTD;

/**
 * Matches two BooleanTDs.
 *
 * @author Tobias Brieden
 */
public class BooleanTDMatcher implements Matcher<BooleanTD> {

    public BooleanTDMatcher() {
    }

    @Override
    public MatchResult matches(BooleanTD actual, BooleanTD expected) {
        MatchResult result = new MatchResult();
        
        if (actual.getBooleanValue() != expected.getBooleanValue()) {
            result.addFailureCause("The boolean values are not the same.");
        }
        
        return result;
    }

}
