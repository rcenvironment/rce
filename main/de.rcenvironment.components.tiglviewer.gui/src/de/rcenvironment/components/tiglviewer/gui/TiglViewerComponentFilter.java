/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.tiglviewer.gui;

import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for TiGL Viewer component instances.
 * 
 * @author Doreen Seider
 */
public class TiglViewerComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : TiglViewerComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
