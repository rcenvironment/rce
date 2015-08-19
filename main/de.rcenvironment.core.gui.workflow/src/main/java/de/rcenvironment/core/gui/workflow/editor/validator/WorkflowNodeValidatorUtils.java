/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage.Type;

/**
 * Utility class with useful methods when working with {@link WorkflowNodeValidator} and {@link WorkflowNodeValidationMessage}.
 * 
 * @author Marc Stammerjohann
 */
public final class WorkflowNodeValidatorUtils {

    private static WorkflowNodeValidatorsRegistry registry = WorkflowNodeValidatorsRegistry.Factory.getInstance();

    private static Map<String, List<WorkflowNodeValidationMessage>> messagesMap =
        new HashMap<String, List<WorkflowNodeValidationMessage>>();

    private static int workflowErrors = 0 - 1;

    private static int workflowWarnings = 0 - 1;

    /**
     * This class has only static methods.
     */
    private WorkflowNodeValidatorUtils() {}

    /**
     * 
     * Returns the amount of errors in the executed workflow. However if the
     * {@link WorkflowNodeValidatorUtils#initializeMessages(WorkflowDescription)} is not called, it will return -1
     * 
     * @return amount of errors or -1, if not initialized
     */
    public static int getWorkflowErrors() {
        return workflowErrors;
    }

    /**
     * 
     * Returns the amount of warnings in the executed workflow. However if the
     * {@link WorkflowNodeValidatorUtils#initializeMessages(WorkflowDescription)} is not called, it will return -1
     * 
     * @return amount of warnings or -1, if not initialized
     */
    public static int getWorkflowWarnings() {
        return workflowWarnings;
    }

    /**
     * @return true, if errors exist
     */
    public static boolean hasErrors() {
        return workflowErrors > 0;
    }

    /**
     * @return true, if warnings exist
     */
    public static boolean hasWarnings() {
        return workflowWarnings > 0;
    }
    
    /**
     * 
     * Calling the {@link AbstractWorkflowNodeValidator#validateOnStart()} and retrieve all {@link WorkflowNodeValidationMessage}s. Provides
     * the {@link WorkflowNodeValidationMessage}s to be count and to retrieve the component names.
     * 
     * @param workflowDescription to retrieve the {@link WorkflowNode} and the available {@link WorkflowNodeValidationMessage}
     */
    public static void initializeMessages(WorkflowDescription workflowDescription) {
        retrieveWorkflowNodeValidationMessages(workflowDescription);
        countMessages();
    }

    private static void countMessages() {
        workflowErrors = 0;
        workflowWarnings = 0;
        Collection<List<WorkflowNodeValidationMessage>> values = messagesMap.values();
        for (Collection<WorkflowNodeValidationMessage> collection : values) {
            for (WorkflowNodeValidationMessage message : collection) {
                if (message.getType().equals(Type.ERROR)) {
                    workflowErrors++;
                } else if (message.getType().equals(Type.WARNING)) {
                    workflowWarnings++;
                }
            }
        }
    }

    /**
     * Retrieve a list of all {@link WorkflowNodeValidationMessage} of the workflow. Ignoring disabled components.
     * 
     * @param workflowDescription to retrieve the {@link WorkflowNode} and the available {@link WorkflowNodeValidationMessage}
     * @return a list of {@link WorkflowNodeValidationMessage}
     */
    private static void retrieveWorkflowNodeValidationMessages(WorkflowDescription workflowDescription) {
        messagesMap.clear();
        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            if (node.isEnabled()) {
                List<WorkflowNodeValidator> validatorsForWorkflowNode = registry.getValidatorsForWorkflowNode(node, true);
                for (WorkflowNodeValidator workflowNodeValidator : validatorsForWorkflowNode) {
                    Collection<WorkflowNodeValidationMessage> validationMessages = workflowNodeValidator.getMessages();
                    messagesMap.put(node.getName(), new LinkedList<WorkflowNodeValidationMessage>(validationMessages));
                }
            }
        }
    }
    /**
     * Retrieve the component names, which have a {@link WorkflowNodeValidationMessage} for the specified type. Ignoring disabled
     * components.
     * 
     * @param type to validate the workflow
     * @return a map
     */
    public static Map<String, String> getComponentNames(Type type) {
        Map<String, String> componentNames = new HashMap<String, String>();
        Set<Entry<String, List<WorkflowNodeValidationMessage>>> entrySet = messagesMap.entrySet();
        for (Entry<String, List<WorkflowNodeValidationMessage>> entry : entrySet) {
            String nodeName = entry.getKey();
            Collection<WorkflowNodeValidationMessage> value = entry.getValue();
            for (WorkflowNodeValidationMessage workflowNodeValidationMessage : value) {
                Type typeNodeValidation = workflowNodeValidationMessage.getType();
                if (typeNodeValidation.equals(type)) {
                    componentNames.put(nodeName, "");
                }
            }
        }
        return componentNames;
    }


}
