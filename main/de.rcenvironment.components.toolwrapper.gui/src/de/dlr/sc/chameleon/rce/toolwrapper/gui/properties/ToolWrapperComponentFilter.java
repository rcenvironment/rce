/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.gui.properties;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;


/**
 * Filter for ToolWrapper instances.
 *
 * @author Markus Kunde
 */
public class ToolWrapperComponentFilter extends ComponentFilter {
    
    @Override
    public boolean filterComponentName(final String componentId) {
        boolean isMatch = false;
        for (String id : CpacsComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
