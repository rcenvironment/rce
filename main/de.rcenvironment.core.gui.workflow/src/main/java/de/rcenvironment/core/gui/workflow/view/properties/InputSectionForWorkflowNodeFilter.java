/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.gui.workflow.parts.ReadOnlyWorkflowNodePart;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;


/**
 * Filter class to display the input section for "running" workflow nodes.
 *
 * @author Doreen Seider
 */
public class InputSectionForWorkflowNodeFilter implements IFilter {

    private final boolean inputViewEnabled;
    
    public InputSectionForWorkflowNodeFilter() {
        ConfigurationService configurationService = ServiceRegistry.createAccessFor(this).getService(ConfigurationService.class);
        inputViewEnabled = configurationService.getConfigurationSegment("general")
            .getBoolean(ComponentConstants.CONFIG_KEY_ENABLE_INPUT_TAB, false);
    }
    
    @Override
    public boolean select(Object object) {
        return object instanceof ReadOnlyWorkflowNodePart && inputViewEnabled;
    }


}
