/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.ComponentInstallationMockFactory;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentDescriptionFactoryService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.testutils.ComponentDescriptionFactoryServiceDefaultStub;
import de.rcenvironment.core.component.testutils.ComponentTestUtils;
import de.rcenvironment.core.component.testutils.DistributedComponentKnowledgeServiceDefaultStub;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test cases for {@link WorkflowDescriptionPersistenceHandler}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowDescriptionPersistenceHandlerTest {

    private static final String COMP_VERSION_INPUT_PROVIDER = "3.2";

    private static final String COMP_ID_INPUT_PROVIDER =
        "de.rcenvironment.inputprovider" + ComponentConstants.ID_SEPARATOR + COMP_VERSION_INPUT_PROVIDER;

    private static final String COMP_VERSION_SCRIPT = "3.4";

    private static final String COMP_ID_SCRIPT = "de.rcenvironment.script" + ComponentConstants.ID_SEPARATOR + COMP_VERSION_SCRIPT;

    private static final String COMP_VERSION_OUTPUT_WRITER = "2.0";

    private static final String COMP_ID_OUTPUT_WRITER =
        "de.rcenvironment.outputwriter" + ComponentConstants.ID_SEPARATOR + COMP_VERSION_OUTPUT_WRITER;

    private static final String LOCAL_INSTANCE_ID = "c8061f66333342c9a393c2184c75454f";

    private static final String LOCAL_LOGICAL_NODE_ID = LOCAL_INSTANCE_ID + ":0";

    private static final String REMOTE_INSTANCE_ID = "5f323616fcc4440d852074f737a9f297";

    private static final String REMOTE_LOGICAL_NODE_ID = REMOTE_INSTANCE_ID + ":0";

    private static final int TEST_TIMEOUT_MSEC = 2000;

    /**
     * Tests if a basic workflow is read properly by {@link WorkflowDescriptionPersistenceHandler}. It doesn't check each detail, but tries
     * to cover most parts of the workflow.
     * 
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testValidBasicWorkflow() throws IOException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler =
            WorkflowDescriptionPersistenceHandlerTestUtils.createWorkflowDescriptionPersistenceHandlerTestInstance();

        handler.bindPlatformService(createPlatformServiceMock());

        Map<ComponentInstallation, ComponentDescription> compInstToCompDescMapping = createCompInstToCompDescMapping();
        Map<String, ComponentInstallation> wfNodeIdToCompInstMapping = createWfNodeIdToCompInstMapping(compInstToCompDescMapping.keySet());

        handler.bindDistributedComponentKnowledgeService(createComponentKnowledgeServiceMock(compInstToCompDescMapping));
        handler.bindComponentDescriptionFactoryService(createComponentDescriptionFactoryServiceMock(compInstToCompDescMapping));

        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getTestFileStream("Basic_test_workflow.wf"));
        assertEquals("aebf6bb8-ad05-4b2e-a698-a6705f6dc0cf", workflowDescription.getIdentifier());
        assertEquals(4, workflowDescription.getWorkflowVersion());

        assertWorkflowNodes(workflowDescription, wfNodeIdToCompInstMapping);
        assertConnections(workflowDescription);
        assertBendpoints(workflowDescription);
        assertWorkflowLabels(workflowDescription);
    }

    /**
     * Test if loading a workflow file and saving it again produces the same file.
     * 
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     * @throws SecurityException on failure setting the encoding
     * @throws NoSuchFieldException on failure setting the encoding
     * @throws IllegalAccessException on failure setting the encoding
     * @throws IllegalArgumentException on failure setting the encoding
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testWritingWorkflow() throws IOException, WorkflowFileException, NoSuchFieldException, SecurityException,
        IllegalArgumentException, IllegalAccessException {

        setFileEncoding("Cp1252");
        String filename = "Labels_old_style.wf";
        TempFileServiceAccess.setupUnitTestEnvironment();

        File tempFileOrig = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("Labels_orig.wf");
        FileUtils.copyInputStreamToFile(getTestFileStream(filename), tempFileOrig);

        WorkflowDescriptionPersistenceHandler handler =
            WorkflowDescriptionPersistenceHandlerTestUtils.createWorkflowDescriptionPersistenceHandlerTestInstance();
        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getTestFileStream(filename));
        ByteArrayOutputStream workflowStream = handler.writeWorkflowDescriptionToStream(workflowDescription);
        File tempFileTest = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("Labels_test.wf");
        try (OutputStream outputStream = new FileOutputStream(tempFileTest)) {
            workflowStream.writeTo(outputStream);
        }

        setFileEncoding("UTF-8");
        Assert.assertTrue(FileUtils.contentEqualsIgnoreEOL(tempFileOrig, tempFileTest, "Cp1252"));

        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFileOrig);
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFileTest);
    }

    private static void setFileEncoding(String encoding) throws NoSuchFieldException, IllegalAccessException {
        System.setProperty("file.encoding", encoding);
        Field cs = Charset.class.getDeclaredField("defaultCharset");
        cs.setAccessible(true);
        cs.set(null, null);
    }

    private void assertWorkflowNodes(WorkflowDescription wd, Map<String, ComponentInstallation> wfNodeIdToCompInstMapping) {
        assertEquals(3, wd.getWorkflowNodes().size());
        for (WorkflowNode wfNode : wd.getWorkflowNodes()) {
            assertTrue(wfNode.getComponentDescription().getComponentInstallation().getComponentInterface()
                .getIdentifierAndVersion().equals(wfNodeIdToCompInstMapping.get(wfNode.getIdentifier())
                    .getComponentInterface().getIdentifierAndVersion()));
            final String logicalNodeIdStringOfComponent =
                wfNode.getComponentDescription().getComponentInstallation().getNodeId();
            assertEquals(logicalNodeIdStringOfComponent,
                wfNodeIdToCompInstMapping.get(wfNode.getIdentifier()).getNodeId());
        }
        // spot check
        WorkflowNode ouputWritterWfNode = wd.getWorkflowNode("361ff68d-04c7-4ef1-a1b7-aba1292b339d");
        final int x = 571;
        final int y = 265;
        assertEquals(x, ouputWritterWfNode.getX());
        assertEquals(y, ouputWritterWfNode.getY());
        assertEquals("Combiner", ouputWritterWfNode.getName());
    }

    private void assertConnections(WorkflowDescription wd) {
        assertEquals(2, wd.getConnections().size());
        for (Connection cn : wd.getConnections()) {
            assertTrue(cn.getSourceNode().getName().equals("Floater") || cn.getSourceNode().getName().equals("Texter"));
            assertTrue(cn.getTargetNode().getName().equals("Combiner"));
            // spot check
            if (cn.getSourceNode().getName().equals("Texter")) {
                assertEquals("e292472a-2b5e-4007-ab6c-2d2bd784d6f6", cn.getOutput().getIdentifier());
                assertEquals("text_outp", cn.getOutput().getName());
                assertEquals(DataType.ShortText, cn.getOutput().getDataType());
                assertEquals("d4d41dc2-5586-423a-9f2e-cc0bd7f00d24", cn.getInput().getIdentifier());
                assertEquals("text_inp", cn.getInput().getName());
                assertEquals(DataType.ShortText, cn.getInput().getDataType());
                assertEquals("default", cn.getInput().getDynamicEndpointIdentifier());
                assertEquals("fcbbe1be-1fc2-45fc-9718-0c64d76c7bf8", cn.getInput().getParentGroupName());
            }
        }
    }

    private void assertBendpoints(WorkflowDescription wd) {
        for (Connection cn : wd.getConnections()) {
            // spot check
            if (cn.getSourceNode().getName().equals("Floater")) {
                assertEquals(1, cn.getBendpoints().size());
                final int x = 590;
                assertEquals(x, cn.getBendpoints().get(0).x);
                final int y = 177;
                assertEquals(y, cn.getBendpoints().get(0).y);
            }
        }
    }

    private void assertWorkflowLabels(WorkflowDescription wd) {
        assertEquals(1, wd.getWorkflowLabels().size());
        // spot check
        WorkflowLabel wfLabel = wd.getWorkflowLabels().get(0);
        assertEquals("2a16256d-4b49-421b-8bb0-9244008a7592", wfLabel.getIdentifier());
        assertEquals("Workflow that covers a broad range of possible content in a workflow file.", wfLabel.getText());
        final int height = 68;
        assertEquals(height, wfLabel.getHeight());
        final int width = 665;
        assertEquals(width, wfLabel.getWidth());
        final int x = 50;
        assertEquals(x, wfLabel.getX());
        final int y = 25;
        assertEquals(y, wfLabel.getY());
        final int textSize = 11;
        assertEquals(textSize, wfLabel.getTextSize());
    }

    private PlatformService createPlatformServiceMock() {
        PlatformService platformService = EasyMock.createNiceMock(PlatformService.class);
        final InstanceNodeSessionId instanceSessionId = NodeIdentifierTestUtils.createTestInstanceNodeSessionId(LOCAL_INSTANCE_ID);
        final LogicalNodeId defaultLogicalNodeId = instanceSessionId.convertToDefaultLogicalNodeId();
        EasyMock.expect(platformService.getLocalInstanceNodeSessionId())
            .andReturn(instanceSessionId)
            .anyTimes();
        EasyMock.expect(platformService.getLocalDefaultLogicalNodeId())
            .andReturn(defaultLogicalNodeId)
            .anyTimes();
        EasyMock.replay(platformService);
        return platformService;
    }

    private DistributedComponentKnowledgeService createComponentKnowledgeServiceMock(
        Map<ComponentInstallation, ComponentDescription> compInstToCompDescMapping) {
        DistributedComponentKnowledgeService compKnowledgeService = EasyMock.createStrictMock(DistributedComponentKnowledgeService.class);
        EasyMock.expect(compKnowledgeService.getCurrentSnapshot())
            .andStubReturn(createComponentKnowledgeMock(compInstToCompDescMapping));
        EasyMock.replay(compKnowledgeService);
        return compKnowledgeService;
    }

    private DistributedComponentKnowledge createComponentKnowledgeMock(
        Map<ComponentInstallation, ComponentDescription> compInstToCompDescMapping) {
        DistributedComponentKnowledge compKnowledge = EasyMock.createStrictMock(DistributedComponentKnowledge.class);
        EasyMock.expect(compKnowledge.getAllInstallations()).andStubReturn(
            ComponentTestUtils.convertToListOfDistributedComponentEntries(compInstToCompDescMapping.keySet()));
        EasyMock.replay(compKnowledge);
        return compKnowledge;
    }

    private Map<ComponentInstallation, ComponentDescription> createCompInstToCompDescMapping() {
        Map<ComponentInstallation, ComponentDescription> compInstToCompDescMapping = new HashMap<>();
        for (ComponentInstallation compInst : createComponentInstallationMocks()) {
            ConfigurationDescription confDesc = EasyMock.createNiceMock(ConfigurationDescription.class);
            EasyMock.replay(confDesc);
            EndpointDescriptionsManager inpDescManager = EasyMock.createNiceMock(EndpointDescriptionsManager.class);
            EasyMock.replay(inpDescManager);
            EndpointDescriptionsManager outpDescManager = EasyMock.createNiceMock(EndpointDescriptionsManager.class);
            EasyMock.replay(outpDescManager);
            ComponentDescription compDesc = EasyMock.createNiceMock(ComponentDescription.class);
            EasyMock.expect(compDesc.getComponentInstallation()).andStubReturn(compInst);
            EasyMock.expect(compDesc.getIdentifier())
                .andStubReturn(compInst.getComponentInterface().getIdentifierAndVersion());
            EasyMock.expect(compDesc.getConfigurationDescription()).andStubReturn(confDesc);
            EasyMock.expect(compDesc.getInputDescriptionsManager()).andStubReturn(inpDescManager);
            EasyMock.expect(compDesc.getOutputDescriptionsManager()).andStubReturn(outpDescManager);
            EasyMock.replay(compDesc);
            compInstToCompDescMapping.put(compInst, compDesc);
        }
        return compInstToCompDescMapping;
    }

    private Map<String, ComponentInstallation> createWfNodeIdToCompInstMapping(Set<ComponentInstallation> compInstallations) {
        Map<String, ComponentInstallation> wfNodeIdToCompInstMapping = new HashMap<>();
        for (ComponentInstallation compInst : compInstallations) {
            if (compInst.getComponentInterface().getIdentifierAndVersion().equals(COMP_ID_INPUT_PROVIDER)
                && (compInst.getNodeId() == null || isComponentInstallationLocatedOnInstance(compInst, LOCAL_INSTANCE_ID))) {
                wfNodeIdToCompInstMapping.put("fbfcc614-b0e9-4a97-81db-26dd40964e15", compInst);
            } else if (compInst.getComponentInterface().getIdentifierAndVersion().equals(COMP_ID_SCRIPT)
                && isComponentInstallationLocatedOnInstance(compInst, REMOTE_INSTANCE_ID)) {
                wfNodeIdToCompInstMapping.put("c5aa840d-68a8-456f-943b-569a1875933d", compInst);
            } else if (compInst.getComponentInterface().getIdentifierAndVersion().equals(COMP_ID_OUTPUT_WRITER)
                && (compInst.getNodeId() == null || isComponentInstallationLocatedOnInstance(compInst, LOCAL_INSTANCE_ID))) {
                wfNodeIdToCompInstMapping.put("361ff68d-04c7-4ef1-a1b7-aba1292b339d", compInst);
            }
        }
        return wfNodeIdToCompInstMapping;
    }

    private Collection<ComponentInstallation> createComponentInstallationMocks() {
        Set<ComponentInstallation> compInstallations = new HashSet<>();
        compInstallations.add(ComponentInstallationMockFactory.createComponentInstallationMock(
            COMP_ID_INPUT_PROVIDER, COMP_VERSION_INPUT_PROVIDER, LOCAL_LOGICAL_NODE_ID));
        compInstallations.add(ComponentInstallationMockFactory.createComponentInstallationMock(
            COMP_ID_SCRIPT, COMP_VERSION_SCRIPT, LOCAL_LOGICAL_NODE_ID));
        compInstallations.add(ComponentInstallationMockFactory.createComponentInstallationMock(
            COMP_ID_SCRIPT, COMP_VERSION_SCRIPT, REMOTE_LOGICAL_NODE_ID));
        compInstallations.add(ComponentInstallationMockFactory.createComponentInstallationMock(
            COMP_ID_OUTPUT_WRITER, COMP_VERSION_OUTPUT_WRITER, LOCAL_LOGICAL_NODE_ID));
        compInstallations.add(ComponentInstallationMockFactory.createComponentInstallationMock(
            COMP_ID_OUTPUT_WRITER, COMP_VERSION_OUTPUT_WRITER, REMOTE_LOGICAL_NODE_ID));
        return compInstallations;
    }

    private ComponentDescriptionFactoryService createComponentDescriptionFactoryServiceMock(
        Map<ComponentInstallation, ComponentDescription> compInstToCompDescMapping) {
        ComponentDescriptionFactoryService compKnowledgeService = EasyMock.createStrictMock(ComponentDescriptionFactoryService.class);
        for (ComponentInstallation compInst : compInstToCompDescMapping.keySet()) {
            EasyMock.expect(compKnowledgeService.createComponentDescription(compInst))
                .andStubReturn(compInstToCompDescMapping.get(compInst));
        }
        EasyMock.replay(compKnowledgeService);
        return compKnowledgeService;
    }

    /**
     * Tests if {@link WorkflowDescriptionPersistenceHandler} can handle json arrays on top level, which key is not known.
     * 
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testIfUnknownJsonArrayIsHandledProperly() throws IOException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler =
            WorkflowDescriptionPersistenceHandlerTestUtils.createWorkflowDescriptionPersistenceHandlerTestInstance();
        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getTestFileStream("UnknownJsonArray.wf"));
        assertEquals("03c4b8e3-7238-43f3-8992-2c3956b737a9", workflowDescription.getIdentifier());
        assertEquals(1, workflowDescription.getWorkflowVersion());
    }

    /**
     * Tests if {@link WorkflowDescriptionPersistenceHandler} can parse {@link WorkflowLabel}s in both ways supported (json array as one
     * single string and as normal node).
     * 
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testParseLabelsEitherStyle() throws IOException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler =
            WorkflowDescriptionPersistenceHandlerTestUtils.createWorkflowDescriptionPersistenceHandlerTestInstance();
        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getTestFileStream("Labels_old_style.wf"));
        assertLabelsParsedEitheStyle(workflowDescription);
        workflowDescription = handler.readWorkflowDescriptionFromStream(getTestFileStream("Labels_new_style.wf"));
        assertLabelsParsedEitheStyle(workflowDescription);
    }

    private void assertLabelsParsedEitheStyle(WorkflowDescription workflowDescription) {
        assertEquals("697261b6-eaf5-44ab-af40-6c161a4f26f8", workflowDescription.getIdentifier());
        assertEquals(4, workflowDescription.getWorkflowVersion());
        assertEquals(1, workflowDescription.getWorkflowLabels().size());
        assertEquals("A label with umlauts: öäüÖÄÜß", workflowDescription.getWorkflowLabels().get(0).getText());
        assertEquals(7, workflowDescription.getWorkflowLabels().get(0).getTextSize());
    }

    /**
     * Tests if {@link WorkflowDescriptionPersistenceHandler} can parse bendpoints in both ways supported (json array as one single string
     * and as normal node).
     * 
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testParseBendpointsEitherStyle() throws IOException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler =
            WorkflowDescriptionPersistenceHandlerTestUtils.createWorkflowDescriptionPersistenceHandlerTestInstance();
        handler.bindDistributedComponentKnowledgeService(new DistributedComponentKnowledgeServiceDefaultStub());
        handler.bindComponentDescriptionFactoryService(new ComponentDescriptionFactoryServiceDefaultStub());
        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getTestFileStream("Bendpoints_old_style.wf"));
        assertBendpointsParsedEitherStyle(workflowDescription);
        workflowDescription = handler.readWorkflowDescriptionFromStream(getTestFileStream("Bendpoints_new_style.wf"));
        assertBendpointsParsedEitherStyle(workflowDescription);
    }

    private void assertBendpointsParsedEitherStyle(WorkflowDescription workflowDescription) {
        assertEquals("a5018ce0-bfdd-4704-a1a7-32d8a3a739ad", workflowDescription.getIdentifier());
        assertEquals(4, workflowDescription.getWorkflowVersion());
        for (Connection connection : workflowDescription.getConnections()) {
            if (connection.getSourceNode().getIdentifier().equals("d31c12aa-faab-4ed1-89ae-08eaa8c9a9af")) {
                assertEquals(1, connection.getBendpoints().size());
                final int x = 352;
                final int y = 41;
                assertEquals(x, connection.getBendpoint(0).x);
                assertEquals(y, connection.getBendpoint(0).y);
            }
        }
    }

    /**
     * Regression test for issue #0013377 that causes/caused the parser to hang on specific input.
     * 
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void specificRegressionTest() throws IOException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler =
            WorkflowDescriptionPersistenceHandlerTestUtils.createWorkflowDescriptionPersistenceHandlerTestInstance();
        try {
            handler.readWorkflowDescriptionFromStream(getTestFileStream("Bug_Demo.wf"));
        } catch (WorkflowFileException e) {
            WorkflowDescription workflowDescription = e.getParsedWorkflowDescription();
            assertNotNull(workflowDescription);
            assertEquals("697261b6-eaf5-44ab-af0f35-9445-40a3-bbe1-84b36cc6ab2f", workflowDescription.getIdentifier());
        }
    }

    private InputStream getTestFileStream(String filename) {
        return getClass().getResourceAsStream("/workflows_unit_test/" + filename);
    }

    private boolean isComponentInstallationLocatedOnInstance(ComponentInstallation compInst, String instanceId) {
        // encapsulate comparison until component installations use id objects natively
        return compInst.getNodeIdObject().getInstanceNodeIdString().equals(instanceId);
    }
}
