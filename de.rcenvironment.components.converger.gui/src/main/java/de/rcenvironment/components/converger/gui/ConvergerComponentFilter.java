/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.converger.gui;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for merger component.
 * 
 * @author Sascha Zur
 */
public class ConvergerComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : ConvergerComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
