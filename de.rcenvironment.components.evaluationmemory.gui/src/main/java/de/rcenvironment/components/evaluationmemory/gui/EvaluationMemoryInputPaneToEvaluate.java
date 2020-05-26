/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithOutputSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * The configuration pane for the inputs that are to be evaluated by the evaluation memory component. We require a custom pane here since we
 * need to show a customized endpoint edit dialog, namely an {@link EvaluationMemoryEndpointEditDialog} that allows the user to specify
 * tolerances on the input
 *
 * @author Alexander Weinert
 */
public class EvaluationMemoryInputPaneToEvaluate extends InputCoupledWithOutputSelectionPane {

    public EvaluationMemoryInputPaneToEvaluate(String title, String endpointId, Executor executor, Refreshable outputPane) {
        super(title, endpointId, executor, outputPane);
    }

    @Override
    protected void onAddClicked() {
        Map<String, String> metaData = new HashMap<>();

        final Shell parentShell = Display.getDefault().getActiveShell();
        final EndpointMetaDataDefinition metaDataDefinition =
            endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage).getMetaDataDefinition();
        EvaluationMemoryEndpointEditDialog dialog =
            new EvaluationMemoryEndpointEditDialog(parentShell, EndpointActionType.ADD, configuration, endpointType, dynEndpointIdToManage,
                false, metaDataDefinition, metaData);

        if (dialog.open() == Dialog.OK) {
            String name = dialog.getChosenName();
            DataType type = dialog.getChosenDataType();
            metaData = dialog.getMetadataValues();

            appendPercentageSignToTolerance(metaData);
            executeAddCommand(name, type, metaData);
        }
    }

    private void appendPercentageSignToTolerance(Map<String, String> metaData) {
        /*
         * In order to increase usability, we ask the user to only enter the percentage value of the tolerance, but do not require her to
         * enter the percentage sign. Hence, the metadata obtained from the dialog above does not contain the percentage sign, i.e., when
         * displaying the tolerance in this input pane, we would display only the bare number. As this might be confusing, we opt to append
         * the percentage sign here manually before setting and displaying the meta data. NOTE: Since this implies that the percentage sign
         * is stored in the meta data as well, we have to recall to remove it before parsing the resulting value.
         */
        if (metaData.containsKey(EvaluationMemoryComponentConstants.META_TOLERANCE)) {
            final String toleranceText = metaData.get(EvaluationMemoryComponentConstants.META_TOLERANCE);
            if (!toleranceText.isEmpty()) {
                metaData.put(
                    EvaluationMemoryComponentConstants.META_TOLERANCE,
                    toleranceText + EvaluationMemoryComponentConstants.PERCENTAGE_SIGN);
            }
        }
    }
    
    private void removePercentageSignFromTolerance(Map<String, String> metaData) {
        // See #appendPercentageSignToTolerance for an explanation why we require this method
        if (metaData.containsKey(EvaluationMemoryComponentConstants.META_TOLERANCE)) {
            final String toleranceText = metaData.get(EvaluationMemoryComponentConstants.META_TOLERANCE);
            if (!toleranceText.isEmpty()) {
                metaData.put(
                    EvaluationMemoryComponentConstants.META_TOLERANCE,
                    toleranceText.substring(0, toleranceText.length() - 1));
            }
        }
    }

    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        final Shell parentShell = Display.getDefault().getActiveShell();
        final EndpointMetaDataDefinition metaDataDefinition =
            endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage).getMetaDataDefinition();
        
        removePercentageSignFromTolerance(newMetaData);
        EvaluationMemoryEndpointEditDialog dialog =
            new EvaluationMemoryEndpointEditDialog(parentShell, EndpointActionType.EDIT, configuration, endpointType, dynEndpointIdToManage,
                false, metaDataDefinition, newMetaData);

        onEditClicked(name, dialog, newMetaData);
    }

    /**
     * We override this method in order to append the percentage sign to the metadata if necessary. {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane#editEndpoint(
     *      de.rcenvironment.core.component.model.endpoint.api.EndpointDescription,
     *      java.lang.String, de.rcenvironment.core.datamodel.api.DataType, java.util.Map)
     */
    @Override
    protected void editEndpoint(EndpointDescription oldDesc, String newName, DataType newType, Map<String, String> newMetaData) {
        appendPercentageSignToTolerance(newMetaData);
        super.editEndpoint(oldDesc, newName, newType, newMetaData);
    }

}
