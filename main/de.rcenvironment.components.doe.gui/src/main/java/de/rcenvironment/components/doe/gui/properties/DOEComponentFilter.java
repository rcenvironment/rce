/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.gui.properties;

import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for the DoE component.
 * 
 * @author Sascha Zur
 */
public class DOEComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        return componentId.startsWith(DOEConstants.COMPONENT_ID);
    }

}
