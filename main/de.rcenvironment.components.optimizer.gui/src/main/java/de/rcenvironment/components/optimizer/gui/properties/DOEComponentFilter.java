/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import de.rcenvironment.components.optimizer.common.DOEComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;


/**
 * Filter for OptimizerComponent instances.
 *
 * @author Sascha Zur
 */
public class DOEComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : DOEComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
