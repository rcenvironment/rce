/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Matches two vectors.
 * 
 * @author Marlon Schroeter
 *
 */
public class VectorTDMatcher implements Matcher<VectorTD> {

    @Override
    public MatchResult matches(VectorTD actual, VectorTD expected) {
        MatchResult result = new MatchResult();

        int actualRow = actual.getRowDimension();
        int expectedRow = expected.getRowDimension();

        if (actualRow != expectedRow) {
            result.addFailureCause("The vector dimensions are not the same.");
            return result;
        }

        for (int i = 0; i < actualRow; i++) {
            if (!actual.getFloatTDOfElement(i).equals(expected.getFloatTDOfElement(i))) {
                result.addFailureCause(StringUtils.format("The vector values in row %s are not the same.", i));
                return result;
            }
        }

        return result;
    }

}
