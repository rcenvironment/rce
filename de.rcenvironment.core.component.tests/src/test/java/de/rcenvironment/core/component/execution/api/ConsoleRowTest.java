/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import de.rcenvironment.core.component.execution.impl.ConsoleRowImpl;


/**
 * Test cases for ConsoleRow.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ConsoleRowTest {

    private static final int PREVENT_TIMESTAMP_COLLISION_WAIT_TIME_MSEC = 100;

    private final String workflowId = "workflow-id";

    private final String componentId = "component-id";
    
    private final String workflowName = "workflow-name";

    private final String componentName = "component-name";

    private final String firstMessageLine = "1. console message line";
    
    private final String secondMessageLine = "2. console message line";

    private final ConsoleRow.Type type = ConsoleRow.Type.TOOL_OUT;

    /** Test. */
    @Test
    public void test() {
        ConsoleRowImpl row1 = new ConsoleRowImpl();
        row1.setWorkflowIdentifier(workflowId);
        row1.setComponentIdentifier(componentId);
        row1.setWorkflowName(workflowName);
        row1.setComponentName(componentName);
        row1.setType(type);
        row1.setPayload(firstMessageLine);
        row1.setTimestamp(1);
        
        assertTrue(row1.getComponentIdentifier().equals(componentId));
        assertTrue(row1.getWorkflowIdentifier().equals(workflowId));
        assertTrue(row1.getComponentName().equals(componentName));
        assertTrue(row1.getPayload().equals(firstMessageLine));
        assertNotNull(row1.getTimestamp());
        assertTrue(row1.getType().equals(type));
        assertTrue(row1.getWorkflowName().equals(workflowName));
        assertTrue(row1.toString().contains(componentName));
        assertTrue(row1.toString().contains(firstMessageLine));
        assertTrue(row1.toString().contains(type.toString()));
        assertTrue(row1.toString().contains(workflowName));

        ConsoleRowImpl row2 = new ConsoleRowImpl();
        row2.setWorkflowIdentifier(workflowId);
        row2.setComponentIdentifier(componentId);
        row2.setWorkflowName(workflowName);
        row2.setComponentName(componentName);
        row2.setType(type);
        row2.setPayload(secondMessageLine);
        row2.setTimestamp(2);
        
        assertTrue(row2.compareTo(row1) > 0);

        row1.setIndex(1);
        row2.setIndex(2);

        assertTrue(row2.compareTo(row1) > 0);

        // prevent rows 1 and 3 having the same timestamp by accident
        try {
            Thread.sleep(PREVENT_TIMESTAMP_COLLISION_WAIT_TIME_MSEC);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        // create row with same data and new number;
        // should not be equal due to timestamp and index
        ConsoleRowImpl row3 = new ConsoleRowImpl();
        row3.setWorkflowIdentifier(workflowId);
        row3.setComponentIdentifier(componentId);
        row3.setWorkflowName(workflowName);
        row3.setComponentName(componentName);
        row3.setType(type);
        row3.setPayload(firstMessageLine);
        row3.setTimestamp(3);
        
        row3.setIndex(3);
        assertFalse(row3.equals(row1));
        assertFalse(row1.equals(row3));

        // set to same index; should still not be equal due to timestamp
        row3.setIndex(1);
        assertFalse(row3.equals(row1));
        assertFalse(row1.equals(row3));

        // set to same timestamp; now they should be equal
        row3.setTimestamp(row1.getTimestamp());
        assertTrue(row3.equals(row1));
        assertTrue(row1.equals(row3));

        assertEquals(row1.toString().hashCode(), row1.hashCode());

    }
}
