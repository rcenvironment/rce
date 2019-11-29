/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.ui.PlatformUI;



/**
 * Handler to open help view via context menu.
 *
 * @author Oliver Seebach
 */
public class OpenHelpViewHandler extends AbstractWorkflowNodeEditHandler {

    private static final String HELP_VIEW_ID = "org.eclipse.help.ui.HelpView";
    
    @Override
    void edit() {
        PlatformUI.getWorkbench().getHelpSystem()
            .displayDynamicHelp();
    }

}
