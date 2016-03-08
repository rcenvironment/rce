/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.update.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.update.api.DistributedPersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.workflow.ComponentInstallationMockFactory;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import junit.framework.Assert;

/**
 * Test cases for {@link PersistentWorkflowDescriptionUpdateServiceImpl}.
 * 
 * @author Sascha Zur
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
        Assert.assertEquals("4", persistentWorkflowDescriptionV3.getWorkflowVersion());

    }
    
    /**
     * Tests if the node identifier are set correctly.
     */
    @Test
    public void testCheckAndSetNodeIdentifier() {
        
        final String componentId = "de.rce.comp-A";
        final String version = "1.1";
        final String componentIdPlusVersion = componentId + ComponentConstants.ID_SEPARATOR + version;
        String nodeId1 = "node-id-1";
        
        PersistentWorkflowDescriptionUpdateServiceImpl updateService = new PersistentWorkflowDescriptionUpdateServiceImpl();
        updateService.bindPlatformService(new PlatformServiceDefaultStub() {
            @Override
            public NodeIdentifier getLocalNodeId() {
                return NodeIdentifierFactory.fromNodeId("local-node-id");
            }
        });
        PersistentComponentDescription persCompDesc = EasyMock.createStrictMock(PersistentComponentDescription.class);
        EasyMock.expect(persCompDesc.getComponentIdentifier()).andStubReturn(componentId);
        EasyMock.expect(persCompDesc.getComponentVersion()).andStubReturn(version);
        EasyMock.expect(persCompDesc.getComponentNodeIdentifier()).andStubReturn(NodeIdentifierFactory.fromNodeId(nodeId1));
        Capture<NodeIdentifier> capturedNodeId = new Capture<>();
        persCompDesc.setNodeIdentifier(EasyMock.capture(capturedNodeId));
        EasyMock.replay(persCompDesc);
        
        Collection<ComponentInstallation> compInstalls = new ArrayList<>();
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, "node-id-2"));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId1));
        
        PersistentComponentDescription resultPersCompDesc = updateService.checkAndSetNodeIdentifier(persCompDesc, compInstalls);
        
        assertEquals(0, capturedNodeId.getValues().size());
        assertEquals(persCompDesc.getComponentNodeIdentifier(), resultPersCompDesc.getComponentNodeIdentifier());
    }
    
}
