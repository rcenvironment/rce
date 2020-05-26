/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.remoteaccess;

import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for ToolAcess component.
 * 
 * @author Brigitte Boden
 */
public class SshRemoteAccessClientComponentFilter extends ComponentFilter {
    
    @Override
    public boolean filterComponentName(String componentId) {
        return componentId.startsWith(SshRemoteAccessConstants.COMPONENT_ID);
    }

}
