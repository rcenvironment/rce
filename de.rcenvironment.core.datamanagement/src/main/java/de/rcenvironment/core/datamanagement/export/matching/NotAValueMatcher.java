/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.NotAValueTD;

/**
 * Matches two NotAValueTDs.
 *
 * @author Tobias Brieden
 */
public class NotAValueMatcher implements Matcher<NotAValueTD>  {

    @Override
    public MatchResult matches(NotAValueTD actual, NotAValueTD expected) {

        MatchResult result = new MatchResult();
        
        if (!actual.getCause().equals(expected.getCause())) {
            result.addFailureCause("The causes are not the same.");
        }
        
        return result;
    }

}
