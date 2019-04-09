/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view;

import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;

/**
 * Interface that to get instantiated {@link Component}s of running workflows injected.
 * 
 * @author Heinrich Wendel
 * @author Sascha Zur
 */
public interface ComponentRuntimeView {

    /**
     * Called by RCE to initialize the data for the given component's runtime view.
     * 
     * @param componentExecutionInformation The {@link ComponentExecutionInformation} of the component to monitor.
     */
    void initializeData(ComponentExecutionInformation componentExecutionInformation);

    /**
     * This method is called *after* the initializeData method is finished and needs to be called in
     * the GUI Thread.
     */
    void initializeView();
}
