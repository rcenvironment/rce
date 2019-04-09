/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;


/**
 * Action class for opening the authorization groups management dialog.
 *
 * @author Jan Flink
 */
public class ManageAuthorizationGroupsAction extends Action {

    public ManageAuthorizationGroupsAction() {
        super("Authorization Groups...");
    }

    @Override
    public void run() {
        Dialog d = new AuthorizationGroupDialog(Display.getDefault().getActiveShell());
        d.open();
    }

}
