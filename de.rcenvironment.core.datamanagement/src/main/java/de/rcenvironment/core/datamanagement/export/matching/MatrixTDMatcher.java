/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Matches two matrices.
 * 
 * @author Marlon Schroeter
 *
 */
public class MatrixTDMatcher implements Matcher<MatrixTD> {

    @Override
    public MatchResult matches(MatrixTD actual, MatrixTD expected) {
        MatchResult result = new MatchResult();

        int actualRow = actual.getRowDimension();
        int actualColumn = actual.getColumnDimension();
        int expectedRow = expected.getRowDimension();
        int expectedColumn = expected.getColumnDimension();

        if (actualRow != expectedRow || actualColumn != expectedColumn) {
            result.addFailureCause("The matrix dimensions are not the same.");
            return result;
        }

        for (int i = 0; i < actualRow; i++) {
            for (int j = 0; j < actualColumn; j++) {
                if (!actual.getFloatTDOfElement(i, j).equals(expected.getFloatTDOfElement(i, j))) {
                    result.addFailureCause(
                            StringUtils.format("The matrix values in row %s and column %s are not the same.", i, j));
                    return result;
                }
            }
        }

        return result;
    }

}
