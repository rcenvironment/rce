/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;


/**
 * Filter for VampZero instances.
 *
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class VampZeroInitializerComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(final String componentId) {
        boolean isMatch = false;
        for (String id : VampZeroInitializerComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
