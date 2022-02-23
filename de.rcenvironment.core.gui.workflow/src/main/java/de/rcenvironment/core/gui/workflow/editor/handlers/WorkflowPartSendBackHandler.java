/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

/**
 * 
 * Handler to move nodes xor labels backwards.
 *
 * @author Jascha Riedel
 */
public class WorkflowPartSendBackHandler extends AbstractWorkflowPartSendHandler{

    @Override
    void edit() {
        send(SendType.SEND_BACK);
    }
    
    
    
}
