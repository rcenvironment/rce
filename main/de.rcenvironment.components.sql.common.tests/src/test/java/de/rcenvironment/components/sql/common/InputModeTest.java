/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.sql.common;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test for {@link InputMode}.
 *
 * @author Christian Weiss
 */
public class InputModeTest {
    
    /** Test. */
    @Test
    public void testValues() {
        Assert.assertTrue(InputMode.values().length > 0);
    }
    
    /** Test. */
    @Test
    public void testGetLabel() {
        final InputMode[] modes = InputMode.values();
        final List<String> labels = new LinkedList<String>();
        for (final InputMode mode : modes) {
            final String label = mode.getLabel();
            Assert.assertNotNull(label);
            Assert.assertFalse(label.isEmpty());
            Assert.assertFalse(labels.contains(label));
            labels.add(label);
            final InputMode modeByLabel = InputMode.valueOfLabel(label);
            Assert.assertEquals(mode, modeByLabel);
        }
        try {
            InputMode.valueOfLabel("");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            e = null;
        }
    }
    
    /** Test. */
    @Test
    public void testGetLabels() {
        final String[] labels = InputMode.getLabels();
        Assert.assertNotNull(labels);
        final List<String> labelsList = new LinkedList<String>();
        for (final InputMode mode : InputMode.values()) {
            final String label = mode.getLabel();
            labelsList.add(label);
        }
        Assert.assertArrayEquals(labels, labelsList.toArray(new String[0]));
    }

}
