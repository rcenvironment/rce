/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.update.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.update.api.DistributedPersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.workflow.ComponentInstallationMockFactory;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import junit.framework.Assert;

/**
 * Test cases for {@link PersistentWorkflowDescriptionUpdateServiceImpl}.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class PersistentWorkflowDescriptionUpdateServiceImplTest {

    /**
     * Tests if the workflow version is updated correctly.
     * 
     * @throws IOException on error
     * @throws JsonParseException on error
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test() throws JsonParseException, IOException {
        String persistentDescriptionV3 = "{"
            + "\"identifier\" : \"697261b6-eaf5-44ab-af40-6c161a4f26f8\","
            + "\"workflowVersion\" : \"3\""
            + "}";

        List<PersistentComponentDescription> descriptions = new ArrayList<PersistentComponentDescription>();

        PersistentWorkflowDescription persistentWorkflowDescriptionV3 =
            new PersistentWorkflowDescription(descriptions, persistentDescriptionV3);

        PersistentWorkflowDescriptionUpdateServiceImpl updateService = new PersistentWorkflowDescriptionUpdateServiceImpl();
        DistributedPersistentComponentDescriptionUpdateService componentUpdateServiceMock = EasyMock
            .createMock(DistributedPersistentComponentDescriptionUpdateService.class);
        List<PersistentComponentDescription> componentDescriptions = new LinkedList<PersistentComponentDescription>();
        EasyMock.expect(componentUpdateServiceMock.performComponentDescriptionUpdates(EasyMock.anyInt(),
            EasyMock.anyObject(new LinkedList<PersistentComponentDescription>().getClass()),
            EasyMock.anyBoolean())).andReturn(componentDescriptions).anyTimes();
        EasyMock.replay(componentUpdateServiceMock);

        updateService.bindComponentDescriptionUpdateService(componentUpdateServiceMock);
        persistentWorkflowDescriptionV3 = updateService.performWorkflowDescriptionUpdate(persistentWorkflowDescriptionV3);
        Assert.assertEquals(String.valueOf(WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER),
            persistentWorkflowDescriptionV3.getWorkflowVersion());

    }

    /**
     * Tests if the node identifier are set correctly.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void testCheckAndSetNodeIdentifier() throws IdentifierException {

        final String componentId = "de.rce.comp-A";
        final String version = "1.1";
        final String componentIdPlusVersion = componentId + ComponentConstants.ID_SEPARATOR + version;

        final LogicalNodeId localDefaultLogicalNodeId = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        final LogicalNodeId nodeId1 = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        final LogicalNodeId nodeId2 = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        final LogicalNodeId nodeId3 = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();

        PersistentWorkflowDescriptionUpdateServiceImpl updateService = new PersistentWorkflowDescriptionUpdateServiceImpl();
        updateService.bindPlatformService(new PlatformServiceDefaultStub() {

            @Override
            public LogicalNodeId getLocalDefaultLogicalNodeId() {
                return localDefaultLogicalNodeId;
            }
        });
        PersistentComponentDescription persCompDesc = EasyMock.createStrictMock(PersistentComponentDescription.class);
        EasyMock.expect(persCompDesc.getComponentIdentifier()).andStubReturn(componentId);
        EasyMock.expect(persCompDesc.getComponentVersion()).andStubReturn(version);
        EasyMock.expect(persCompDesc.getComponentNodeIdentifier()).andStubReturn(nodeId1);
        Capture<LogicalNodeId> capturedNodeId = new Capture<>();
        persCompDesc.setNodeIdentifier(EasyMock.capture(capturedNodeId));
        EasyMock.expectLastCall().asStub();
        EasyMock.replay(persCompDesc);

        Collection<ComponentInstallation> compInstalls = new ArrayList<>();
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId2));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId1));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version,
            localDefaultLogicalNodeId));

        updateService.checkAndSetNodeIdentifier(persCompDesc, compInstalls);
        assertTrue(capturedNodeId.hasCaptured());
        assertEquals(localDefaultLogicalNodeId, capturedNodeId.getValue());

        compInstalls = new ArrayList<>();
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId2));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId1));

        updateService.checkAndSetNodeIdentifier(persCompDesc, compInstalls);
        assertTrue(capturedNodeId.hasCaptured());
        assertEquals(nodeId1, capturedNodeId.getValue());

        compInstalls = new ArrayList<>();
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId2));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId3));

        updateService.checkAndSetNodeIdentifier(persCompDesc, compInstalls);
        assertTrue(capturedNodeId.hasCaptured());
        assertTrue(capturedNodeId.getValue().equals(nodeId2) || capturedNodeId.getValue().equals(nodeId3));

        updateService.checkAndSetNodeIdentifier(persCompDesc, new ArrayList<ComponentInstallation>());
        assertTrue(capturedNodeId.hasCaptured());
        assertEquals(localDefaultLogicalNodeId, capturedNodeId.getValue());

    }

}
