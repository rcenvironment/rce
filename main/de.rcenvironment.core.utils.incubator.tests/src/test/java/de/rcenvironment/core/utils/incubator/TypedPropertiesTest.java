/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Test cases for {@link TypedProperties}.
 * 
 * @author Arne Bachmann
 */
public class TypedPropertiesTest {

    private static final String BLA = "bla";
    private static final String STRING = "string";
    private static final String DOESN_T_EXIST = "doesn't exist";
    private static final String UNVALUED = "valuesless";
    private static final double DOUBLE = 3.14159D;
    private static final double LONG1 = 23;

    /**
     * Test.
     */
    @Test
    public void testTypedness() {
        final TypedProperties<String> p = new TypedProperties<String>() { };
        assertFalse(Long.class.isInstance(LONG1));
        assertTrue(String.class.isInstance(""));
        
        // try to retrieve non-existing value
        assertNull(p.get(DOESN_T_EXIST, String.class));
        assertNotNull(p.containsKey(DOESN_T_EXIST));
        assertNull(p.getType(DOESN_T_EXIST));
        
        // add and retrieve values
        p.put(STRING, BLA); // not allowed to throw
        assertThat(p.<String>get(STRING, String.class), is(BLA)); // not allowd to throw
        assertTrue(p.getType(STRING) == String.class);
        assertTrue(p.containsKey(STRING));
        
        // wrong type checking
        try {
            p.put(STRING, new Long(1));
            fail("Should have thrown an IllegalStateException");
        } catch (final IllegalStateException e) {
            assertTrue(true);
        } // expected
        try {
            p.get(STRING, Long.class);
            fail("Should have thrown an IllegalStateException");
        } catch (final IllegalStateException e) {
            assertTrue(true);
        } // expected
        p.put(STRING, new Long(1), false); // now overwrite value
        
        // set type in advance
        p.setType(UNVALUED, Double.class);
        assertNull(p.get(UNVALUED, Double.class));
        assertTrue(p.getType(UNVALUED) == Double.class);
        try {
            p.put(UNVALUED, STRING);
            fail("Should have thrown an exception due to expected type");
        } catch (final IllegalStateException e) {
            assertTrue(true);
        } // expected
        p.put(UNVALUED, DOUBLE);
        assertThat(p.get(UNVALUED, Double.class), is(DOUBLE));
    }
    
}
