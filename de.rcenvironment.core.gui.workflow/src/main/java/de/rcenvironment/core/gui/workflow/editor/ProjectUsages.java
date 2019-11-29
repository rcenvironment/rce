/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

/**
 * Enumeration for usages of projects in workflow wizard.
 * 
 * @author Oliver Seebach
 *
 */
public enum ProjectUsages {
    
    /** For creating a new project to associate the workflow with. */
    NEW, 
    /** For associating a new workflow with an existing project. */
    EXISTING
}
