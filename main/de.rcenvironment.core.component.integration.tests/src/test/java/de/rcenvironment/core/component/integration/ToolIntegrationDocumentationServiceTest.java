/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationDocumentationServiceImpl;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Tests for {@link ToolIntegrationDocumentationService}.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class ToolIntegrationDocumentationServiceTest {

    private static final LogicalNodeId LOCAL_NODE_ID = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();

    private static final String TOOL_IDENTIFIER_1 = "identifier1";

    private static final String HASH_1 = "hash1";

    private static final String TOOL_IDENTIFIER_2 = "identifier2";

    private static final String HASH_2 = "hash2";

    private File cachedir = null;

    /**
     * Set up the environment.
     */
    @Before
    public void setup() {
        TempFileServiceAccess.setupUnitTestEnvironment();
        try {
            cachedir = TempFileServiceAccess.getInstance().createManagedTempDir();
        } catch (IOException e) {
            Assert.fail("Could not set up test directory");
        }
    }

    /**
     * Test if cache is used correctly.
     */
    @Test
    public void testDocumentationServiceCache() {

        ToolIntegrationDocumentationServiceImpl service = new ToolIntegrationDocumentationServiceImpl();

        Set<ComponentInstallation> ciSet = new HashSet<ComponentInstallation>();
        ciSet.add(createComponentInstallation(TOOL_IDENTIFIER_1, HASH_1, LOCAL_NODE_ID.getLogicalNodeIdString()));
        ciSet.add(createComponentInstallation(TOOL_IDENTIFIER_2, HASH_2, LOCAL_NODE_ID.getLogicalNodeIdString()));

        DistributedComponentKnowledgeService dcks = createMockedKnowledgeService(ciSet);
        service.bindDistributedComponentKnowledgeService(dcks);

        ConfigurationService configService = createConfigService(cachedir);
        service.bindConfigurationService(configService);

        byte[] returnArray = new byte[] { 1, 1, 1, 0, 0, 0 };
        List<String> toolIDs = new ArrayList<>();
        toolIDs.add(TOOL_IDENTIFIER_1);
        toolIDs.add(TOOL_IDENTIFIER_2);

        RemoteToolIntegrationService rtis = createRemoteServiceMock(returnArray, toolIDs);

        CommunicationService commService = createRemoteServiceWithReturningByteArray(LOCAL_NODE_ID, rtis);
        service.bindCommunicationService(commService);

        File directory1;
        try {
            directory1 = service.getToolDocumentation(TOOL_IDENTIFIER_1,
                service.getComponentDocumentationList(TOOL_IDENTIFIER_1).get(HASH_1), HASH_1);

            File directory2 =
                service.getToolDocumentation(TOOL_IDENTIFIER_1, service.getComponentDocumentationList(TOOL_IDENTIFIER_1).get(HASH_1),
                    HASH_1);
            Assert.assertEquals(directory1, directory2);

            File directory3 =
                service.getToolDocumentation(TOOL_IDENTIFIER_2,
                    service.getComponentDocumentationList(TOOL_IDENTIFIER_2).get(HASH_2), HASH_2);
            Assert.assertEquals(false, directory3.equals(directory1));
        } catch (RemoteOperationException | IOException e) {
            Assert.fail();
        }

        EasyMock.verify(rtis);

    }

    private RemoteToolIntegrationService createRemoteServiceMock(byte[] returnArray, List<String> toolIDs) {
        RemoteToolIntegrationService rtis = EasyMock.createStrictMock(RemoteToolIntegrationService.class);
        for (String tool : toolIDs) {
            try {
                EasyMock.expect(rtis.getToolDocumentation(tool)).andReturn(returnArray);
            } catch (RemoteOperationException e) {
                Assert.fail();
            }
        }
        EasyMock.replay(rtis);
        return rtis;
    }

    /**
     * Clean up.
     */
    @After
    public void tearDown() {
        try {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(cachedir);
        } catch (IOException e) {
            Assert.fail();
        }
    }

    private ComponentInstallation createComponentInstallation(String installationID, String hash, String nodeID) {
        ComponentInstallation ci = EasyMock.createNiceMock(ComponentInstallation.class);

        EasyMock.expect(ci.getInstallationId()).andReturn(installationID).anyTimes();

        ComponentInterface cint = EasyMock.createNiceMock(ComponentInterface.class);
        EasyMock.expect(cint.getDocumentationHash()).andReturn(hash).anyTimes();
        EasyMock.replay(cint);
        ComponentRevision cr = EasyMock.createNiceMock(ComponentRevision.class);
        EasyMock.expect(cr.getComponentInterface()).andReturn(cint).anyTimes();
        EasyMock.replay(cr);
        EasyMock.expect(ci.getComponentRevision()).andReturn(cr).anyTimes();
        EasyMock.expect(ci.getNodeId()).andReturn(nodeID).anyTimes();
        EasyMock.replay(ci);

        return ci;
    }

    private CommunicationService createRemoteServiceWithReturningByteArray(LogicalNodeId nodeId, RemoteToolIntegrationService rtis) {

        CommunicationService commService = EasyMock.createNiceMock(CommunicationService.class);
        EasyMock.expect(
            commService.getRemotableService(RemoteToolIntegrationService.class, nodeId))
            .andReturn(rtis).anyTimes();
        EasyMock.replay(commService);
        return commService;
    }

    private ConfigurationService createConfigService(File tempDirPath) {
        ConfigurationService configService = EasyMock.createNiceMock(ConfigurationService.class);
        EasyMock.expect(configService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA)).andReturn(tempDirPath);
        EasyMock.replay(configService);
        return configService;
    }

    private DistributedComponentKnowledgeService createMockedKnowledgeService(Set<ComponentInstallation> ciSet) {
        DistributedComponentKnowledge dck = EasyMock.createNiceMock(DistributedComponentKnowledge.class);
        EasyMock.expect(dck.getAllInstallations()).andReturn(ciSet).anyTimes();
        EasyMock.replay(dck);
        DistributedComponentKnowledgeService dcks = EasyMock.createNiceMock(DistributedComponentKnowledgeService.class);
        EasyMock.expect(dcks.getCurrentComponentKnowledge()).andReturn(dck).anyTimes();
        EasyMock.replay(dcks);
        return dcks;
    }
}
