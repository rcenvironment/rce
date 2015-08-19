/*
 * Copyright (C) 2006-2015 DLR, Germany
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
 */
public class Perspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        
        // relative positions of the views.
        final float leftRatio = 0.3f;
        final float bottomRatio = 0.6f;
        
        String editorArea = layout.getEditorArea();
        IFolderLayout left = layout.createFolder("de.rcenvironment.core.Perspective.left", IPageLayout.LEFT, leftRatio, editorArea);
        layout.createPlaceholderFolder("de.rcenvironment.core.Perspective.bottom", IPageLayout.BOTTOM, bottomRatio, editorArea);
        left.addView(IPageLayout.ID_PROJECT_EXPLORER);
    }

}
