/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.rcenvironment.components.outputwriter.common.OutputWriterValidatorHelper;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;

/**
 * Tests correct replacement of placeholder by OutputWriterValidatorHelper. This class is seemingly misleadingly named since only the
 * OutputLocationWriter uses this replacement. Moreover, the replacement happens via calls to static methods, hence the replacement is
 * strongly linked to the correct operation of the OutputLocationWriter.
 * 
 * @author Alexander Weinert
 */
public class OutputLocationWriterTest {

    private static final String BOOL_VAR = "boolVar";

    private static final String FLOAT_VAR = "floatVar";

    private static final String INT_VAR = "intVar";

    private static final String TIMESTAMP = "2018";

    private final TypedDatumFactory tdFactory = new ComponentContextMock().getService(TypedDatumService.class).getFactory();

    /**
     * Test that the empty string is preserved by placeholder replacement.
     */
    @Test
    public void testEmptyFormatString() {
        final String formatString = "";

        final String expectedOutput = "";
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, new HashMap<String, TypedDatum>(), TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that the timestamp placeholder is correctly replaced.
     */
    @Test
    public void testTimestamp() {
        final String formatString = "[Timestamp]";

        final String expectedOutput = TIMESTAMP;
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, new HashMap<String, TypedDatum>(), TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that the execution count placeholder is correctly replaced.
     */
    @Test
    public void testExecutionCount() {
        final String formatString = "[Execution count]";

        final String expectedOutput = "14";
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, new HashMap<String, TypedDatum>(), TIMESTAMP, 14);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that the linebreak placeholder is correctly replaced.
     */
    @Test
    public void testLinebreak() {
        final String formatString = "a[Linebreak]b";

        final String expectedOutput = String.format("a%sb", System.getProperty("line.separator"));
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, new HashMap<String, TypedDatum>(), TIMESTAMP, 14);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that placeholders of type boolean are correctly replaced.
     */
    @Test
    public void testBool() {
        final String formatString = "leftSep [boolVar] rightSep";

        final String expectedOutput = "leftSep true rightSep";
        final Map<String, TypedDatum> values = new HashMap<>();
        values.put(BOOL_VAR, tdFactory.createBoolean(true));
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, values, TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that placeholders of type int are correctly replaced.
     */
    @Test
    public void testInt() {
        final String formatString = "leftSep[intVar]rightSep";

        final String expectedOutput = "leftSep15rightSep";
        final Map<String, TypedDatum> values = new HashMap<>();
        final int fifteen = 15;
        values.put(INT_VAR, tdFactory.createInteger(fifteen));
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, values, TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that placeholders of type float are correctly replaced.
     */
    @Test
    public void testFloat() {
        final String formatString = "leftSep[floatVar]rightSep";

        final String expectedOutput = "leftSep5.0rightSep";
        final Map<String, TypedDatum> values = new HashMap<>();
        values.put(FLOAT_VAR, tdFactory.createFloat(5.0));
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, values, TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that multiple placeholders are correctly replaced.
     */
    @Test
    public void testMultiple() {
        final String formatString = "leftSep[floatVar]rightSep[boolVar]middle[intVar]someOther";

        final String expectedOutput = "leftSep5.0rightSepfalsemiddle1someOther";
        final Map<String, TypedDatum> values = new HashMap<>();
        values.put(FLOAT_VAR, tdFactory.createFloat(5.0));
        values.put(BOOL_VAR, tdFactory.createBoolean(false));
        values.put(INT_VAR, tdFactory.createInteger(1));
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, values, TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that unknown placeholders are not replaced, but preserved in their original form.
     */
    @Test
    public void testUnknown() {
        final String formatString = "leftSep[floatVar]rightSep[boolVar]middle[intVar]someOther";

        final String expectedOutput = "leftSep5.0rightSep[boolVar]middle1someOther";
        final Map<String, TypedDatum> values = new HashMap<>();
        values.put(FLOAT_VAR, tdFactory.createFloat(5.0));
        values.put(INT_VAR, tdFactory.createInteger(1));
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, values, TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }

    /**
     * Test that brackets and backslashes are correctly escaped.
     */
    @Test
    public void testEscape() {
        final String formatString = "leftSep[we\\[irdNa\\]me]rightSep[in\\\\clud\\[ing]middle[spe\\[ci\\]\\]alCha\\\\rs]someOther";

        final String expectedOutput = "leftSep12.8rightSepfalsemiddle18someOther";
        final Map<String, TypedDatum> values = new HashMap<>();
        final double twelvePointEight = 12.8;
        final int eighteen = 18;
        values.put("we[irdNa]me", tdFactory.createFloat(twelvePointEight));
        values.put("in\\clud[ing", tdFactory.createBoolean(false));
        values.put("spe[ci]]alCha\\rs", tdFactory.createInteger(eighteen));
        final String actualOutput =
            OutputWriterValidatorHelper.replacePlaceholders(formatString, values, TIMESTAMP, 0);

        assertEquals(expectedOutput, actualOutput);
    }
}
