/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration.workflow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationConstants;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.workflow.integration.IntegrationHistoryDataItemSubtreeBuilder;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for workflow integrator components.
 * 
 * @author Jan Flink
 */
public class WorkflowIntegrationHistoryDataItemSubtreeBuilder extends IntegrationHistoryDataItemSubtreeBuilder {

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { WorkflowIntegrationConstants.WORKFLOW_INTEGRATOR_COMPONENT_ID_PREFIX.replace(".", "\\.") + ".*" };
    }

    @Override
    public Serializable deserializeHistoryDataItem(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (Serializable) ois.readObject();
    }
}
