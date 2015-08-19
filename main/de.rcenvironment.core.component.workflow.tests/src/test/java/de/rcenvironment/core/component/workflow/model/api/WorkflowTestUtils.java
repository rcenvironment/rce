/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;

import junit.framework.Assert;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Utils class for the workflow tests.
 * 
 * @author Sascha Zur
 * 
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
        InputStream is = WorkflowDescription.class.getResourceAsStream("/workflows_unit_test/DummyUnitTest.wf");
        WorkflowDescription wd = null;
        DummyWorkflowDescriptionPersistenceHandler dwph = new DummyWorkflowDescriptionPersistenceHandler();
        dwph.bindDistributedComponentKnowledgeService(null);
        dwph.bindPlatformService(null);
        try {
            wd = dwph.readWorkflowDescriptionFromStream(is, null);
        } catch (IOException e) {
            Assert.fail();
        } catch (ParseException e) {
            Assert.fail();
        }
        return wd;
    }

    /**
     * Dummy class to prevent the persistence handle calling getallComponentDescriptions.
     * 
     * @author Sascha Zur
     */
    private static final class DummyWorkflowDescriptionPersistenceHandler extends WorkflowDescriptionPersistenceHandler {

        @Override
        protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
            DistributedComponentKnowledge knowledge = EasyMock.createNiceMock(DistributedComponentKnowledge.class);
            EasyMock.expect(knowledge.getAllInstallations()).andReturn(new ArrayList<ComponentInstallation>()).anyTimes();
            EasyMock.replay(knowledge);
            componentKnowledgeService = EasyMock.createNiceMock(DistributedComponentKnowledgeService.class);
            EasyMock.expect(componentKnowledgeService.getCurrentComponentKnowledge()).andReturn(knowledge).anyTimes();
            EasyMock.replay(componentKnowledgeService);

        }
        
        @Override
        protected void bindPlatformService(PlatformService newService) {
            platformService = EasyMock.createNiceMock(PlatformService.class);
            EasyMock.expect(platformService.getLocalNodeId()).andReturn(NodeIdentifierFactory.fromNodeId("local-node-id")).anyTimes();
            EasyMock.replay(platformService);

        }
    }
}
