/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.inputprovider.execution;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Writes given start values into outputs.
 * 
 * @author Mark Geiger
 * @author Doreen Seider
 */
public class InputProviderComponent extends DefaultComponent {

    private static final String BACKSLASH = "\\";

    private static final String FRONTSLASH = "/";

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
                datum = getTypedDatumForDirectory(value, outputName);
                break;
            default:
                throw new ComponentException("Internal error: Given data type is not supported: " + type);
            }
            componentContext.writeOutput(outputName, datum);
            componentContext.getLog().componentInfo("Wrote to output '" + outputName + "': " + value);
        }
    }

    private TypedDatum getTypedDatumForFile(String value, String outputName) throws ComponentException {
        File file = createFileObject(value);
        if (file == null) {
            throw new ComponentException(StringUtils.format("Internal error: No file given for output '%s'", outputName));
        } else if (!file.exists()) {
            throw new ComponentException(StringUtils.format("Given path doesn't refer to a file on node %s: %s",
                componentContext.getNodeId().getAssociatedDisplayName(), file.getAbsolutePath()));
        }
        try {
            return dataManagementService.createFileReferenceTDFromLocalFile(componentContext, file, file.getName());
        } catch (IOException e) {
            throw new ComponentException("Failed to store file into the data management - "
                + "if it is not stored in the data management, it can not be sent as output value: " + file.getAbsolutePath(), e);
        }
    }

    private TypedDatum getTypedDatumForDirectory(String value, String outputName) throws ComponentException {
        File dir = createFileObject(value);
        if (dir == null) {
            throw new ComponentException(StringUtils.format("No directory given for output '%s'", outputName));
        } else if (!dir.exists()) {
            throw new ComponentException(StringUtils.format("Directory doesn't exist on node %s: %s",
                componentContext.getNodeId().getAssociatedDisplayName(), dir.getAbsolutePath()));
        }
        if (!dir.isDirectory()) {
            throw new ComponentException(StringUtils.format("Given path doesn't refer to a directory on node %s: %s",
                componentContext.getNodeId().getAssociatedDisplayName(), dir.getAbsolutePath()));
        }
        try {
            return dataManagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext, dir, dir.getName());
        } catch (IOException e) {
            throw new ComponentException("Failed to store directory into the data management - "
                + "if it is not stored in the data management, it can not be sent as output value: " + dir.getAbsolutePath(), e);
        }
    }

    private File createFileObject(String value) throws ComponentException {
        if (value.isEmpty()) {
            return null;
        }
        // if front and backslashes are mixed -> exception
        if (value.contains(FRONTSLASH) && value.contains(BACKSLASH)) {
            throw new ComponentException(StringUtils.format(
                "Given path to file or directory could not be resolved, as it contains front and backslash as well: %s", value));
        }
        // determine separator
        String usedSeparator = "";
        if (value.contains(FRONTSLASH)) {
            usedSeparator = FRONTSLASH;
        } else if (value.contains(BACKSLASH)) {
            usedSeparator = BACKSLASH;
        } else {
            // if neither front nor backslash exists -> exception
            throw new ComponentException(StringUtils.format("Given path to file or directory does not exist on node %s: %s",
                componentContext.getNodeId().getAssociatedDisplayName(), value));
        }
        File file = new File(value);
        if (!file.isAbsolute()) {
            String projectName = value.split(usedSeparator)[0];
            String pathToFileWithinProject = value.substring(projectName.length());
            IPath projectLocation = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getLocation();
            if (projectLocation == null) {
                throw new ComponentException("Given project not found: " + projectName);
            }
            file = new File(projectLocation.toFile().getAbsolutePath() + usedSeparator + pathToFileWithinProject);
        }
        return file;
    }

}
