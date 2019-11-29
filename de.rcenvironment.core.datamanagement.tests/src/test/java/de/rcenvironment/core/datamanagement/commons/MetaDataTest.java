/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests cases for {@link MetaData}.
 * 
 * @author Juergen Klein
 * @author Doreen Seider
 */
public class MetaDataTest {

    private final String key = "testKey";

    private MetaData metaData;

    /** Set up. */
    @Before
    public void setUp() {
        metaData = new MetaData(key, true, false);
    }

    /** Test. */
    @Test
    public void test() {
        metaData = new MetaData(key, true, false);
        assertFalse(metaData.isReadOnly());
        assertTrue(new MetaData(key, false, true).isReadOnly());
    }

    /** Test. */
    @Test
    public void testIsReadOnly() {
        assertFalse(metaData.isReadOnly());
        assertTrue(new MetaData(key, false, true).isReadOnly());
    }

    /** Test. */
    @Test
    public void testGetKey() {
        assertEquals(key, metaData.getKey());
    }

    /** Test. */
    @Test
    public void testEquals() {
        assertTrue(metaData.equals(metaData));
        assertTrue(metaData.equals(new MetaData(key, true, false)));
        assertFalse(metaData.equals(new Object()));
        assertFalse(metaData.equals(new MetaData(key, false, true)));
        assertFalse(metaData.equals(new MetaData(key, true, true)));
        assertFalse(metaData.equals(new MetaData("wuattt", true, false)));
        assertFalse(metaData.equals(null));
    }

    /** Test. */
    @Test
    public void testToString() {
        assertEquals(key, metaData.toString());
    }

    /** Test. */
    @Test
    public void testHashCode() {
        metaData.hashCode();
        new MetaData(key, false, true).hashCode();
    }
}
