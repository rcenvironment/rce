/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.inputprovider.execution;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;

/**
 * Writes given start values into outputs.
 * 
 * @author Mark Geiger
 * @author Doreen Seider
 */
public class InputProviderComponent extends DefaultComponent {

    private ComponentContext componentContext;
    
    private TypedDatumFactory typedDatumFactory;

    private ComponentDataManagementService dataManagementService;
    
    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }
    
    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getOutputs().size() > 0;
    }
    
    @Override
    public void start() throws ComponentException {
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        
        for (String outputName : componentContext.getOutputs()) {
            // Only if there are starting values! = Don't send nothing.
            String value = componentContext.getOutputMetaDataValue(outputName, InputProviderComponentConstants.META_VALUE);
            if (componentContext.getConfigurationKeys().contains(outputName)) {
                value = componentContext.getConfigurationValue(outputName);
            }
            DataType type = componentContext.getOutputDataType(outputName);

            TypedDatum datum;
            switch (componentContext.getOutputDataType(outputName)) {
            case ShortText:
                datum = typedDatumFactory.createShortText(value);
                break;
            case Boolean:
                datum = typedDatumFactory.createBoolean(Boolean.parseBoolean(value));
                break;
            case Float:
                datum = typedDatumFactory.createFloat(Double.parseDouble(value));
                break;
            case Integer:
                datum = typedDatumFactory.createInteger(Long.parseLong(value));
                break;
            case FileReference:
                datum = getTypedDatumForFile(value, outputName);
                break;
            case DirectoryReference:
                datum = getTypedDatumForDirectory(value);
                break;
            default:
                throw new ComponentException("Given data type is not supported: " + type);
            }
            componentContext.writeOutput(outputName, datum);
            componentContext.printConsoleLine("Wrote to output '" + outputName + "': " + value, ConsoleRow.Type.COMPONENT_OUTPUT);
        }
    }
    
    private TypedDatum getTypedDatumForFile(String value, String outputName) throws ComponentException {
        File file = createFileObject(value);
        if (file == null) {
            throw new ComponentException(String.format("%s: No file name given for output '%s'",
                componentContext.getInstanceName(), outputName));
        } else if (!file.exists()) {
            throw new ComponentException(String.format("%s: File doesn't exist on node %s: %s",
                componentContext.getInstanceName(), componentContext.getNodeId().getAssociatedDisplayName(), file.getAbsolutePath()));
        }
        try {
            return dataManagementService.createFileReferenceTDFromLocalFile(componentContext, file, file.getName());
        } catch (IOException ex) {
            throw new ComponentException(String.format("%s: Writing file to data management failed",
                componentContext.getInstanceName()), ex);
        }
    }

    private TypedDatum getTypedDatumForDirectory(String value) throws ComponentException {
        File dir = createFileObject(value);
        if (!dir.exists()) {
            throw new ComponentException(String.format("%s: Directory doesn't exist on node %s: %s",
                componentContext.getInstanceName(), componentContext.getNodeId().getAssociatedDisplayName(), dir.getAbsolutePath()));
        }
        if (!dir.isDirectory()) {
            throw new ComponentException(String.format("%s: Given path doesn't refer to a directory on node %s: %s",
                componentContext.getInstanceName(), componentContext.getNodeId().getAssociatedDisplayName(), dir.getAbsolutePath()));
        }
        try {
            return dataManagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext, dir, dir.getName());
        } catch (IOException ex) {
            throw new ComponentException(String.format("%s: Writing directory to data management failed",
                componentContext.getInstanceName()), ex);
        }
    }

    private File createFileObject(String value) {
        if (value.isEmpty()) {
            return null;
        }
        File file = new File(value);
        if (!file.isAbsolute() && !value.isEmpty()) {
            file = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath() + "/" + value);
        }
        return file;
    }

}
