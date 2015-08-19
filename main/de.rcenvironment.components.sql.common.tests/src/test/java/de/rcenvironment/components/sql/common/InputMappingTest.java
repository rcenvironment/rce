/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common;

import org.junit.Assert;
import org.junit.Test;

import de.rcenvironment.components.sql.common.InputMapping.ColumnMapping;

/**
 * Test case for {@link InputMapping}.
 * 
 * @author Christian Weiss
 */
public class InputMappingTest {

    /** Test. */
    @Test
    public void testAdd() {
        final InputMapping inputMapping = new InputMapping();
        final ColumnMapping columnMapping1 = inputMapping.add("name0", ColumnType.IGNORE);
        final ColumnMapping columnMapping2 = inputMapping.add("name00", ColumnType.IGNORE);
        Assert.assertTrue(inputMapping.contains(columnMapping1));
        Assert.assertTrue(inputMapping.contains(columnMapping2));
        inputMapping.remove(columnMapping1);
        Assert.assertFalse(inputMapping.contains(columnMapping1));
        Assert.assertTrue(inputMapping.contains(columnMapping2));
    }
    
    /** Test. */
    @Test
    public void testGetIndex() {
        final InputMapping inputMapping = new InputMapping();
        final ColumnMapping columnMapping1 = inputMapping.add("name10", ColumnType.IGNORE);
        Assert.assertEquals(0, columnMapping1.getIndex());
        final ColumnMapping columnMapping2 = inputMapping.add("name100", ColumnType.IGNORE);
        Assert.assertEquals(0, columnMapping1.getIndex());
        Assert.assertEquals(1, columnMapping2.getIndex());
        inputMapping.remove(columnMapping1);
        Assert.assertEquals(0, columnMapping2.getIndex());
    }

    /** Test. */
    @Test
    public void testMoveUp() {
        final InputMapping inputMapping = new InputMapping();
        final ColumnMapping columnMapping1 = inputMapping.add("name20", ColumnType.IGNORE);
        final ColumnMapping columnMapping2 = inputMapping.add("name200", ColumnType.IGNORE);
        final ColumnMapping columnMapping3 = inputMapping.add("name2000", ColumnType.IGNORE);
        Assert.assertEquals(0, columnMapping1.getIndex());
        Assert.assertEquals(1, columnMapping2.getIndex());
        Assert.assertEquals(2, columnMapping3.getIndex());
        // move up when not in position 0
        inputMapping.moveUp(columnMapping2);
        Assert.assertEquals(0, columnMapping2.getIndex());
        Assert.assertEquals(1, columnMapping1.getIndex());
        Assert.assertEquals(2, columnMapping3.getIndex());
        // move up when in position 0
        inputMapping.moveUp(columnMapping2);
        Assert.assertEquals(0, columnMapping2.getIndex());
        Assert.assertEquals(1, columnMapping1.getIndex());
        Assert.assertEquals(2, columnMapping3.getIndex());
    }

    /** Test. */
    @Test
    public void testMoveDown() {
        final InputMapping inputMapping = new InputMapping();
        final ColumnMapping columnMapping1 = inputMapping.add("name30", ColumnType.IGNORE);
        final ColumnMapping columnMapping2 = inputMapping.add("name300", ColumnType.IGNORE);
        final ColumnMapping columnMapping3 = inputMapping.add("name3000", ColumnType.IGNORE);
        Assert.assertEquals(0, columnMapping1.getIndex());
        Assert.assertEquals(1, columnMapping2.getIndex());
        Assert.assertEquals(2, columnMapping3.getIndex());
        // move up when not in position <last>
        inputMapping.moveDown(columnMapping2);
        Assert.assertEquals(0, columnMapping1.getIndex());
        Assert.assertEquals(1, columnMapping3.getIndex());
        Assert.assertEquals(2, columnMapping2.getIndex());
        // move up when in position <last>
        inputMapping.moveDown(columnMapping2);
        Assert.assertEquals(0, columnMapping1.getIndex());
        Assert.assertEquals(1, columnMapping3.getIndex());
        Assert.assertEquals(2, columnMapping2.getIndex());
    }

    /** Test. */
    @Test
    public void testSerialization() {
        final InputMapping inputMapping = new InputMapping();
        String serialized;
        // zero columns
        serialized = inputMapping.serialize();
        Assert.assertEquals("", serialized);
        @SuppressWarnings("unused")
        ColumnMapping columnMapping;
        // one column (name,String)
        columnMapping = inputMapping.add("name1", ColumnType.STRING);
        serialized = inputMapping.serialize();
        Assert.assertEquals("name1#STRING", serialized);
        // two columns (name,String),(id,Integer)
        columnMapping = inputMapping.add("id1", ColumnType.INTEGER);
        serialized = inputMapping.serialize();
        Assert.assertEquals("name1#STRING;id1#INTEGER", serialized);
    }

    /** Test. */
    @Test
    public void testDeserialization() {
        InputMapping inputMapping;
        // zero columns
        inputMapping = InputMapping.deserialize("");
        Assert.assertEquals(0, inputMapping.size());
        // one column (name,String)
        inputMapping = InputMapping.deserialize("#IGNORE");
        Assert.assertEquals(1, inputMapping.size());
        inputMapping = InputMapping.deserialize("#");
        Assert.assertEquals(1, inputMapping.size());
        Assert.assertEquals(ColumnType.IGNORE, inputMapping.get(0).getType());
        inputMapping = InputMapping.deserialize("name2#STRING");
        Assert.assertEquals(1, inputMapping.size());
        // two columns (name,String),(id,Integer)
        inputMapping = InputMapping.deserialize("name2#STRING;id2#INTEGER");
        Assert.assertEquals(2, inputMapping.size());
        Assert.assertEquals("name2", inputMapping.get(0).getName());
        Assert.assertEquals(ColumnType.STRING, inputMapping.get(0).getType());
        Assert.assertEquals("id2", inputMapping.get(1).getName());
        Assert.assertEquals(ColumnType.INTEGER, inputMapping.get(1).getType());
        inputMapping = InputMapping.deserialize("#;#");
        Assert.assertEquals(2, inputMapping.size());
    }

}
