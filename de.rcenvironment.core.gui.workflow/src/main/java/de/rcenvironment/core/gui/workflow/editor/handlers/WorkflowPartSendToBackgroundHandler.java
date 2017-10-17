/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

/**
 * 
 * Handler to move nodes xor labels backwards.
 *
 * @author Jascha Riedel
 */
public class WorkflowPartSendToBackgroundHandler extends AbstractWorkflowPartSendHandler{

    @Override
    void edit() {
        send(SendType.SEND_TO_BACKGROUND);
    }
}
