/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.gui.workflow.editor.properties.AbstractWorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Endpoint selection pane.
 * 
 * @author Doreen Seider
 */
public class ConvergerEndpointSelectionPane extends EndpointSelectionPane {

    private final TypedDatumFactory typedDatumFactory;
    
    private final TypedDatumSerializer typedDatumSerializer;
    
    private final ConvergerEndpointSelectionPane outputPane;
    
    public ConvergerEndpointSelectionPane(String title, EndpointType direction, Executor executor, boolean readonly,
        String id, ConvergerEndpointSelectionPane outputPane) {
        super(title, direction, executor, readonly, id, false);
        this.outputPane = outputPane;
        TypedDatumService typedDatumService = ServiceRegistry.createAccessFor(this).getService(TypedDatumService.class);
        typedDatumFactory = typedDatumService.getFactory();
        typedDatumSerializer = typedDatumService.getSerializer();
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = new AddInputOutputCommand(name, type, metaData);
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command = new EditInputOutputCommand(oldDescription, newDescription);
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        WorkflowNodeCommand command = new RemoveInputOutputCommand(names);
        execute(command);
    }

    /**
     * Adds endpoints.
     * 
     * @author Doreen Seider
     */
    private class AddInputOutputCommand extends AbstractWorkflowNodeCommand {

        private final String name;

        private final DataType type;

        private Map<String, String> metaData;

        protected AddInputOutputCommand(String name, DataType type, Map<String, String> metaData) {
            this.name = name;
            this.type = type;
            this.metaData = metaData;
        }

        @Override
        public void execute2() {
            addEndpointsForValueToConverge(getProperties(), name, type, metaData, getWorkflowNode());
            updateTable();
            outputPane.updateTable();

        }

        @Override
        public void undo2() {
            removeEndpointsForValueToConverge(getProperties(), name, getWorkflowNode());
            updateTable();
            outputPane.updateTable();

        }
    }

    /**
     * Adds endpoints.
     * 
     * @author Doreen Seider
     */
    private class EditInputOutputCommand extends AbstractWorkflowNodeCommand {

        private EndpointDescription newDesc;

        private EndpointDescription oldDesc;

        protected EditInputOutputCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
            this.oldDesc = oldDescription;
            this.newDesc = newDescription;
        }

        @Override
        public void execute2() {
            
            setEndpointInitValue(newDesc.getMetaData());
            
            EndpointDescriptionsManager inputManager = getProperties().getInputDescriptionsManager();
            inputManager.editDynamicEndpointDescription(oldDesc.getName(), newDesc.getName(), newDesc.getDataType(), newDesc.getMetaData());

            EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();

            EndpointDescription outputDesc = outputManager.getEndpointDescription(oldDesc.getName());
            outputDesc.setName(newDesc.getName());
            outputManager.editDynamicEndpointDescription(oldDesc.getName(), newDesc.getName(), outputDesc.getDataType(),
                outputDesc.getMetaData());

            EndpointDescription outputConvergedDesc = outputManager.getEndpointDescription(oldDesc.getName()
                + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
            outputConvergedDesc.setName(newDesc.getName() + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
            outputManager.editDynamicEndpointDescription(oldDesc.getName() + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                newDesc.getName() + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, outputConvergedDesc.getDataType(),
                outputConvergedDesc.getMetaData());
            updateTable();
            outputPane.updateTable();
        }

        @Override
        public void undo2() {
            EndpointDescriptionsManager inputManager = getProperties().getInputDescriptionsManager();
            inputManager.editDynamicEndpointDescription(newDesc.getName(), oldDesc.getName(), oldDesc.getDataType(), oldDesc.getMetaData());

            EndpointDescription outputDesc = getProperties().getOutputDescriptionsManager().getEndpointDescription(newDesc.getName());
            EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();
            outputDesc.setName(oldDesc.getName());
            outputManager.editDynamicEndpointDescription(newDesc.getName(), oldDesc.getName(), outputDesc.getDataType(),
                outputDesc.getMetaData());
            EndpointDescription outputConvergedDesc = getProperties().getOutputDescriptionsManager()
                .getEndpointDescription(newDesc.getName() + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
            outputConvergedDesc.setName(oldDesc.getName() + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
            outputManager.editDynamicEndpointDescription(newDesc.getName() + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                oldDesc.getName() + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, outputConvergedDesc.getDataType(),
                outputConvergedDesc.getMetaData());
            updateTable();
            outputPane.updateTable();
        }
    }

    /**
     * Removes endpoints.
     * 
     * @author Doreen Seider
     */
    private class RemoveInputOutputCommand extends AbstractWorkflowNodeCommand {

        private final List<String> names;

        private Map<String, EndpointDescription> oldDescriptions;

        protected RemoveInputOutputCommand(List<String> names) {
            this.names = names;
            this.oldDescriptions = new HashMap<String, EndpointDescription>();
        }

        @Override
        public void initialize() {
            for (String name : names) {
                oldDescriptions.put(name, getProperties().getInputDescriptionsManager().getEndpointDescription(name));
            }
        }

        @Override
        public void execute2() {
            for (String name : names) {
                removeEndpointsForValueToConverge(getProperties(), name, getWorkflowNode());
                updateTable();
                outputPane.updateTable();
            }
        }

        @Override
        public void undo2() {
            for (String name : names) {
                EndpointDescription oldDescription = oldDescriptions.get(name);
                addEndpointsForValueToConverge(getProperties(), name, oldDescription.getDataType(), oldDescription.getMetaData(),
                    getWorkflowNode());
                updateTable();
                outputPane.updateTable();
            }

        }
    }

    private void addEndpointsForValueToConverge(ComponentInstanceProperties properties,
        String name, DataType type, Map<String, String> metaData, WorkflowNode workflowNode) {
        
        setEndpointInitValue(metaData);
        
        EndpointDescriptionsManager inputManager = properties.getInputDescriptionsManager();
        inputManager.addDynamicEndpointDescription(ConvergerComponentConstants.ID_VALUE_TO_CONVERGE,
            name, type, metaData);
        EndpointDescriptionsManager outputManager = properties.getOutputDescriptionsManager();
        outputManager.addDynamicEndpointDescription(ConvergerComponentConstants.ID_CONVERGED_VALUE,
            name, type, new HashMap<String, String>());
        outputManager.addDynamicEndpointDescription(ConvergerComponentConstants.ID_CONVERGED_VALUE,
            name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, type, new HashMap<String, String>());
        
    }

    private void removeEndpointsForValueToConverge(ComponentInstanceProperties properties, String name, WorkflowNode workflowNode) {
        EndpointDescriptionsManager inputManager = properties.getInputDescriptionsManager();
        inputManager.removeDynamicEndpointDescription(name);
        EndpointDescriptionsManager outputManager = properties.getOutputDescriptionsManager();
        outputManager.removeDynamicEndpointDescription(name);
        outputManager.removeDynamicEndpointDescription(name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
    }
    
    private void setEndpointInitValue(Map<String, String> metaData) {
        if (metaData.containsKey(ConvergerComponentConstants.META_STARTVALUE)
            && Boolean.valueOf(metaData.get(ConvergerComponentConstants.META_HAS_STARTVALUE))) {
            FloatTD startValue = typedDatumFactory.createFloat(Double.valueOf(metaData.get(ConvergerComponentConstants.META_STARTVALUE)));
            metaData.put(ComponentConstants.INPUT_METADATA_KEY_INIT_VALUE, typedDatumSerializer.serialize(startValue));
        } else {
            metaData.remove(ComponentConstants.INPUT_METADATA_KEY_INIT_VALUE);
        }
    }

}
