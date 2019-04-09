/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.component.api.StringIdentifier;

/**
 * TODO document: what exactly does this identify?
 * 
 * This class should be used instead of the currently used String identifier to have type guarantees.
 *
 * @author Tobias Brieden
 */
public class ComponentExecutionIdentifier extends StringIdentifier implements Serializable {

    private static final long serialVersionUID = -8285405993110905097L;

    public ComponentExecutionIdentifier(String identifier) {
        super(identifier);
    }

}
