/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.Serializable;

import de.rcenvironment.core.component.api.StringIdentifier;

/**
 * This class should be used instead of the currently used String identifier to have type guarantees.
 *
 * @author Tobias Brieden
 */
public class WorkflowNodeIdentifier extends StringIdentifier implements Serializable {

    private static final long serialVersionUID = 5697100478249469584L;

    public WorkflowNodeIdentifier(String identifier) {
        super(identifier);
    }

}
