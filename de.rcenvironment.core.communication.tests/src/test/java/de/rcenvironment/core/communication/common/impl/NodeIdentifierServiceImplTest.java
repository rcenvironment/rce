/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

        // first, test the basic behavior: an instance session's name should be shared between ids of the same instance,
        // but should not affect a different instance's ids

        final InstanceNodeId instance1Id = service.generateInstanceNodeId();
        final InstanceNodeId instance2Id = service.generateInstanceNodeId();
        final InstanceNodeSessionId instance1Session1Id = service.generateInstanceNodeSessionId(instance1Id);
        // generate a new id object pointing to the same instance session
        final InstanceNodeSessionId instance1Session1IdClone =
            service.parseInstanceNodeSessionIdString(instance1Session1Id.getInstanceNodeSessionIdString());

        // no explicit name set -> all should be at default
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance1Id.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance1Session1Id.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance1Session1IdClone.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());

        final String testName = "instance1_session1";
        service.associateDisplayName(instance1Session1Id, testName);
        // should be set for the exact instance session id object
        assertEquals(testName, instance1Session1Id.getAssociatedDisplayName());
        // should be set for the cloned instance session id object, too
        assertEquals(testName, instance1Session1IdClone.getAssociatedDisplayName());
        // should NOT affect a different instance id
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());
    }

    /**
     * Tests that display names associated with instance session ids are also set for the matching instance ids.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void testTransitiveDisplayNameSetting() throws IdentifierException {

        // same general setup as in basic test

        final InstanceNodeId instance1Id = service.generateInstanceNodeId();
        final InstanceNodeId instance2Id = service.generateInstanceNodeId();
        final InstanceNodeSessionId instance1Session1Id = service.generateInstanceNodeSessionId(instance1Id);
        final InstanceNodeSessionId instance1Session2Id = service.generateInstanceNodeSessionId(instance1Id);

        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance1Id.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance1Session1Id.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance1Session2Id.getAssociatedDisplayName());
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());

        final String testName1 = "name1";
        final String testName2 = "name2";
        final String testName3 = "name3";

        // attach an explicit name to the *older* instance session
        service.associateDisplayName(instance1Session1Id, testName1);

        // test the name that was explicitly set (sanity check)
        assertEquals(testName1, instance1Session1Id.getAssociatedDisplayName());
        // the session's name should have propagated to the underlying instance
        assertEquals(testName1, instance1Id.getAssociatedDisplayName());
        // it should also have propagated to the future session with no explicit name association yet (inherited from instance)
        assertEquals(testName1, instance1Session2Id.getAssociatedDisplayName());
        // the other instance should not have been affected (sanity check)
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());

        // now attach an explicit name to the *newer* instance session
        service.associateDisplayName(instance1Session2Id, testName2);

        // test the name that was explicitly set (sanity check)
        assertEquals(testName2, instance1Session2Id.getAssociatedDisplayName());
        // the session's name should have propagated to the underlying instance, as the session id is *newer*
        assertEquals(testName2, instance1Id.getAssociatedDisplayName());
        // it should have propagated to the outdated session, too, but the name should have the "outdated" marker attached
        assertEquals(testName2 + CommonIdBase.DISPLAY_NAME_SUFFIX_FOR_OUTDATED_SESSIONS, instance1Session1Id.getAssociatedDisplayName());
        // the other instance should not have been affected (sanity check)
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());

        // now try to attach a different explicit name to the *older* instance session again (which should NOT work)
        service.associateDisplayName(instance1Session1Id, testName3); // note: setting for the instance *session* id

        // the name change request for the outdated session should have been ignored (only store the latest session's data)
        assertEquals(testName2 + CommonIdBase.DISPLAY_NAME_SUFFIX_FOR_OUTDATED_SESSIONS, instance1Session1Id.getAssociatedDisplayName());
        // the older session's name change request should NOT have propagated to the underlying instance (should keep the second name)
        assertEquals(testName2, instance1Id.getAssociatedDisplayName());

        // the other instance should not have been affected (sanity check)
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());
        // the session's name should have propagated to the underlying instance, as the session id is *newer*
        assertEquals(testName2, instance1Id.getAssociatedDisplayName());
        // it should also NOT have affected the newer session
        assertEquals(testName2, instance1Session2Id.getAssociatedDisplayName());
        // the other instance should not have been affected (sanity check)
        assertEquals(CommonIdBase.DEFAULT_DISPLAY_NAME, instance2Id.getAssociatedDisplayName());
    }

}
