/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.ShortTextTD;

/**
 * Matches two short texts.
 * 
 * @author Marlon Schroeter
 *
 */
public class ShortTextTDMatcher implements Matcher<ShortTextTD> {

    @Override
    public MatchResult matches(ShortTextTD actual, ShortTextTD expected) {
        MatchResult result = new MatchResult();

        if (!actual.getShortTextValue().equals(expected.getShortTextValue())) {
            result.addFailureCause("The short text values are not the same.");
        }

        return result;
    }

}
