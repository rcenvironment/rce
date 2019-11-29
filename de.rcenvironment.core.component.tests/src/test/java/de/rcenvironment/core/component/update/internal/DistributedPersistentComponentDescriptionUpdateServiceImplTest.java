/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.update.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.component.ComponentInstallationMockFactory;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.testutils.ComponentTestUtils;
import de.rcenvironment.core.component.testutils.DistributedComponentKnowledgeServiceDefaultStub;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.api.RemotablePersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Test cases for {@link DistributedPersistentComponentDescriptionUpdateServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 * @author Tobias Brieden
 */
public class DistributedPersistentComponentDescriptionUpdateServiceImplTest {

    private static final String VERSION_2 = "2";

    private static final String VERSION_1 = "1";

    private static final InstanceNodeSessionId NODE_ID_WITH_UPDATE = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("with update");

    private LogicalNodeId localLogicalNodeId = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();

    private PersistentComponentDescription updatedComponentDescription = EasyMock.createNiceMock(PersistentComponentDescription.class);

    /**
     * Test.
     * 
     * @throws IOException on error.
     */
    @Test
    public void test() throws IOException {

        DistributedPersistentComponentDescriptionUpdateServiceImpl updaterService =
            new DistributedPersistentComponentDescriptionUpdateServiceImpl();
        updaterService.bindCommunicationService(new TestCommunicationService());
        updaterService.bindDistributedComponentKnowledgeService(new TestComponentKnowledgeService());
        PlatformService platformServiceMock = EasyMock.createNiceMock(PlatformService.class);
        EasyMock.expect(platformServiceMock.getLocalDefaultLogicalNodeId()).andReturn(localLogicalNodeId).anyTimes();
        EasyMock.replay(platformServiceMock);
        updaterService.bindPlatformService(platformServiceMock);

        List<PersistentComponentDescription> descriptions = new ArrayList<PersistentComponentDescription>();
        PersistentComponentDescription descriptionWithoutUpdate = createLocalComponentDescriptionWithoutUpdate();
        descriptions.add(descriptionWithoutUpdate);

        assertEquals(PersistentDescriptionFormatVersion.NONE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, false));

        PersistentComponentDescription descriptionWithUpdate = createRemoteComponentDescriptionWithUpdate();
        descriptions.add(descriptionWithUpdate);

        assertEquals(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE,
            updaterService.getFormatVersionsAffectedByUpdate(descriptions, false));

        List<PersistentComponentDescription> udpatedDescriptions = updaterService
            .performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE, descriptions, false);

        assertEquals(2, udpatedDescriptions.size());
        assertTrue(udpatedDescriptions.contains(updatedComponentDescription));
        assertTrue(udpatedDescriptions.contains(descriptionWithoutUpdate));
        assertFalse(udpatedDescriptions.contains(descriptionWithUpdate));
    }

    /**
     * Implementation of the {@link DistributedComponentKnowledgeService} for test purposes.
     *
     * @author Tobias Brieden
     */
    class TestComponentKnowledgeService extends DistributedComponentKnowledgeServiceDefaultStub {

        @Override
        public DistributedComponentKnowledge getCurrentSnapshot() {
            DistributedComponentKnowledge mock = EasyMock.createNiceMock(DistributedComponentKnowledge.class);
            List<ComponentInstallation> componentInstallations = new LinkedList<>();
            componentInstallations.add(ComponentInstallationMockFactory.createComponentInstallationMock("comp_with_update", VERSION_2,
                NODE_ID_WITH_UPDATE.convertToDefaultLogicalNodeId()));
            componentInstallations.add(ComponentInstallationMockFactory.createComponentInstallationMock("comp_without_update", VERSION_1,
                localLogicalNodeId.convertToDefaultLogicalNodeId()));
            EasyMock.expect(mock.getAllInstallations())
                .andReturn(ComponentTestUtils.convertToListOfDistributedComponentEntries(componentInstallations))
                .anyTimes();
            EasyMock.replay(mock);

            return mock;
        }
    }

    /**
     * Implementation of CommunicationService for test purposes.
     * 
     * @author Doreen Seider
     */
    class TestCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        public <T> T getRemotableService(Class<T> iface, NetworkDestination dest) {
            ResolvableNodeId nodeId = (ResolvableNodeId) dest;
            if (nodeId.isSameInstanceNodeAs(localLogicalNodeId)) {
                return (T) new LocalComponentDescriptionUpdateService();
            } else if (nodeId.isSameInstanceNodeAs(NODE_ID_WITH_UPDATE)) {
                return (T) new RemoteComponentDescriptionUpdateService();
            }
            return null;
        }
    }

    /**
     * Dummy implementation of {@link PersistentComponentDescriptionUpdater}.
     * 
     * @author Doreen Seider
     */
    class LocalComponentDescriptionUpdateService implements RemotablePersistentComponentDescriptionUpdateService {

        @Override
        public int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, Boolean silent) {
            return PersistentDescriptionFormatVersion.NONE;
        }

        @Override
        public List<PersistentComponentDescription> performComponentDescriptionUpdates(Integer formatVersion,
            List<PersistentComponentDescription> descriptions, Boolean silent) throws IOException {
            throw new IllegalStateException();
        }

    }

    /**
     * Dummy implementation of {@link PersistentComponentDescriptionUpdater}.
     * 
     * @author Doreen Seider
     */
    class RemoteComponentDescriptionUpdateService implements RemotablePersistentComponentDescriptionUpdateService {

        @Override
        public int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, Boolean silent) {
            return PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
        }

        @Override
        public List<PersistentComponentDescription> performComponentDescriptionUpdates(Integer formatVersion,
            List<PersistentComponentDescription> descriptions, Boolean silent) throws IOException {
            List<PersistentComponentDescription> descs = new ArrayList<PersistentComponentDescription>();
            descs.add(updatedComponentDescription);
            return descs;
        }

    }

    private PersistentComponentDescription createRemoteComponentDescriptionWithUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentNodeIdentifier()).andReturn(NODE_ID_WITH_UPDATE.convertToDefaultLogicalNodeId()).anyTimes();
        EasyMock.expect(description.getComponentIdentifier()).andReturn("comp_with_update").anyTimes();
        EasyMock.expect(description.getComponentVersion()).andReturn(VERSION_1).anyTimes();
        EasyMock.replay(description);
        return description;
    }

    private PersistentComponentDescription createLocalComponentDescriptionWithoutUpdate() {
        PersistentComponentDescription description = EasyMock.createNiceMock(PersistentComponentDescription.class);
        EasyMock.expect(description.getComponentNodeIdentifier()).andReturn(null).anyTimes();
        EasyMock.expect(description.getComponentIdentifier()).andReturn("comp_without_update").anyTimes();
        EasyMock.expect(description.getComponentVersion()).andReturn(VERSION_1).anyTimes();
        EasyMock.replay(description);
        return description;
    }

    /**
     * Tests if the node identifier are set correctly.
     * 
     * TODO add more test cases with different component versions etc.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void testCheckAndSetNodeIdentifier() throws IdentifierException {

        /*---------------------------------------------------------------------
        | 1. Test
        *-------------------------------------------------------------------*/
        // setup

        final String componentId = "de.rce.comp-A";
        final String version = "1.1";
        final String componentIdPlusVersion = componentId + ComponentConstants.ID_SEPARATOR + version;

        final LogicalNodeId localDefaultLogicalNodeId = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        final LogicalNodeId nodeId1 = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        final LogicalNodeId nodeId2 = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        final LogicalNodeId nodeId3 = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();

        DistributedPersistentComponentDescriptionUpdateServiceImpl updateService =
            new DistributedPersistentComponentDescriptionUpdateServiceImpl();
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
        EasyMock.replay(persCompDesc);

        Collection<ComponentInstallation> compInstalls = new ArrayList<>();
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId2));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId1));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version,
            localDefaultLogicalNodeId));

        // execution
        LogicalNodeId targetNode = updateService.getTargetNodeForUpdate(persCompDesc,
            ComponentTestUtils.convertToListOfDistributedComponentEntries(compInstalls));
        // assertions
        assertEquals(localDefaultLogicalNodeId, targetNode);

        /*---------------------------------------------------------------------
        | 2. Test
        *-------------------------------------------------------------------*/
        // setup
        compInstalls = new ArrayList<>();
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId2));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId1));
        // execution
        targetNode = updateService.getTargetNodeForUpdate(persCompDesc,
            ComponentTestUtils.convertToListOfDistributedComponentEntries(compInstalls));
        // assertions
        assertEquals(nodeId1, targetNode);

        /*---------------------------------------------------------------------
        | 3. Test
        *-------------------------------------------------------------------*/
        // setup
        compInstalls = new ArrayList<>();
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId2));
        compInstalls.add(ComponentInstallationMockFactory.createComponentInstallationMock(componentIdPlusVersion, version, nodeId3));
        // execution
        targetNode = updateService.getTargetNodeForUpdate(persCompDesc,
            ComponentTestUtils.convertToListOfDistributedComponentEntries(compInstalls));
        // assertions
        assertTrue(targetNode.equals(nodeId2));

        /*---------------------------------------------------------------------
        | 4. Test
        *-------------------------------------------------------------------*/
        // setup and execution
        targetNode = updateService.getTargetNodeForUpdate(persCompDesc, new ArrayList<DistributedComponentEntry>());
        // assertions
        assertEquals(localDefaultLogicalNodeId, targetNode);

    }

}
