/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common.variables.legacy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;


/**
 * Test cases for {@link BoundVariable}.
 *
 * @author Arne Bachmann
 */
@SuppressWarnings("deprecation") // Keep test for deprecated class BoundVariable
public class BoundVariableTest {
    
    private static final String NAME = "name";

    /**
     * Test.
     */
    @Test
    public void testConstructor() {
        assertThat(new BoundVariable(NAME, VariableType.String).getName(), is(NAME));
        assertThat(new BoundVariable(NAME, VariableType.Logic).getType(), is(VariableType.Logic));
        for (final VariableType type: VariableType.values()) {
            assertThat(new BoundVariable(NAME, type).getType(), is(type));
        }
        final BoundVariable from = new BoundVariable("test", VariableType.Real, "34.45");
        assertThat(new BoundVariable(from).getName(), is("test"));
        assertThat(new BoundVariable(from).getType(), is(VariableType.Real));
        final double realValue = 34.45;
        assertThat(new BoundVariable(from).getRealValue(), is(realValue));
    }
    
    /**
     * Arg.
     */
    @Test
    public void testSetters() {
        assertThat(new BoundVariable(NAME, 3.0D).setRealValue(2.0).getRealValue(), is(2.0));
        assertThat(new BoundVariable(NAME, 3).setIntegerValue(2).getIntegerValue(), is(2L));
        assertThat(new BoundVariable(NAME, true).setLogicValue(false).getLogicValue(), is(false));
        assertThat(new BoundVariable(NAME, "a").setStringValue("b").getStringValue(), is("b"));
        assertThat(new BoundVariable(NAME, 3.0D).setIntegerValue(2).getRealValue(), is(2.0));
        assertThat(new BoundVariable(NAME, 3).setStringValue("2").getIntegerValue(), is(2L));
        assertThat(new BoundVariable(NAME, true).setStringValue("false").getLogicValue(), is(false));
        assertThat(new BoundVariable(NAME, "a").setRealValue(4.0).getStringValue(), is("4.0"));
        assertThat(new BoundVariable(NAME, "x").toString(), is(NAME + ": String = x"));
    }
    
}
