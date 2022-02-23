/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;


/**
 * Test cases for {@link MetaDataSet}.
 *
 * @author Juergen Klein
 */
public class MetaDataSetTest {

    private final MetaData metaData1 = new MetaData("TestKey1", false);
    private final MetaData metaData2 = new MetaData("TestKey2", false);
    private final MetaData metaData3 = new MetaData("TestKey3", false);

    private final String value1 = "TestValue1";
    private final String value2 = "TestValue2";
    private final String value3 = "TestValue3";

    private MetaDataSet metaDataSet;

    /** Set up. */
    @Before
    public void setUp() {
        metaDataSet = new MetaDataSet();
        metaDataSet.setValue(metaData1, value1);
        metaDataSet.setValue(metaData2, value2);
    }

    /** Test. */
    @Test
    public void testHashCode() {
        int hashCode1 = metaDataSet.hashCode();
        metaDataSet.setValue(metaData1, value3);
        assertFalse(hashCode1 == metaDataSet.hashCode());
    }

    /** Test. */
    @Test
    public void testMetaDataSet() {
        new MetaDataSet();
    }

    /** Test. */
    @Test
    public void testGetValue() {
        String value = metaDataSet.getValue(metaData1);
        assertEquals(value1, value);
        value = metaDataSet.getValue(metaData2);
        assertEquals(value2, value);
    }

    /** Test. */
    @Test
    public void testSetValue() {
        metaDataSet.setValue(metaData3, value3);
        String value = metaDataSet.getValue(metaData3);
        assertEquals(value3, value);
    }

    /** Test. */
    @Test
    public void testRemove() {
        metaDataSet.remove(MetaData.AUTHOR);
        metaDataSet.setValue(metaData3, value3);
        assertNotNull(metaDataSet.getValue(metaData3));
        metaDataSet.remove(metaData3);
        assertNull(metaDataSet.getValue(metaData3));
    }

    /** Test. */
    @Test
    public void testIterator() {
        metaDataSet.setValue(metaData3, value3);
        Iterator<MetaData> iterator = metaDataSet.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        iterator.next();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());
    }

    /** Test. */
    @Test
    public void testClone() {
        MetaDataSet clone = metaDataSet.clone();
        assertNotSame(metaDataSet, clone);
        assertEquals(value1, clone.getValue(metaData1));
        assertEquals(value2, clone.getValue(metaData2));
    }

    /** Test. */
    @Test
    public void testIsEmpty() {
        assertFalse(metaDataSet.isEmpty());
        assertTrue(new MetaDataSet().isEmpty());
    }

    /** Test. */
    @Test
    public void testEquals() {
        assertTrue(metaDataSet.equals(metaDataSet));
        MetaDataSet mds = new MetaDataSet();
        mds.setValue(metaData1, value1);
        mds.setValue(metaData2, value2);
        assertTrue(metaDataSet.equals(mds));
        assertTrue(mds.equals(metaDataSet));
        MetaDataSet clone = metaDataSet.clone();
        clone.setValue(metaData3, value3);
        assertFalse(clone.equals(metaDataSet));
        assertFalse(metaDataSet.equals(null));
        assertFalse(metaDataSet.equals(new MetaDataSetTest()));
    }

    /** Test. */
    @Test
    public void testToString() {
        metaDataSet.toString();
    }

}
