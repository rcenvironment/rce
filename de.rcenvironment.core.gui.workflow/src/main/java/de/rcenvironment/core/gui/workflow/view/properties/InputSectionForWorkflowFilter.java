/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;


/**
 * Filter class to display the input section for "running" workflow.
 *
 * @author Doreen Seider
 */
public class InputSectionForWorkflowFilter implements IFilter {

    private final boolean inputViewEnabled;
    
    public InputSectionForWorkflowFilter() {
        ConfigurationService configurationService = ServiceRegistry.createAccessFor(this).getService(ConfigurationService.class);
        inputViewEnabled = configurationService.getConfigurationSegment("general")
            .getBoolean(ComponentConstants.CONFIG_KEY_ENABLE_INPUT_TAB, false);
    }
    
    @Override
    public boolean select(Object object) {
        return object instanceof WorkflowExecutionInformationPart && inputViewEnabled;
    }


}
