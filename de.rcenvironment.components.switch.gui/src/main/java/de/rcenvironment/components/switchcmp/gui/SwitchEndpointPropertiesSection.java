/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.beans.PropertyChangeEvent;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.gui.SwitchConditionSection.TableRowBehavior;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * 
 * Properties section for switch component.
 *
 * @author David Scholz
 * @author Kathrin Schaffert
 */
public class SwitchEndpointPropertiesSection extends EndpointPropertySection {

    protected final Log log = LogFactory.getLog(SwitchEndpointPropertiesSection.class);

    public SwitchEndpointPropertiesSection() {

        SwitchDataInputSelectionPane dataInputPane = new SwitchDataInputSelectionPane(this);

        SwitchConditionInputSelectionPane conditionInputPane = new SwitchConditionInputSelectionPane(
            Messages.conditionInputString, EndpointType.INPUT, SwitchComponentConstants.CONDITION_INPUT_ID, this);

        EndpointSelectionPane dataOutputpane = new EndpointSelectionPane(Messages.dataOutputString, EndpointType.OUTPUT,
            null, null, new String[] {}, this, true);

        setColumns(1);
        setPanes(dataInputPane, conditionInputPane, dataOutputpane);
        dataInputPane.setAllPanes(new EndpointSelectionPane[] { dataInputPane, conditionInputPane, dataOutputpane });
    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);
        refreshSwitchEndpointsProperties();
    }

    private void refreshSwitchEndpointsProperties() {
        ComponentInstanceProperties config = getConfiguration();
        EndpointDescriptionsManager outputDescManager = config.getOutputDescriptionsManager();
        Set<EndpointDescription> inputDescSet = config.getInputDescriptionsManager().getDynamicEndpointDescriptions();

        int count = 0;
        for (EndpointDescription inputDesc : inputDescSet) {
            if (inputDesc.getDynamicEndpointIdentifier().equals(SwitchComponentConstants.DATA_INPUT_ID)) {
                count++;
            }
        }

        int size = outputDescManager.getDynamicEndpointDescriptions().size();
        int numDataOutputs = 0;
        if (count != 0 && size != 0) {
            // number of data outputs per input without NO_MATCH Output channel (substract -1)
            numDataOutputs = size / count - 1;
        }

        String conTable = config.getConfigurationDescription().getConfigurationValue(SwitchComponentConstants.CONDITION_KEY);

        if (conTable == null) {
            log.warn(
                "The condition key in the workflow configuration file is damaged or missing. "
                    + "Please note that Data Outputs can be lost.");
            return;
        }

        int numOfCon = SwitchConditionSection.getTableContentLength(conTable);

        if (numDataOutputs == numOfCon) {
            super.refreshSection();
            return;
        }

        TableRowBehavior trb;
        while (numDataOutputs != numOfCon) {
            if (numDataOutputs < numOfCon) {
                trb = TableRowBehavior.ADD_ROW;
                SwitchConditionSection.updateOutputDescriptions(config, trb, numDataOutputs);
                numDataOutputs = numDataOutputs + 1;
            } else {
                trb = TableRowBehavior.REMOVE_ROW;
                SwitchConditionSection.updateOutputDescriptions(config, trb, numDataOutputs);
                numDataOutputs = numDataOutputs - 1;
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        if (evt.getPropertyName().equals(SwitchComponentConstants.CONDITION_KEY_PROPERTY_ID)) {
            refreshSwitchEndpointsProperties();
        }
    }

}
