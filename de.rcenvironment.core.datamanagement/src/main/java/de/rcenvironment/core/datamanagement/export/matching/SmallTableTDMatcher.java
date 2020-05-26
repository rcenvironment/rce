/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.matching;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Matches two small matrices.
 * 
 * @author Marlon Schroeter
 *
 */
public class SmallTableTDMatcher implements Matcher<SmallTableTD> {

    private Map<DataType, Matcher> matchers = new HashMap<>();

    @Override
    public MatchResult matches(SmallTableTD actual, SmallTableTD expected) {
        MatchResult result = new MatchResult();

        int actualRow = actual.getRowCount();
        int actualColumn = actual.getColumnCount();
        int expectedRow = expected.getRowCount();
        int expectedColumn = expected.getColumnCount();

        if (actualRow != expectedRow || actualColumn != expectedColumn) {
            result.addFailureCause("The dimensions of the small tables are not the same.");
            return result;
        }
        
        initMatchers();

        for (int i = 0; i < actualRow; i++) {
            for (int j = 0; j < actualColumn; j++) {
                TypedDatum actualTD = actual.getTypedDatumOfCell(i, j);
                TypedDatum expectedTD = expected.getTypedDatumOfCell(i, j);

                if (!actualTD.getDataType().getClass().equals(expectedTD.getDataType().getClass())) {
                    result.addFailureCause(
                            StringUtils.format("The types in row %s and column %s are not the same.", i, j));
                    return result;
                }

                Matcher<TypedDatum> matcher = matchers.get(actualTD.getDataType());
                if (matcher == null) {
                    throw new IllegalArgumentException(
                            StringUtils.format("No matcher found for type %s", actualTD.getDataType().toString()));
                }
                MatchResult nestedMatchResult = matcher.matches(actualTD, expectedTD);

                if (!nestedMatchResult.hasMatched()) {
                    result.addFailureCause(
                            StringUtils.format("The values of type %s in row %s and column %s are not the same.",
                                    actualTD.getDataType().toString(), i, j));
                }
            }
        }

        return result;
    }
    
    private void initMatchers() {
        if (matchers.isEmpty()) {
            matchers.put(DataType.Boolean, new BooleanTDMatcher());
            matchers.put(DataType.Integer, new IntegerTDMatcher());
            matchers.put(DataType.Float, new FloatTDMatcher());
            matchers.put(DataType.NotAValue, new NotAValueMatcher());
            matchers.put(DataType.ShortText, new ShortTextTDMatcher());
            matchers.put(DataType.DateTime, new DateTimeTDMatcher());
            matchers.put(DataType.DirectoryReference, new DirectoryReferenceTDMatcher());
            matchers.put(DataType.FileReference, new FileReferenceTDMatcher());
            matchers.put(DataType.Matrix, new MatrixTDMatcher());
            matchers.put(DataType.Vector, new VectorTDMatcher());
            matchers.put(DataType.SmallTable, new SmallTableTDMatcher());
        }
    }
}
