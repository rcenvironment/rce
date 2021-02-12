/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.EnumUtils;
import org.junit.Test;

import de.rcenvironment.core.utils.incubator.StateChangeException;

/**
 * Test cases for {@link ComponentStateMachine}.
 * 
 * @author Doreen Seider
 */
public class ComponentStateMachineTest {

    /**
     * Tests if each {@link ComponentStateMachineEvent} is covered by an {@link ComponentStateMachine.EventProcessorEventProcessor}.
     * @throws StateChangeException on unexpected error
     */
    @Test
    public void testEventProcessorsInitialization() throws StateChangeException {
        @SuppressWarnings("deprecation")
        ComponentStateMachine compStateMachine = new ComponentStateMachine();
        compStateMachine.initializeEventProcessors();
        for (ComponentStateMachineEventType eventType : EnumUtils.getEnumList(ComponentStateMachineEventType.class)) {
            assertTrue(compStateMachine.eventProcessors.containsKey(eventType));
        }
    }

}
