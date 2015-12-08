/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.spi;

/**
 * Interface for tree nodes related to the "copy to clipboard" action.
 * 
 * @author Robert Mischke
 */
public interface StandardUserNodeActionNode {

    /**
     * @param actionType the action type to query
     * @return whether the specified action should be enabled while this node is selected
     */
    boolean isActionApplicable(StandardUserNodeActionType actionType);

    /**
     * Invoked when the "copy to clipboard" action is triggered while this node is selected.
     * 
     * @param actionType the action type to perform
     */
    void performAction(StandardUserNodeActionType actionType);
}
