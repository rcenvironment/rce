/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.evaluationmemory.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;

/**
 * Test cases for {@link ToleranceHandling}.
 *
 * @author Alexander Weinert
 */
public class ToleranceHandlingTest {

    private static final Double ZEROPERCENT = new Double(0.0);
    private static final Double TENPERCENT = new Double(0.1);

    private static final FloatTD FLOAT13 = createFloatDatumMock(1.3);

    private static final FloatTD FLOAT14 = createFloatDatumMock(1.4);

    private static final FloatTD FLOAT15 = createFloatDatumMock(1.5);

    private static final FloatTD FLOAT16 = createFloatDatumMock(1.6);

    private static final FloatTD FLOAT17 = createFloatDatumMock(1.7);

    private static final IntegerTD INT13 = createIntDatumMock(13);

    private static final IntegerTD INT14 = createIntDatumMock(14);

    private static final IntegerTD INT15 = createIntDatumMock(15);

    private static final IntegerTD INT16 = createIntDatumMock(16);

    private static final IntegerTD INT17 = createIntDatumMock(17);

    private static final String X1 = "X1";

    private static final String X2 = "X2";

    private static IntegerTD createIntDatumMock(long value) {
        final IntegerTD retVal = EasyMock.createNiceMock(IntegerTD.class);

        EasyMock.expect(retVal.getDataType()).andStubReturn(DataType.Integer);
        EasyMock.expect(retVal.getIntValue()).andStubReturn(value);
        EasyMock.replay(retVal);

        return retVal;
    }

    private static FloatTD createFloatDatumMock(double value) {
        final FloatTD retVal = EasyMock.createNiceMock(FloatTD.class);

        EasyMock.expect(retVal.getDataType()).andStubReturn(DataType.Float);
        EasyMock.expect(retVal.getFloatValue()).andStubReturn(value);
        EasyMock.replay(retVal);

        return retVal;
    }

    /**
     * Tests that strict tolerance handling works as expected.
     */
    @Test
    public void testToleranceIntervalsFloat() {
        final ComponentLog log = EasyMock.createStrictMock(ComponentLog.class);
        EasyMock.replay(log);
        // It does not matter here whether we construct strict or lenient handling, as both use the same algorithm for testing the tolerance
        // intervals
        final ToleranceHandling toleranceHandling = ToleranceHandling.constructStrictHandling(log);
        
        final SortedMap<String, TypedDatum> inputs = new TreeMap<>();
        final SortedMap<String, Double> tolerances = new TreeMap<>();
        final SortedMap<String, TypedDatum> storedValues = new TreeMap<>();

        // Test that a value is in its own tolerance interval even if no tolerance is given.
        inputs.put(X1, FLOAT15);
        tolerances.put(X1, null);
        storedValues.put(X1, FLOAT15);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that a value is in its own tolerance interval if zero tolerance is given.
        inputs.put(X1, FLOAT15);
        tolerances.put(X1, ZEROPERCENT);
        storedValues.put(X1, FLOAT15);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that lower bounds work
        tolerances.put(X1, TENPERCENT);
        storedValues.put(X1, FLOAT15);

        inputs.put(X1, FLOAT13);
        assertFalse(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));
        inputs.put(X1, FLOAT14);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that upper bounds work
        tolerances.put(X1, TENPERCENT);
        storedValues.put(X1, FLOAT15);

