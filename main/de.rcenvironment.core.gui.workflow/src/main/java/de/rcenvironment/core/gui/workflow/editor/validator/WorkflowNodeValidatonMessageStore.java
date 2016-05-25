/*
 * Copyright (C) 2006-2016 DLR, Germany
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

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * 
 * {@link WorkflowNodeValidatonMessageStore} stores recent {@link WorkflowNodeValidationMessage} for the different {@link WorkflowNode}s.
 * 
 * @author Marc Stammerjohann
 */
public final class WorkflowNodeValidatonMessageStore {

    private static WorkflowNodeValidatonMessageStore instance = null;

    private Map<String, List<WorkflowNodeValidationMessage>> validatorMap = null;

    private WorkflowNodeValidatonMessageStore() {
        validatorMap = new HashMap<String, List<WorkflowNodeValidationMessage>>();
    }

    /**
     * 
     * Creates or returns an instance of {@link WorkflowNodeValidatonMessageStore}.
     * 
     * @return instance of {@link WorkflowNodeValidatonMessageStore}
     */
    public static synchronized WorkflowNodeValidatonMessageStore getInstance() {
        if (instance == null) {
            instance = new WorkflowNodeValidatonMessageStore();
        }
        return instance;
    }

    /**
     * 
     * Add recent {@link WorkflowNodeValidationMessage}s. {@link WorkflowNodeValidationMessage} is not added, if it can be revalidate on
     * workflow start.
     * 
     * @param identifier of the {@link WorkflowNode}
     * @param validationMessages to be stored
     */
    public void addValidatonMessages(String identifier, List<WorkflowNodeValidationMessage> validationMessages) {
        List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        for (WorkflowNodeValidationMessage message : validationMessages) {
            if (!message.isRevalidateOnWorkflowStart()) {
                messages.add(message);
            }
        }
        validatorMap.put(identifier, messages);
    }

    /**
     * Retrieve all recently stored {@link WorkflowNodeValidationMessage}s for the {@link WorkflowNode} .
     * 
     * @param identifier of the {@link WorkflowNode}
     * @return all stored {@link WorkflowNodeValidationMessage}s
     */
    public List<WorkflowNodeValidationMessage> retrieveValidatonMessages(String identifier) {
        return validatorMap.get(identifier);
    }

    /**
     * 
     * Retrieve all recently stored {@link WorkflowNodeValidationMessage}s, which are not contained in the result list.
     * 
     * @param result list to check, if {@link WorkflowNodeValidationMessage} is contained or not
     * @param identifier of the {@link WorkflowNode}
     * 
     * @return all recently stored {@link WorkflowNodeValidationMessage}s
     */
    public Collection<WorkflowNodeValidationMessage> getRecentMessages(List<WorkflowNodeValidationMessage> result, String identifier) {
        List<WorkflowNodeValidationMessage> allMessages = new LinkedList<WorkflowNodeValidationMessage>();
        if (validatorMap.containsKey(identifier)) {
            for (WorkflowNodeValidationMessage message : validatorMap.get(identifier)) {
                if (!result.contains(message)) {
                    allMessages.add(message);
                }
            }
        }
        return allMessages;
    }

}
