/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;

import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.component.testutils.DistributedComponentKnowledgeServiceDefaultStub;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;

/**
 * Test cases for {@link WorkflowDescriptionPersistenceHandlerTest}.
 * 
 * @author Doreen Seider
 */
public class WorkflowDescriptionPersistenceHandlerTest {

    private static final int TEST_TIMEOUT_MSEC = 1000;

    /**
     * Tests if {@link WorkflowDescriptionPersistenceHandler} can handle json arrays on top level, which key is not known.
     * 
     * @throws ParseException on test failure
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testIfUnknownJsonArrayIsHandledProperly() throws IOException, ParseException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler = new WorkflowDescriptionPersistenceHandler();
        handler.bindPlatformService(new PlatformServiceDefaultStub());
        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getClass()
            .getResourceAsStream("/workflows_unit_test/UnknownJsonArray.wf"));
        assertEquals("03c4b8e3-7238-43f3-8992-2c3956b737a9", workflowDescription.getIdentifier());
        assertEquals(1, workflowDescription.getWorkflowVersion());
    }

    /**
     * Tests if {@link WorkflowDescriptionPersistenceHandler} can parse {@link WorkflowLabel}s in both ways supported (json array as one
     * single string and as normal node).
     * 
     * @throws ParseException on test failure
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testParseLabels() throws IOException, ParseException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler = new WorkflowDescriptionPersistenceHandler();
        handler.bindPlatformService(new PlatformServiceDefaultStub());
        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getClass()
            .getResourceAsStream("/workflows_unit_test/Labels.wf"));
        assertEquals("697261b6-eaf5-44ab-af40-6c161a4f26f8", workflowDescription.getIdentifier());
        assertEquals(4, workflowDescription.getWorkflowVersion());
        assertEquals(2, workflowDescription.getWorkflowLabels().size());
        assertEquals("First label", workflowDescription.getWorkflowLabels().get(0).getText());
        assertEquals("Second label", workflowDescription.getWorkflowLabels().get(1).getText());
    }

    /**
     * Tests if {@link WorkflowDescriptionPersistenceHandler} can parse bendpoints in both ways supported (json array as one single string
     * and as normal node).
     * 
     * @throws ParseException on test failure
     * @throws IOException on test failure
     * @throws WorkflowFileException on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testParseBendpoints() throws IOException, ParseException, WorkflowFileException {
        WorkflowDescriptionPersistenceHandler handler = new WorkflowDescriptionPersistenceHandler();
        handler.bindPlatformService(new PlatformServiceDefaultStub());
        handler.bindDistributedComponentKnowledgeService(new DistributedComponentKnowledgeServiceDefaultStub());
        WorkflowDescription workflowDescription = handler.readWorkflowDescriptionFromStream(getClass()
            .getResourceAsStream("/workflows_unit_test/Bendpoints.wf"));
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
    

}