        inputs.put(X1, FLOAT17);
        assertFalse(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));
        inputs.put(X1, FLOAT16);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that a vector is outside the tolerance interval if one of its values is outside the tolerance interval
        storedValues.put(X2, FLOAT15);
        tolerances.put(X2, TENPERCENT);
        inputs.put(X1, FLOAT14);
        inputs.put(X2, FLOAT17);
        assertFalse(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that a vector is inside the tolerance interval if all its values are inside the tolerance intervals
        inputs.put(X1, FLOAT14);
        inputs.put(X2, FLOAT16);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));
    }

    /**
     * Tests that lenient tolerance handling works as expected.
     */
    @Test
    public void testToleranceIntervalsInt() {
        final ComponentLog log = EasyMock.createStrictMock(ComponentLog.class);
        EasyMock.replay(log);
        // It does not matter here whether we construct strict or lenient handling, as both use the same algorithm for testing the tolerance
        // intervals
        final ToleranceHandling toleranceHandling = ToleranceHandling.constructStrictHandling(log);

        final SortedMap<String, TypedDatum> inputs = new TreeMap<>();
        final SortedMap<String, Double> tolerances = new TreeMap<>();
        final SortedMap<String, TypedDatum> storedValues = new TreeMap<>();

        // Test that a value is in its own tolerance interval even if no tolerance is given.
        inputs.put(X1, INT15);
        tolerances.put(X1, null);
        storedValues.put(X1, INT15);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that a value is in its own tolerance interval if zero tolerance is given.
        inputs.put(X1, INT15);
        tolerances.put(X1, ZEROPERCENT);
        storedValues.put(X1, INT15);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that lower bounds work
        tolerances.put(X1, TENPERCENT);
        storedValues.put(X1, INT15);

        inputs.put(X1, INT13);
        assertFalse(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));
        inputs.put(X1, INT14);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that upper bounds work
        tolerances.put(X1, TENPERCENT);
        storedValues.put(X1, INT15);

        inputs.put(X1, INT17);
        assertFalse(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));
        inputs.put(X1, INT16);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that a vector is outside the tolerance interval if one of its values is outside the tolerance interval
        storedValues.put(X2, INT15);
        tolerances.put(X2, TENPERCENT);
        inputs.put(X1, INT14);
        inputs.put(X2, INT17);
        assertFalse(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));

        // Test that a vector is inside the tolerance interval if all its values are inside the tolerance intervals
        inputs.put(X1, INT14);
        inputs.put(X2, INT16);
        assertTrue(toleranceHandling.isInToleranceInterval(inputs, tolerances, storedValues));
    }

    /**
     * Tests that strict tolerance handling only returns a value from a singleton set of candidates and otherwise forces re-evaluation.
     */
    @Test
    public void testStrictToleranceHandling() {
        final ComponentLog log = EasyMock.createStrictMock(ComponentLog.class);
        EasyMock.replay(log);
        final ToleranceHandling toleranceHandling = ToleranceHandling.constructStrictHandling(log);

        final SortedMap<String, TypedDatum> actualInput = new TreeMap<>();
        actualInput.put(X1, FLOAT15);
        final Collection<SortedMap<String, TypedDatum>> storedInputs = new HashSet<>();

        // Test that strict tolerance handling does not return a value from an empty set of candidates
        SortedMap<String, TypedDatum> result = toleranceHandling.pickMostToleratedInputs(storedInputs, actualInput);
        assertNull(result);

        // Test that strict tolerance handling returns the unique value from a singleton set of candidates
        final SortedMap<String, TypedDatum> storedInput1 = new TreeMap<>();
        storedInput1.put(X1, FLOAT14);
        storedInputs.add(storedInput1);

        EasyMock.reset(log);
        log.componentInfo(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall();
        EasyMock.replay(log);
        result = toleranceHandling.pickMostToleratedInputs(storedInputs, actualInput);
        assertEquals(storedInput1, result);

        // Test that strict tolerance handling returns the unique value from a singleton set of candidates
        final SortedMap<String, TypedDatum> storedInput2 = new TreeMap<>();
        storedInput2.put(X1, FLOAT16);
        storedInputs.add(storedInput2);

        EasyMock.reset(log);
        EasyMock.replay(log);
        result = toleranceHandling.pickMostToleratedInputs(storedInputs, actualInput);
        assertNull(result);
    }

    /**
     * Tests that lenient tolerance handling returns a value as soon as the set of candidates is nonempty.
     */
    @Test
    public void testLenientToleranceHandling() {
        final ComponentLog log = EasyMock.createStrictMock(ComponentLog.class);
        EasyMock.replay(log);
        final ToleranceHandling toleranceHandling = ToleranceHandling.constructLenientHandling(log);

        final SortedMap<String, TypedDatum> actualInput = new TreeMap<>();
        actualInput.put(X1, FLOAT15);
        final Collection<SortedMap<String, TypedDatum>> storedInputs = new HashSet<>();

        // Test that lenient tolerance handling does not return a value from an empty set of candidates
        SortedMap<String, TypedDatum> result = toleranceHandling.pickMostToleratedInputs(storedInputs, actualInput);
        assertNull(result);

        // Test that lenient tolerance handling returns the unique value from a singleton set of candidates
        final SortedMap<String, TypedDatum> storedInput1 = new TreeMap<>();
        storedInput1.put(X1, FLOAT14);
        storedInputs.add(storedInput1);

        EasyMock.reset(log);
        log.componentInfo(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall();
        EasyMock.replay(log);
        result = toleranceHandling.pickMostToleratedInputs(storedInputs, actualInput);
        assertEquals(storedInput1, result);

        // Test that lenient tolerance handling returns any value from a nonempty set of candidates
        final SortedMap<String, TypedDatum> storedInput2 = new TreeMap<>();
        storedInput2.put(X1, FLOAT16);
        storedInputs.add(storedInput2);

        EasyMock.reset(log);
        result = toleranceHandling.pickMostToleratedInputs(storedInputs, actualInput);
        assertNotNull(result);
        assertTrue(storedInputs.contains(result));
    }
}
