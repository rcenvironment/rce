/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.scpinputloader.execution;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.scpinputloader.common.ScpInputLoaderComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.api.LocalExecutionOnly;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Loads inputs that were transferred via SCP from a json file and writes them to outputs.
 * 
 * @author Brigitte Boden
 */
@LocalExecutionOnly
public class ScpInputLoaderComponent extends DefaultComponent {

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private File uploadDirectory;

    private TypedDatumFactory typedDatumFactory;

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
        TypedDatumSerializer serializer = componentContext.getService(TypedDatumService.class).getSerializer();
        ObjectMapper mapper = new ObjectMapper();

        // TODO improve error handling
        uploadDirectory =
            new File(componentContext.getConfigurationValue(ScpInputLoaderComponentConstants.UPLOAD_DIRECTORY_CONFIGURATION_KEY));

        File inputsDescriptionFile =
            new File(uploadDirectory,
                componentContext.getConfigurationValue(ScpInputLoaderComponentConstants.INPUTS_DESCRIPTION_FILE_CONFIGURATION_KEY));

        if (!inputsDescriptionFile.exists()) {
            throw new ComponentException(
                "Inputs description file inputs.json does not exist in the input directory. Possible reason: The component was called"
                    + " in a local workflow; it should only be used in an SSH remote access workflow. ");
        }

        // When called from another RCE instance, the input files and directories are already compressed. When called e.g. from
        // the stand-alone RCE-Remote tool, they can be uncompressed.
        boolean uncompressedInput =
            Boolean.valueOf(componentContext.getConfigurationValue(ScpInputLoaderComponentConstants.UNCOMPRESSED_UPLOAD_CONFIGURATION_KEY));
        boolean simpleFormat =
            Boolean.valueOf(componentContext.getConfigurationValue(ScpInputLoaderComponentConstants.SIMPLE_DESCRIPTION_CONFIGURATION_KEY));

        try {
            @SuppressWarnings("unchecked") Map<String, Object> outputsMap =
                mapper.readValue(inputsDescriptionFile, new HashMap<String, Object>().getClass());
            for (String outputName : componentContext.getOutputs()) {
                DataType type;
                Object valueObj = outputsMap.get(outputName);
                // May be null in case of "not required" or "required if connected" inputs
                if (valueObj != null) {
                    String value = valueObj.toString();
                    type = componentContext.getOutputDataType(outputName);
                    TypedDatum outputTD;
                    if (simpleFormat) {
                        outputTD = getTypedDatumFromSimpleString(value, type, uncompressedInput);
                    } else {
                        TypedDatum deserializedTD = serializer.deserialize(value);
                        switch (type) {
                        case DirectoryReference:
                            outputTD =
                                getTypedDatumForDirectory(((DirectoryReferenceTD) deserializedTD).getDirectoryReference(),
                                    ((DirectoryReferenceTD) deserializedTD).getDirectoryName(), uncompressedInput);
                            break;
                        case FileReference:
                            outputTD =
                                getTypedDatumForFile(((FileReferenceTD) deserializedTD).getFileReference(),
                                    ((FileReferenceTD) deserializedTD).getFileName(), uncompressedInput);
                            break;
                        default:
                            outputTD = deserializedTD;
                        }
                    }
                    componentContext.writeOutput(outputName, outputTD);
                }
            }
        } catch (IOException e) {
            throw new ComponentException("Could not parse inputs.json description file.");
        }

    }

    private TypedDatum getTypedDatumFromSimpleString(String value, DataType type, boolean uncompressed) throws ComponentException {
        TypedDatum datum;
        try {
            switch (type) {
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
                datum = getTypedDatumForFile(value, value, uncompressed);
                break;
            case DirectoryReference:
                datum = getTypedDatumForDirectory(value, value, uncompressed);
                break;
            default:
                throw new ComponentException("Internal error: Given data type is not supported: " + type);
            }
        } catch (NumberFormatException e) {
            throw new ComponentException("Input datum " + value + " could not be parsed to type " + type);
        }
        return datum;
    }

    private TypedDatum getTypedDatumForFile(String fileRef, String origFileName, boolean uncompressed)
        throws ComponentException {
        File file = new File(uploadDirectory, fileRef);
        if (!file.exists()) {
            throw new ComponentException(StringUtils.format("Given path doesn't refer to a file in the input directory: %s",
                 file.getName()));
        }
        try {
            if (uncompressed) {
                return dataManagementService.createFileReferenceTDFromLocalFile(componentContext, file, origFileName);
            } else {
                return dataManagementService.createFileReferenceTDFromLocalCompressedFile(componentContext, file, origFileName);
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to store file into the data management - "
                + "if it is not stored in the data management, it can not be sent as output value: " + file.getAbsolutePath(), e);
        }
    }

    private TypedDatum getTypedDatumForDirectory(String directory, String origDirName, boolean uncompressed)
        throws ComponentException {
        File dir = new File(uploadDirectory, directory);
        if (!dir.exists()) {
            throw new ComponentException(StringUtils.format("Directory doesn't exist on node in the input directory: %s",
                 dir.getName()));
        }
        try {
            if (uncompressed) {
                return dataManagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext, dir, origDirName);
            } else {
                return dataManagementService.createDirectoryReferenceTDFromLocalCompressedFile(componentContext, dir, origDirName);
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to store directory into the data management - "
                + "if it is not stored in the data management, it can not be sent as output value: " + dir.getAbsolutePath(), e);
        }
    }

}
