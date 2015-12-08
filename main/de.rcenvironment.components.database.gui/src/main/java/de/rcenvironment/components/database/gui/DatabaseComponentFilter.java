/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Database component filter.
 *
 * @author Oliver Seebach
 */
public class DatabaseComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        if (componentId.startsWith(DatabaseComponentConstants.COMPONENT_ID)) {
            isMatch = true;
        }
        return isMatch;
    }

}
