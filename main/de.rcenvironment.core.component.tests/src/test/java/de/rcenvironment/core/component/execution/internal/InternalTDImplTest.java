/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;


import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.Test;

import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;

/**
 * Tests serialization of {@link InternalTDImpl}.
 * 
 * @author Doreen Seider
 */
public class InternalTDImplTest {

    /**
     * Test.
     */
    @Test
    public void testDeSerializationInternalTD() {
        
        
        InternalTDImpl.InternalTDType type = InternalTDImpl.InternalTDType.WorkflowFinish;
        InternalTDImpl internalTD = new InternalTDImpl(type);
        InternalTDImpl deSerializedInternal = InternalTDImpl.fromString(internalTD.serialize());
        assertEquals(internalTD.getType(), deSerializedInternal.getType());
        assertEquals(internalTD.getIdentifier(), deSerializedInternal.getIdentifier());
        assertEquals(internalTD.getHopsToTraverse(), deSerializedInternal.getHopsToTraverse());
        
        type = InternalTDImpl.InternalTDType.NestedLoopReset;
        WorkflowGraphHop graphHop1 = new WorkflowGraphHop("hopExeId1", "hopOutputName1", "targetExeId1", "targetInputName1");
        WorkflowGraphHop graphHop2 = new WorkflowGraphHop("hopExeId2", "hopOutputName2", "targetExeId2", "targetInputName2");
        Queue<WorkflowGraphHop> graphHops = new LinkedList<>();
        graphHops.add(graphHop1);
        graphHops.add(graphHop2);
        internalTD = new InternalTDImpl(type, graphHops);
        deSerializedInternal = InternalTDImpl.fromString(internalTD.serialize());
        assertEquals(internalTD.getType(), deSerializedInternal.getType());
        assertEquals(internalTD.getIdentifier(), deSerializedInternal.getIdentifier());
        WorkflowGraphHop deSerializedGraphHop1 = deSerializedInternal.getHopsToTraverse().poll();
        assertEquals(graphHop1.getHopExecutionIdentifier(), deSerializedGraphHop1.getHopExecutionIdentifier());
        assertEquals(graphHop1.getHopOuputName(), deSerializedGraphHop1.getHopOuputName());
        assertEquals(graphHop1.getTargetExecutionIdentifier(), deSerializedGraphHop1.getTargetExecutionIdentifier());
        assertEquals(graphHop1.getTargetInputName(), deSerializedGraphHop1.getTargetInputName());
        WorkflowGraphHop deSerializedGraphHop2 = deSerializedInternal.getHopsToTraverse().poll();
        assertEquals(graphHop2.getHopExecutionIdentifier(), deSerializedGraphHop2.getHopExecutionIdentifier());
        assertEquals(graphHop2.getHopOuputName(), deSerializedGraphHop2.getHopOuputName());
        assertEquals(graphHop2.getTargetExecutionIdentifier(), deSerializedGraphHop2.getTargetExecutionIdentifier());
        assertEquals(graphHop2.getTargetInputName(), deSerializedGraphHop2.getTargetInputName());
    }

}
