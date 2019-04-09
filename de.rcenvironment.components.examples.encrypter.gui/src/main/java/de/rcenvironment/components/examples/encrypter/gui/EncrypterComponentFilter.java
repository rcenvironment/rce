/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.gui;

import de.rcenvironment.components.examples.encrypter.common.EncrypterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for encoder component.
 * 
 * @author Sascha Zur
 */
public class EncrypterComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        return componentId.startsWith(EncrypterComponentConstants.COMPONENT_ID);
    }

}
