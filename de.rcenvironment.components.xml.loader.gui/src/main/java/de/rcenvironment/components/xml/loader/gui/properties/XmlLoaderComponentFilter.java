/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
