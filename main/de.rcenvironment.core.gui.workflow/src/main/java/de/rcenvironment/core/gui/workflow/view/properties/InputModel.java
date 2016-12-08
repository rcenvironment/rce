/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.workflow.execution.api.GenericSubscriptionManager;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Provides central access to input values, processed and pending ones.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (tweaked notification setup)
 */
public final class InputModel {

    private static final int MAX_INPUT_COUNT = 25000;

    private static InputModel instance;

    private static Map<String, Map<String, Map<String, Deque<EndpointDatum>>>> allInputs;

    private static InputSubscriptionEventProcessor eventProcessor;

    private static GenericSubscriptionManager currentInputManager;

    private static CountDownLatch initialSubscriptionLatch;

    private InputModel() {}

    /**
     * Singleton getter to provide central model access.
     * 
     * @return the singleton instance
     */
    public static synchronized InputModel getInstance() {
        if (null == instance) {
            instance = new InputModel();
            initialSubscriptionLatch = new CountDownLatch(1);
            allInputs = new ConcurrentHashMap<String, Map<String, Map<String, Deque<EndpointDatum>>>>();
            eventProcessor = new InputSubscriptionEventProcessor(instance);
            currentInputManager = new GenericSubscriptionManager(eventProcessor,
                ServiceRegistry.createAccessFor(instance).getService(CommunicationService.class),
                ServiceRegistry.createAccessFor(instance).getService(WorkflowHostService.class));
            ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                @Override
                @TaskDescription("Initial inputs subscriptions")
                public void run() {
                    currentInputManager
                        .updateSubscriptionsForPrefixes(new String[] { ComponentConstants.NOTIFICATION_ID_PREFIX_PROCESSED_INPUT });
                    initialSubscriptionLatch.countDown();
                }
            });
        }
        return instance;
    }

    /**
     * Updates subscriptions to known server instances.
     */
    public void updateSubscriptions() {
        try {
            initialSubscriptionLatch.await();
        } catch (InterruptedException e) {
            // TODO better handling?
            throw new RuntimeException("Interrupted while waiting for initial subscriptions to complete", e);
        }
        currentInputManager.updateSubscriptionsForPrefixes(new String[] { ComponentConstants.NOTIFICATION_ID_PREFIX_PROCESSED_INPUT });
    }

    /**
     * Batch version of {@link #addConsoleRow(Input)} to reduce synchronization overhead.
     * 
     * @param inputs the list of {@link EndpointDatum}s to add
     */
    public synchronized void addInputs(List<EndpointDatum> inputs) {
        for (EndpointDatum input : inputs) {
            if (isValue(input)) {
                String workflowId = input.getWorkflowExecutionIdentifier();
                String componentId = input.getInputsComponentExecutionIdentifier();
                String inputName = input.getInputName();
                if (!allInputs.containsKey(workflowId)) {
                    allInputs.put(workflowId, new HashMap<String, Map<String, Deque<EndpointDatum>>>());
                }
                if (!allInputs.get(workflowId).containsKey(componentId)) {
                    allInputs.get(workflowId).put(componentId, new HashMap<String, Deque<EndpointDatum>>());
                }
                if (!allInputs.get(workflowId).get(componentId).containsKey(inputName)) {
                    allInputs.get(workflowId).get(componentId).put(inputName, new LinkedList<EndpointDatum>());
                }
                if (allInputs.size() > MAX_INPUT_COUNT) {
                    allInputs.get(workflowId).get(componentId).get(inputName).removeFirst();
                }
                allInputs.get(workflowId).get(componentId).get(inputName).addLast(input);
            }
        }
    }

    /**
     * Returns a {@link Deque} of {@link EndpointDatum}s for a specified input of a specified component and worklfow. Intended to be called
     * by input view.
     * 
     * @param workflowId identifier of workflow
     * @param componentId identifier of component
     * @param inputName name of input
     * @return {@link Deque} containing inputs
     */
    public synchronized Deque<EndpointDatum> getInputs(String workflowId, String componentId, String inputName) {
        Deque<EndpointDatum> inputs = new LinkedList<>();
        if (allInputs.containsKey(workflowId) && allInputs.get(workflowId).containsKey(componentId)
            && allInputs.get(workflowId).get(componentId).containsKey(inputName)) {
            inputs = new LinkedList<EndpointDatum>(allInputs.get(workflowId).get(componentId).get(inputName));
        }
        return inputs;
    }

    private boolean isValue(EndpointDatum input) {
        return !input.getValue().getDataType().equals(DataType.Internal);
    }

    public InputSubscriptionEventProcessor getEventProcessor() {
        return eventProcessor;
    }

    /**
     * Ensures that the console model is registered to listen for inputs.
     */
    public static void ensureInputCaptureIsInitialized() {
        // trigger model creation & subscription if not done yet
        getInstance();
    }

}
