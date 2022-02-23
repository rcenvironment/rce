/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.spi;

/**
 * A type enum for abstract UI actions that are applicable to various kinds of tree nodes.
 * 
 * @author Robert Mischke
 * @author Kathrin Schaffert (#16977 added SHOW_CONFIGURATION_SNIPPET)
 */
public enum StandardUserNodeActionType {
    /**
     * E.g. to start/connect a network connection.
     */
    START,
    /**
     * E.g. to stop/disconnect a network connection.
     */
    STOP,
    /**
     * E.g. to edit a network connection's properties.
     */
    EDIT,
    /**
     * E.g. to delete a network connection configuration.
     */
    DELETE,
    /**
     * E.g. to copy an instance's name or its raw node properties to the clipboard.
     */
    COPY_TO_CLIPBOARD,
    /**
     * E.g. to show a connection configuration snippet to the user
     */
    SHOW_CONFIGURATION_SNIPPET
}
