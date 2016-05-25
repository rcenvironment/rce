/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Tests for {@link WorkflowNodeValidatorUtils}.
 *
 * @author Tobias Rodehutskors
 */
public class WorkflowNodeValidatorUtilsTest {

    private static final String WORKFLOW_NODE_NAME = "DNBEMUZIUG";

    /**
     * Tests that the result of the second validator does not override the result of the first validator.
     * 
     * @throws WorkflowFileException e
     * @throws IOException e
     * @throws SecurityException e
     * @throws NoSuchFieldException e
     * @throws IllegalAccessException e
     * @throws IllegalArgumentException e
     */
    @Test
    public void testMultipleValidators() throws IOException, WorkflowFileException, NoSuchFieldException, SecurityException,
        IllegalArgumentException, IllegalAccessException {

        // SETUP

        // create a dummy workflow node ...
        WorkflowNode node = createNiceMock(WorkflowNode.class);
        expect(node.isEnabled()).andReturn(true).anyTimes();
        expect(node.getName()).andReturn(WORKFLOW_NODE_NAME).anyTimes();
        replay(node);

        // and add it to a dummy workflow description
        WorkflowDescription wd = createNiceMock(WorkflowDescription.class);
        List<WorkflowNode> nl = new LinkedList<WorkflowNode>();
        nl.add(node);
        expect(wd.getWorkflowNodes()).andReturn(nl);
        replay(wd);

        // create two validators
        List<WorkflowNodeValidator> validatorList = new LinkedList<WorkflowNodeValidator>();

        // the first validator returns ONE error message
        WorkflowNodeValidator wnv1 = createNiceMock(WorkflowNodeValidator.class);
        List<WorkflowNodeValidationMessage> messageList1 = new LinkedList<WorkflowNodeValidationMessage>();
        messageList1.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, "property1", "relativeMessage1",
            "absoluteMessage1"));
        expect(wnv1.getMessages()).andReturn(messageList1);
        replay(wnv1);
        validatorList.add(wnv1);

        // the second validator returns NO error message
        WorkflowNodeValidator wnv2 = createNiceMock(WorkflowNodeValidator.class);
        expect(wnv2.getMessages()).andReturn(new LinkedList<WorkflowNodeValidationMessage>());
        replay(wnv2);
        validatorList.add(wnv2);

        // create a dummy validator registry
        WorkflowNodeValidatorsRegistry reg = createNiceMock(WorkflowNodeValidatorsRegistry.class);
        expect(reg.getValidatorsForWorkflowNode(node, true)).andReturn(validatorList);
        replay(reg);
        WorkflowNodeValidatorUtils.setRegistry(reg);

        // TEST

        WorkflowNodeValidatorUtils.initializeMessages(wd);
        assertTrue(WorkflowNodeValidatorUtils.hasErrors());
        Map<String, String> errorComponents = WorkflowNodeValidatorUtils.getComponentNames(WorkflowNodeValidationMessage.Type.ERROR);
        assertTrue(errorComponents.keySet().contains(WORKFLOW_NODE_NAME));
        assertEquals(1, errorComponents.keySet().size());

    }
}
