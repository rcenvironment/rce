/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.testutils.AuthorizationTestUtils;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.testutils.ComponentTestUtils;

/**
 * Test for {@link DistributedComponentKnowledgeService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class DistributedComponentKnowledgeServiceTest {

    private DistributedComponentKnowledgeServiceImpl service;

    private Collection<DistributedComponentEntry> localInstallations = new ArrayList<>();

    private LogicalNodeId defaultComponentLocationId;

    private final AuthorizationService authorizationServiceStub = AuthorizationTestUtils.createAuthorizationServiceStub();

    private final AuthorizationPermissionSet publicPermissionSet =
        authorizationServiceStub.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();

    /**
     * Set up.
     */
    @Before
    public void setUp() {

        LogicalNodeSessionId localNodeId = NodeIdentifierTestUtils.createTestLogicalNodeSessionId(true);
        defaultComponentLocationId = localNodeId.convertToLogicalNodeId();

        service = new DistributedComponentKnowledgeServiceImpl(localNodeId.convertToInstanceNodeSessionId());
        service.activate();

        localInstallations
            .add(ComponentTestUtils.createTestDistributedComponentEntry("id-1", "1", defaultComponentLocationId,
                publicPermissionSet, authorizationServiceStub));
        localInstallations
            .add(ComponentTestUtils.createTestDistributedComponentEntry("id-2", "2", defaultComponentLocationId, null,
                authorizationServiceStub));

    }

    /**
     * Tests if there are no null values in the delta of published components if no installation was removed, but one added.
     */
    @Test
    public void testDeltaOfPublishedInstallations() {

        NodePropertiesService nodePropertiesService = EasyMock.createStrictMock(NodePropertiesService.class);
        Capture<Map<String, String>> firstDelta = new Capture<Map<String, String>>();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(firstDelta));
        Capture<Map<String, String>> secondDelta = new Capture<Map<String, String>>();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(secondDelta));
        Capture<Map<String, String>> thirdDelta = new Capture<Map<String, String>>();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(thirdDelta));
        service.bindNodePropertiesService(nodePropertiesService);
        EasyMock.replay(nodePropertiesService);

        service.updateLocalComponentInstallations(localInstallations, true);

        Map<String, String> delta = firstDelta.getValue();
        assertTrue(!delta.containsValue(null));
        assertEquals(1, delta.size());

        final DistributedComponentEntry entry3 =
            ComponentTestUtils.createTestDistributedComponentEntry("id-3", "3", defaultComponentLocationId,
                publicPermissionSet, authorizationServiceStub);
        localInstallations.add(entry3);

        final DistributedComponentEntry entry4 =
            ComponentTestUtils.createTestDistributedComponentEntry("id-4", "4", defaultComponentLocationId,
                publicPermissionSet, authorizationServiceStub);
        localInstallations.add(entry4);

        service.updateLocalComponentInstallations(localInstallations, true);

        delta = secondDelta.getValue();
        assertTrue(!delta.containsValue(null));
        assertEquals(2, delta.size());

        localInstallations.remove(entry3);

        // TODO add test(s) for change of component publication type (LOCAL vs SHARED) and/or authorization settings

        service.updateLocalComponentInstallations(localInstallations, true);

        delta = thirdDelta.getValue();
        assertTrue(delta.containsValue(null));
        assertEquals(1, delta.size());

    }
}
