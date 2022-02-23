/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.DateTimeTD;

/**
 * Matches two short texts.
 * 
 * @author Marlon Schroeter
 *
 */
public class DateTimeTDMatcher implements Matcher<DateTimeTD> {

    @Override
    public MatchResult matches(DateTimeTD actual, DateTimeTD expected) {
        MatchResult result = new MatchResult();

        if (actual.getDateTimeInMilliseconds() != expected.getDateTimeInMilliseconds()) {
            result.addFailureCause("The DateTime values are not the same.");
        }

        return result;
    }

}
