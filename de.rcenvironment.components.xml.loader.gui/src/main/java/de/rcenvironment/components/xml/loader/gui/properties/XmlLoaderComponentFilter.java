/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.loader.gui.properties;

import de.rcenvironment.components.xml.loader.common.XmlLoaderComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;


/**
 * Filter for Source instances.
 *
 * @author Markus Kunde
 * @author Arne Bachmann
 */
public class XmlLoaderComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : XmlLoaderComponentConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
