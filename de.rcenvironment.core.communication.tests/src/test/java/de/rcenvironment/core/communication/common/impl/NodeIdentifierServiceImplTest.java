/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;

/**
 * {@link NodeIdentifierServiceImpl} test.
 * 
 * @author Robert Mischke
 */
public class NodeIdentifierServiceImplTest {

    private final NodeIdentifierServiceImpl service = new NodeIdentifierServiceImpl(IdGeneratorType.FAST);

    /**
     * Tests that associated display names are shared between equal ids as expected.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void testDisplayNameSharing() throws IdentifierException {
        final InstanceNodeId instanceId1 = service.generateInstanceNodeId();
        final InstanceNodeId instanceId1Clone = service.parseInstanceNodeIdString(instanceId1.getInstanceNodeIdString());
        final InstanceNodeId instanceId2 = service.generateInstanceNodeId();
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId1.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId1Clone.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId2.getAssociatedDisplayName());

        final String testName = "name1";
        service.associateDisplayName(instanceId1, testName);
        assertEquals(testName, instanceId1.getAssociatedDisplayName());
        assertEquals(testName, instanceId1Clone.getAssociatedDisplayName());
        // the other id should not have been affected
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId2.getAssociatedDisplayName());
    }

    /**
     * Tests that display names associated with instance session ids are also set for the matching instance ids.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void testTransitiveDisplayNameSetting() throws IdentifierException {
        final InstanceNodeId instanceId1 = service.generateInstanceNodeId();
        final InstanceNodeSessionId instanceId1Session = service.generateInstanceNodeSessionId(instanceId1);
        final InstanceNodeId instanceId2 = service.generateInstanceNodeId();
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId1.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId1Session.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId2.getAssociatedDisplayName());

        final String testName1 = "name1";
        final String testName2 = "name2";
        final String testName3 = "name3";

        service.associateDisplayName(instanceId1Session, testName1); // note: setting for the instance *session* id

        // sanity check
        assertEquals(testName1, instanceId1Session.getAssociatedDisplayName());
        // now test the transitive setting
        assertEquals(testName1, instanceId1.getAssociatedDisplayName());
        // the other id should not have been affected
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instanceId2.getAssociatedDisplayName());

        // replace the name for the instanceId only; it should not propagate back the the session id
        service.associateDisplayName(instanceId1, testName2); // note: setting for the *instance* id

        assertEquals(testName1, instanceId1Session.getAssociatedDisplayName());
        assertEquals(testName2, instanceId1.getAssociatedDisplayName());

        service.associateDisplayName(instanceId1Session, testName3); // note: setting for the instance *session* id

        assertEquals(testName3, instanceId1Session.getAssociatedDisplayName());
        assertEquals(testName3, instanceId1.getAssociatedDisplayName());
    }

}
