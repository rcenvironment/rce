/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

/**
 * Interface for refreshable classes in GUI. This Interface is used, when a
 * {@link WorkflowNodeCommand} should update some GUI elements via the refresh method.
 * 
 * 
 * @author Sascha Zur
 */
public interface Refreshable {

    /**
     * 
     */
    void refresh();
}
