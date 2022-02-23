/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.scpinputloader.gui;

import de.rcenvironment.components.scpinputloader.common.ScpInputLoaderComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for component.
 * 
 * @author Brigitte Boden
 */
public class ScpInputLoaderComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : ScpInputLoaderComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
