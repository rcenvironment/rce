/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.gui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This class represents the default perspective.
 * 
 * @author Thijs Metsch
 * @author Andreas Baecker
 * @author Jan Flink
 */
public class Perspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        
        // relative positions of the views.
        final float leftRatio = 0.2f;
        final float rightRation = 0.8f;
        final float bottomRatio = 0.7f;
        
        layout.addPerspectiveShortcut("de.rcenvironment.core");
        layout.addShowViewShortcut("de.rcenvironment.gui.workflowList");
        layout.addShowViewShortcut("de.rcenvironment.gui.WorkflowComponentConsole");
        layout.addShowViewShortcut("de.rcenvironment.rce.gui.datamanagement.browser.DataManagementBrowser");
        layout.addShowViewShortcut("de.rcenvironment.core.gui.log.LogView");
        layout.addShowViewShortcut("de.rcenvironment.core.gui.tiglviewer.views.TIGLViewer");
        layout.addShowViewShortcut("de.rcenvironment.core.gui.cluster.view.ClusterJobMonitorView");
        layout.addShowViewShortcut("de.rcenvironment.core.gui.command.CommandConsoleViewer");
        layout.addShowViewShortcut("de.rcenvironment.core.gui.communication.views.NetworkView");
        layout.addShowViewShortcut("de.rcenvironment.core.gui.authorization.ComponentPublishingView");
        layout.addShowViewShortcut("de.rcenvironment.core.gui.palette.view.PaletteView");
        
        String editorArea = layout.getEditorArea();
        IFolderLayout left = layout.createFolder("de.rcenvironment.core.Perspective.left", IPageLayout.LEFT, leftRatio, editorArea);
        left.addView(IPageLayout.ID_PROJECT_EXPLORER);
        IFolderLayout bottomLeft =
            layout.createFolder("de.rcenvironment.core.Perspective.bottomLeft", IPageLayout.BOTTOM, bottomRatio,
                "de.rcenvironment.core.Perspective.left");
        bottomLeft.addView(IPageLayout.ID_OUTLINE);
        layout.createPlaceholderFolder("de.rcenvironment.core.Perspective.bottom", IPageLayout.BOTTOM, bottomRatio, editorArea);
        layout.createPlaceholderFolder("de.rcenvironment.core.Perspective.right", IPageLayout.RIGHT, rightRation, editorArea);
    }

}
