/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.easymock.EasyMock;
import org.junit.Assert;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.testutils.ComponentDescriptionFactoryServiceDefaultStub;
import de.rcenvironment.core.component.testutils.DistributedComponentKnowledgeServiceDefaultStub;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;

/**
 * Utils class for the workflow tests.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public final class WorkflowTestUtils {

    /** Constant. */
    public static final String WFID = "Charles M.";

    /** Constant. */
    public static final String KEY = "Key";

    /** Constant. */
    public static final String CD_IDENTIFIER = ComponentUtils.MISSING_COMPONENT_PREFIX + "Dummy";

    /** Constant. */
    public static final String OUTPUT_NAME = "ah";

    /** Constant. */
    public static final String NODE2_NAME = "Noch krassere Component";

    /** Constant. */
    public static final String INPUT_NAME = "86";

    /** Constant. */
    public static final String PROP_MAP_ID = "trallaaa";

    /** Constant. */
    public static final String PLACEHOLDERNAME = "${testPlaceholder}";

    /** Constant. */
    public static final String GLOBAL_PLACEHOLDERNAME = "${global.testPlaceholder2}";

    /** Constant. */
    public static final String ENCRYPTED_PLACEHOLDERNAME = "${*.testPlaceholder3}";

    /**
     * Satisfy checkstyle.
     */
    @Deprecated
    private WorkflowTestUtils() {

    }

    /**
     * Creates a workflow description for testing.
     * 
     * @return dummy wf description
     */
    public static WorkflowDescription createWorkflowDescription() {
        WorkflowDescription wd = null;
        WorkflowDescriptionPersistenceHandlerTestUtils.initializeStaticFieldsOfWorkflowDescriptionPersistenceHandler();
        DummyWorkflowDescriptionPersistenceHandler dwph = new DummyWorkflowDescriptionPersistenceHandler();
        dwph.bindDistributedComponentKnowledgeService(new DistributedComponentKnowledgeServiceDefaultStub());
        dwph.bindComponentDescriptionFactoryService(new ComponentDescriptionFactoryServiceDefaultStub());
        try (InputStream is = WorkflowDescription.class.getResourceAsStream("/workflows_unit_test/DummyUnitTest.wf")) {
            wd = dwph.readWorkflowDescriptionFromStream(is);
        } catch (IOException | WorkflowFileException e) {
            Assert.fail(e.toString());
        }
        return wd;
    }

    /**
     * Dummy class to prevent the persistence handle calling getallComponentDescriptions.
     * 
     * @author Sascha Zur
     * @author Robert Mischke (8.0.0 id adaptations)
     */
    private static final class DummyWorkflowDescriptionPersistenceHandler extends WorkflowDescriptionPersistenceHandler {

        private final InstanceNodeSessionId virtualInstanceId = NodeIdentifierTestUtils
            .createTestInstanceNodeSessionIdWithDisplayName("local-node-id");

        @Override
        protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
            DistributedComponentKnowledge knowledge = EasyMock.createNiceMock(DistributedComponentKnowledge.class);
            EasyMock.expect(knowledge.getAllInstallations()).andReturn(new ArrayList<DistributedComponentEntry>()).anyTimes();
            EasyMock.replay(knowledge);
            componentKnowledgeService = EasyMock.createNiceMock(DistributedComponentKnowledgeService.class);
            EasyMock.expect(componentKnowledgeService.getCurrentSnapshot()).andReturn(knowledge).anyTimes();
            EasyMock.replay(componentKnowledgeService);

        }

        @Override
        protected void bindPlatformService(PlatformService newService) {
            platformService = EasyMock.createNiceMock(PlatformService.class);
            EasyMock.expect(platformService.getLocalInstanceNodeSessionId()).andReturn(virtualInstanceId).anyTimes();
            EasyMock.replay(platformService);

        }
    }
}
