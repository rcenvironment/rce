/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredViewer;

/**
 * Show group ID action.
 *
 * @author Jan Flink
 */
public final class ShowAuthorizationGroupIdAction extends Action {

    private StructuredViewer viewer;

    protected ShowAuthorizationGroupIdAction(StructuredViewer viewer) {
        super("Show Authorization Group IDs", IAction.AS_CHECK_BOX);
        this.viewer = viewer;
        setChecked(false);
    }

    @Override
    public void run() {
        if (viewer.getLabelProvider() instanceof AuthorizationLabelProvider) {
            ((AuthorizationLabelProvider) viewer.getLabelProvider()).setShowGroupID(isChecked());
            viewer.refresh();
        }
    }
}

