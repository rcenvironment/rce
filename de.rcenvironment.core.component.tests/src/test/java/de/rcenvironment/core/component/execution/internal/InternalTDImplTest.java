/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Tests of {@link InternalTDImpl}.
 * 
 * @author Doreen Seider
 */
public class InternalTDImplTest {

    private static final String HOP_EXE_ID = "hopExeId";

    private static final String HOP_OUTPUT_NAME = "hopOutputName";

    private static final String TARGET_EXE_ID = "targetExeId";

    private static final String TARGET_OUTPUT_NAME = "targetInputName";

    private static final String INTERNAL_TD_ID = "69d1235c-3b24-40f1-b908-caad3d88f1f2";

    private static final String SERIALIZED_WF_FINISH_TD = StringUtils.format("{\"t\":\"WorkflowFinish\",\"i\":\"%s\"}", INTERNAL_TD_ID);

    private static final String INVALID_SERIALIZED_TD = "{\"t\":\"Flt\",\"v\":\"3.5\"}";

    private static final String SERIALIZED_RESET_LOOP_TD =
        StringUtils.format("{\"t\":\"NestedLoopReset\",\"i\":\"%s\",\"h\":[[\"%s\",\"%s\",\"%s\",\"%s\"]]}", INTERNAL_TD_ID,
            HOP_EXE_ID, HOP_OUTPUT_NAME, TARGET_EXE_ID, TARGET_OUTPUT_NAME);

    /**
     * Tests if an InternalTD is serialized as expected and can be deserialized back to an InternalTD as expected so that it is equal to the
     * origin.
     */
    @Test
    public void testDeSerializationInternalTD() {

        InternalTDImpl.InternalTDType type = InternalTDImpl.InternalTDType.WorkflowFinish;
        InternalTDImpl internalTD = new InternalTDImpl(type, INTERNAL_TD_ID);
        String tdStr = internalTD.serialize();
        assertEquals(SERIALIZED_WF_FINISH_TD, tdStr); // ensures backward compatibility
        InternalTDImpl deSerializedInternal = InternalTDImpl.fromString(tdStr);
        assertEquals(internalTD.getType(), deSerializedInternal.getType());
        assertEquals(internalTD.getIdentifier(), deSerializedInternal.getIdentifier());
        assertEquals(internalTD.getHopsToTraverse(), deSerializedInternal.getHopsToTraverse());

        type = InternalTDImpl.InternalTDType.NestedLoopReset;
        WorkflowGraphHop graphHop = new WorkflowGraphHop(new ComponentExecutionIdentifier(HOP_EXE_ID), HOP_OUTPUT_NAME,
            new ComponentExecutionIdentifier(TARGET_EXE_ID), TARGET_OUTPUT_NAME);
        Queue<WorkflowGraphHop> graphHops = new LinkedList<>();
        graphHops.add(graphHop);
        internalTD = new InternalTDImpl(type, INTERNAL_TD_ID, graphHops);
        tdStr = internalTD.serialize();
        assertEquals(SERIALIZED_RESET_LOOP_TD, tdStr); // ensures backward compatibility
        deSerializedInternal = InternalTDImpl.fromString(tdStr);
        assertEquals(internalTD.getType(), deSerializedInternal.getType());
        assertEquals(internalTD.getIdentifier(), deSerializedInternal.getIdentifier());
        WorkflowGraphHop deSerializedGraphHop1 = deSerializedInternal.getHopsToTraverse().poll();
        assertEquals(graphHop.getHopExecutionIdentifier(), deSerializedGraphHop1.getHopExecutionIdentifier());
        assertEquals(graphHop.getHopOuputName(), deSerializedGraphHop1.getHopOuputName());
        assertEquals(graphHop.getTargetExecutionIdentifier(), deSerializedGraphHop1.getTargetExecutionIdentifier());
        assertEquals(graphHop.getTargetInputName(), deSerializedGraphHop1.getTargetInputName());
    }

    /**
     * Tests if {@link InternalTDImpl#fromString(String)} returns a <code>null</code> value if an invalid string input is passed.
     */
    @Test
    public void testDeserializationForInvalidInput() {
        assertNull(InternalTDImpl.fromString(INVALID_SERIALIZED_TD));
    }

}
