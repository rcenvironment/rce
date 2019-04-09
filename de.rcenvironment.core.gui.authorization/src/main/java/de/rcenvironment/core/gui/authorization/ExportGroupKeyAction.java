/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationService;

/**
 * Action class to export an authorization group key.
 *
 * @author Oliver Seebach
 * @author Jan Flink
 */
public final class ExportGroupKeyAction extends SelectionProviderAction {

    private AuthorizationService authorizationService;

    public ExportGroupKeyAction(ISelectionProvider selectionProvider, AuthorizationService authorizationService) {
        super(selectionProvider, "Export Group Key...");
        this.authorizationService = authorizationService;
    }

    @Override
    public void run() {

        IStructuredSelection selection = getStructuredSelection();

        if (selection.isEmpty() || !(selection.getFirstElement() instanceof AuthorizationAccessGroup)) {
            return;
        }

        final Display display = Display.getDefault();
        Shell shell = display.getActiveShell();
        ExportGroupKeyDialog exportGroupKeyDialog =
            new ExportGroupKeyDialog(shell,
                authorizationService.exportToString((AuthorizationAccessGroup) selection.getFirstElement()));
        exportGroupKeyDialog.open();
    }

    @Override
    public void selectionChanged(IStructuredSelection selection) {
        Object selectedGroup = selection.getFirstElement();
        if (selection.size() == 1 && selectedGroup instanceof AuthorizationAccessGroup
            && !authorizationService.isPublicAccessGroup((AuthorizationAccessGroup) selectedGroup)) {
            setEnabled(true);
            return;
        }
        setEnabled(false);
    }
}
