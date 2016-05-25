/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
        final float bottomRatio = 0.7f;
        
        String editorArea = layout.getEditorArea();
        IFolderLayout left = layout.createFolder("de.rcenvironment.core.Perspective.left", IPageLayout.LEFT, leftRatio, editorArea);
        left.addView(IPageLayout.ID_PROJECT_EXPLORER);
        IFolderLayout bottomLeft =
            layout.createFolder("de.rcenvironment.core.Perspective.bottomLeft", IPageLayout.BOTTOM, bottomRatio,
                "de.rcenvironment.core.Perspective.left");
        bottomLeft.addView(IPageLayout.ID_OUTLINE);
        layout.createPlaceholderFolder("de.rcenvironment.core.Perspective.bottom", IPageLayout.BOTTOM, bottomRatio, editorArea);
    }

}
