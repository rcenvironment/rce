/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import de.rcenvironment.core.component.integration.documentation.RemoteToolIntegrationDocumentationService;
import de.rcenvironment.core.component.integration.documentation.internal.ToolIntegrationDocumentationServiceImpl;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;
import de.rcenvironment.core.component.testutils.ComponentTestUtils;
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
        Map<String, String> toolIDsAndHashes = new HashMap<>();
        toolIDsAndHashes.put(TOOL_IDENTIFIER_1, HASH_1);
        toolIDsAndHashes.put(TOOL_IDENTIFIER_2, HASH_2);

        RemoteToolIntegrationDocumentationService rtis = createRemoteServiceMock(returnArray, toolIDsAndHashes);

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

    private RemoteToolIntegrationDocumentationService createRemoteServiceMock(byte[] returnArray, Map<String, String> toolIDsAndHashes) {
        RemoteToolIntegrationDocumentationService rtis = EasyMock.createStrictMock(RemoteToolIntegrationDocumentationService.class);
        for (String tool : toolIDsAndHashes.keySet()) {
            try {
                EasyMock.expect(rtis.loadToolDocumentation(tool, LOCAL_NODE_ID.getLogicalNodeIdString(), toolIDsAndHashes.get(tool)))
                    .andReturn(returnArray);
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

    // TODO replace this with a central mock/stub implementation?
    private ComponentInstallation createComponentInstallation(String installationID, String hash, String nodeID) {

        ComponentInterface cint = EasyMock.createNiceMock(ComponentInterface.class);
        EasyMock.expect(cint.getDocumentationHash()).andReturn(hash).anyTimes();
        EasyMock.expect(cint.getIdentifierAndVersion()).andReturn(installationID).anyTimes();
        EasyMock.replay(cint);

        ComponentRevision cr = EasyMock.createNiceMock(ComponentRevision.class);
        EasyMock.expect(cr.getComponentInterface()).andReturn(cint).anyTimes();
        EasyMock.replay(cr);

        ComponentInstallation ci = EasyMock.createNiceMock(ComponentInstallation.class);
        EasyMock.expect(ci.getInstallationId()).andReturn(installationID).anyTimes();
        EasyMock.expect(ci.getComponentRevision()).andReturn(cr).anyTimes();
        EasyMock.expect(ci.getComponentInterface()).andReturn(cint).anyTimes();
        EasyMock.expect(ci.getNodeId()).andReturn(nodeID).anyTimes();
        EasyMock.replay(ci);

        return ci;
    }

    private CommunicationService createRemoteServiceWithReturningByteArray(LogicalNodeId nodeId,
        RemoteToolIntegrationDocumentationService rtis) {

        CommunicationService commService = EasyMock.createNiceMock(CommunicationService.class);
        EasyMock.expect(
            commService.getRemotableService(RemoteToolIntegrationDocumentationService.class, nodeId))
            .andReturn(rtis).anyTimes();
        EasyMock.replay(commService);
        return commService;
    }

    private ConfigurationService createConfigService(File tempDirPath) {
        ConfigurationService configService = EasyMock.createNiceMock(ConfigurationService.class);
        EasyMock.expect(configService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA)).andStubReturn(tempDirPath);
        EasyMock.replay(configService);
        return configService;
    }

    // TODO replace this with a central mock/stub implementation?
    private DistributedComponentKnowledgeService createMockedKnowledgeService(Set<ComponentInstallation> ciSet) {
        DistributedComponentKnowledge dck = EasyMock.createNiceMock(DistributedComponentKnowledge.class);
        List<DistributedComponentEntry> dceSet = ComponentTestUtils.convertToListOfDistributedComponentEntries(ciSet);
        EasyMock.expect(dck.getAllInstallations()).andReturn(dceSet).anyTimes();
        EasyMock.replay(dck);
        DistributedComponentKnowledgeService dcks = EasyMock.createNiceMock(DistributedComponentKnowledgeService.class);
        EasyMock.expect(dcks.getCurrentSnapshot()).andReturn(dck).anyTimes();
        EasyMock.replay(dcks);
        return dcks;
    }
}
