/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.workflow.execution.api.GenericSubscriptionEventProcessor;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Subscriber for all input notifications in the overall system.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class InputSubscriptionEventProcessor extends GenericSubscriptionEventProcessor {

    private static final long serialVersionUID = 2685613452747737482L;

    private final transient InputModel inputModel;
    
    private final transient EndpointDatumSerializer endpointDatumSerializer;

    public InputSubscriptionEventProcessor(InputModel consoleModel) {
        super();
        this.inputModel = consoleModel;
        this.endpointDatumSerializer = ServiceRegistry.createAccessFor(this).getService(EndpointDatumSerializer.class);
    }

    /**
     * Process all collected inputs and perform a single GUI update to improve performance.
     */
    @Override
    protected synchronized void processCollectedNotifications(List<Notification> notifications) {

        // process the list outside the synchronization block
        List<EndpointDatum> inputs = new ArrayList<>();
        for (Notification notification : notifications) {
            Serializable body = notification.getBody();
            if (body instanceof String) {
                inputs.add(endpointDatumSerializer.deserializeEndpointDatum((String) body));
            }
        }
        inputModel.addInputs(inputs);
    }
}
