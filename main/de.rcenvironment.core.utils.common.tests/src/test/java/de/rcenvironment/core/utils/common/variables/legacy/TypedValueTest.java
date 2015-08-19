/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.variables.legacy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * Tests the super class of BoundVariable.
 * 
 * @author Arne Bachmann
 */
public class TypedValueTest {

    /**
     * Test.
     */
    @Test
    public void testRepresentation() {
        @SuppressWarnings("serial") final Map<VariableType, String> expectedRepresentations =
            new Hashtable<VariableType, String>() {

                {
                    put(VariableType.Integer, "23423423423423432432348567");
                    put(VariableType.Real, "1289734609.344367");
                    put(VariableType.Logic, "true");
                }
            };
        for (final Entry<VariableType, String> entry : expectedRepresentations.entrySet()) {
            final TypedValue var = new TypedValue(entry.getKey(), entry.getValue());
            assertThat(var.getStringValue(), is(entry.getValue()));
        }
    }

    /**
     * More code to check.
     */
    @Test
    public void testMissingCoverage() {
        final long x23434 = 23434L;
        final int x34 = 34;
        final long x45 = 45;
        assertThat(new TypedValue("c".getClass(), "0").getType(), is(VariableType.String));
        assertThat(new TypedValue(false).setLogicValue(true).getLogicValue(), is(true));
        assertThat(new TypedValue((Serializable) null).getValue(), is((Serializable) null));
        assertThat(new TypedValue((Serializable) null).getLogicValue(), is(false));
        assertThat(new TypedValue((Serializable) null).getType(), is(VariableType.Empty));
        assertThat(new TypedValue(Long.class, "23434").getType(), is(VariableType.Integer));
        assertThat(new TypedValue(Long.class, "23434").getIntegerValue(), is(x23434));
        assertThat(new TypedValue(VariableType.String).setValue("abc").getType(), is(VariableType.String));
        assertThat(new TypedValue(x34).setValueFromString("45").getIntegerValue(), is(x45));
    }

    /**
     * Y.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongSetType1() {
        assertThat(new TypedValue(VariableType.String).setValue(2.0D).getType(), is(VariableType.Real));
    }

    /**
     * X.
     */
    @Test(expected = NullPointerException.class)
    public void testWrongSetType2() {
        assertThat(new TypedValue(VariableType.Integer).setEmptyValue().getIntegerValue(), is(0L)); // throws NPE
    }

}
