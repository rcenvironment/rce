/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.EndpointHandlingHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * .
 * 
 * @author Sascha Zur
 * @author Brigitte Boden
 * @author Kathrin Schaffert
 */
public class OutputWriterEndpointSelectionPane extends EndpointSelectionPane {

    protected ObjectMapper jsonMapper;

    public OutputWriterEndpointSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage, Executor executor) {
        super(title, direction, dynEndpointIdToManage, new String[] {}, new String[] {}, executor);

        jsonMapper = JsonUtils.getDefaultObjectMapper();
        jsonMapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
    }

    @Override
    protected void onAddClicked() {
        Set<String> paths = new TreeSet<String>();
        for (String endpointName : getDynamicEndpointNames()) {
            paths.add(getMetaData(endpointName).get(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING));
        }
        OutputWriterEndpointEditDialog dialog =
            new OutputWriterEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD, configuration,
                endpointType,
                dynEndpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage)
                    .getMetaDataDefinition(),
                new HashMap<String, String>(), paths);

        onAddClicked(dialog);
    }

    @Override
    protected void onEditClicked() {
        Set<String> paths = new TreeSet<String>();
        for (String endpointName : getDynamicEndpointNames()) {
            paths.add(getMetaData(endpointName).get(OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING));
        }
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = endpointManager.getEndpointDescription(name).getEndpointDefinition().isStatic();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        OutputWriterEndpointEditDialog dialog =
            new OutputWriterEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.EDIT, configuration,
                endpointType,
                dynEndpointIdToManage, isStaticEndpoint, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData, paths);

        onEditClicked(name, dialog, newMetaData);
    }

    @Override
    protected void onEditClicked(String name, EndpointEditDialog dialog, Map<String, String> newMetaData) {

        dialog.initializeValues(name);

        if (dialog.open() == Dialog.OK) {

            EndpointDescription oldDesc = endpointManager.getEndpointDescription(name);

            String newName = dialog.getChosenName();
            DataType newType = dialog.getChosenDataType();
            newMetaData = dialog.getMetadataValues();

            if (isEndpointChanged(oldDesc, newName, newType, newMetaData)) {

                if (EndpointHandlingHelper.editEndpointDataType(endpointType, oldDesc, newType)) {
                    editEndpoint(oldDesc, newName, newType, newMetaData);
                }
            }
        }
    }

    @Override
    protected void editEndpoint(EndpointDescription oldDesc, String newName, DataType newType, Map<String, String> newMetaData) {

        String outputName = outputNameForInputName(oldDesc.getName());

        EndpointDescription newDesc = endpointManager.getEndpointDescription(oldDesc.getName());

        if (!newName.equals(oldDesc.getName())) {
            newDesc.setName(newName);
        }
        newDesc.setDataType(newType);
        newDesc.setMetaData(newMetaData);

        switch (editConfirmed(oldDesc, newName, newType, outputName)) {

        case InputInvolvedInTarget:
            executeEditCommand(oldDesc, newDesc, true);
            break;
        case DoNotRemoveCompatibleTypes:
            executeEditCommand(oldDesc, newDesc, false);
            break;
        case InputNotInvolvedInTarget:
            return;
        default:
            return;
        }
    }

    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription, boolean removeInput) {
        String outputName = outputIdForInputName(oldDescription.getName());
        WorkflowNodeCommand command =
            new OutputWriterEditDynamicInputCommand(endpointType, oldDescription, newDescription, outputName, removeInput, this);
        execute(command);
    }

    /**
     * User has to confirm change if an outputLocation is involved.
     * 
     * @param oldDesc old endpoint
     * @param newDataType target data type
     * @param newName new name for input
     * @param outputName name of corresponding output
     * @return <code>true</code> if edit operation shall be performed <code>false</code> otherwise
     */
    protected static DataTypeInputHandling editConfirmed(EndpointDescription oldDesc, String newName, DataType newDataType,
        String outputName) {
        DataType[] compatibleTypes = new DataType[] { DataType.Integer, DataType.Float };
        List<DataType> compatibleTypesList = Arrays.asList(compatibleTypes);
        if (outputName == null) {
            return DataTypeInputHandling.InputInvolvedInTarget;
        }
        if (compatibleTypesList.contains(newDataType) && compatibleTypesList.contains(oldDesc.getDataType())) {
            return DataTypeInputHandling.DoNotRemoveCompatibleTypes;
        }
        if (newDataType.equals(DataType.DirectoryReference) || newDataType.equals(DataType.FileReference) || newName != oldDesc
            .getName()) {

            if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
                Messages.editingInputWithOutputLocationDialogTitle,
                StringUtils.format(Messages.editingInputWithOutputLocationDialogText,
                    oldDesc.getName(), outputName))) {
                return DataTypeInputHandling.InputNotInvolvedInTarget;
            }
        }
        return DataTypeInputHandling.InputInvolvedInTarget;
    }

    /**
     * 
     * Enum to handle different Input Data Type behaviour. Inputs written to target, respectively not written to target. Inputs written to
     * target and shouldn't be removed.
     * 
     * @author Kathrin Schaffert
     *
     */
    protected enum DataTypeInputHandling {
        InputInvolvedInTarget,
        InputNotInvolvedInTarget,
        DoNotRemoveCompatibleTypes
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        Set<String> outputLocationIds = new HashSet<String>();
        Set<String> outputLocationNames = new HashSet<String>();
        for (String inputName : names) {
            if (outputIdForInputName(inputName) != null) {
                outputLocationIds.add(outputIdForInputName(inputName));
                outputLocationNames.add(outputNameForInputName(inputName));
            }
        }
        if (deleteConfirmed(outputLocationNames)) {

            final WorkflowNodeCommand command =
                new OutputWriterRemoveDynamicInputCommand(endpointType, dynEndpointIdToManage, names,
                    new ArrayList<String>(outputLocationIds), this);
            execute(command);

        }
    }

    /**
     * User has to confirm deletion if outputLocations are involved.
     * 
     * @return <code>true</code> if edit operation shall be performed <code>false</code> otherwise
     */
    protected static boolean deleteConfirmed(Set<String> outputLocationNames) {
        if (!outputLocationNames.isEmpty()) {
            if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
                Messages.editingInputWithOutputLocationDialogTitle,
                StringUtils.format(Messages.deletingInputWithOutputLocationDialogText,
                    outputLocationNames.toString()))) {
                return false;
            }
        }
        return true;
    }

    private List<String> getDynamicEndpointNames() {
        List<String> result = new LinkedList<String>();
        for (EndpointDescription e : endpointManager.getDynamicEndpointDescriptions()) {
            result.add(e.getName());
        }
        return result;
    }

    /**
     * 
     * @param inputName Name of input
     * @return ID of outputLocation, if inputName is connected to one, null otherwise
     */
    protected String outputIdForInputName(String inputName) {
        String jsonString = configuration.getConfigurationDescription()
            .getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
        if (jsonString == null) {
            jsonString = "";
        }
        try {
            OutputLocationList list =
                jsonMapper.readValue(jsonString, OutputLocationList.class);

            for (OutputLocation ol : list.getOutputLocations()) {
                if (ol.getInputs().contains(inputName)) {
                    return ol.getGroupId();
                }
            }

        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when reading JSON: " + e.getMessage());
        }
        return null;
    }

    /**
     * 
     * @param inputName Name of input
     * @return Name of outputLocation, if inputName is connected to one, null otherwise
     */
    protected String outputNameForInputName(String inputName) {
        String jsonString = configuration.getConfigurationDescription()
            .getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
        if (jsonString == null) {
            jsonString = "";
        }
        try {
            OutputLocationList list =
                jsonMapper.readValue(jsonString, OutputLocationList.class);

            for (OutputLocation ol : list.getOutputLocations()) {
                if (ol.getInputs().contains(inputName)) {
                    return ol.getFilename();
                }
            }

        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when reading JSON: " + e.getMessage());
        }
        return null;
    }

}
