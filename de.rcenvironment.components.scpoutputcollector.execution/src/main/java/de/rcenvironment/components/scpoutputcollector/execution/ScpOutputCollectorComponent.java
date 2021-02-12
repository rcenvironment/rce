/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.scpoutputcollector.execution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.scpoutputcollector.common.ScpOutputCollectorComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.api.LocalExecutionOnly;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Implementation of the ScpOutputcollector component.
 * 
 * @author Brigitte Boden
 * 
 */
@LocalExecutionOnly
public class ScpOutputCollectorComponent extends DefaultComponent {

    private static final String UNDERSCORE = "_";

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private TypedDatumSerializer serializer;

    private ObjectMapper mapper;

    private Map<String, List<String>> inputsMap;

    private File downloadDir;

    private File outputsFile;

    private Log log;

    private boolean uncompressedDownload;

    private boolean simpleDescriptionFormat;

    private TempFileService tempFileService = TempFileServiceAccess.getInstance();

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        log = LogFactory.getLog(getClass());
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        serializer = componentContext.getService(TypedDatumService.class).getSerializer();
        mapper = new ObjectMapper();
        inputsMap = new HashMap<String, List<String>>();
        // Create download dir
        downloadDir =
            new File(componentContext.getConfigurationValue(ScpOutputCollectorComponentConstants.DOWNLOAD_DIRECTORY_CONFIGURATION_KEY));
        downloadDir.mkdirs();
        uncompressedDownload =
            Boolean.valueOf(componentContext
                .getConfigurationValue(ScpOutputCollectorComponentConstants.UNCOMPRESSED_DOWNLOAD_CONFIGURATION_KEY));
        simpleDescriptionFormat =
            Boolean.valueOf(componentContext
                .getConfigurationValue(ScpOutputCollectorComponentConstants.SIMPLE_DESCRIPTION_CONFIGURATION_KEY));
    }

    @Override
    public void processInputs() throws ComponentException {

        // Read the inputs and put them in the downloadDir/the outputs.json file

        try {
            if (componentContext != null && componentContext.getInputsWithDatum() != null) {
                for (String inputName : componentContext.getInputsWithDatum()) {
                    final TypedDatum input = componentContext.readInput(inputName);
                    if (!inputsMap.containsKey(inputName)) {
                        inputsMap.put(inputName, new ArrayList<String>());
                    }

                    switch (input.getDataType()) {
                    case DirectoryReference:
                        if (uncompressedDownload) {
                            File inputDir = new File(downloadDir, inputName + UNDERSCORE + (inputsMap.get(inputName).size() + 1)
                                + UNDERSCORE + ((DirectoryReferenceTD) input).getDirectoryName());
                            // Necessary to copy to directory with correct filename
                            File tempDir;
                            try {
                                tempDir = tempFileService.createManagedTempDir();
                            } catch (IOException e) {
                                throw new ComponentException("Failed to create temporary directory that is required by Output Writer", e);
                            }
                            try {
                                dataManagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext,
                                    ((DirectoryReferenceTD) input), tempDir);
                                FileUtils.moveDirectory(new File(tempDir, ((DirectoryReferenceTD) input).getDirectoryName()), inputDir);
                            } catch (IOException e) {
                                throw new ComponentException(StringUtils.format("Failed to write directory of input '%s' to %s",
                                    inputName, inputDir.getAbsolutePath()), e);
                            } finally {
                                try {
                                    tempFileService.disposeManagedTempDirOrFile(tempDir);
                                } catch (IOException e) {
                                    LogFactory.getLog(getClass()).error("Failed to delete temporary directory", e);
                                }
                            }

                            inputsMap.get(inputName).add(inputDir.getName());
                        } else {
                            File inputFile = new File(downloadDir, ((DirectoryReferenceTD) input).getDirectoryReference());
                            dataManagementService.copyReferenceTDToLocalCompressedFile(componentContext, input, inputFile);
                            inputsMap.get(inputName).add(serializer.serialize(input));
                        }
                        break;
                    case FileReference:
                        if (uncompressedDownload) {
                            File inputFile = new File(downloadDir, inputName + UNDERSCORE + (inputsMap.get(inputName).size() + 1)
                                + UNDERSCORE + ((FileReferenceTD) input).getFileName());
                            dataManagementService.copyFileReferenceTDToLocalFile(componentContext, ((FileReferenceTD) input), inputFile);
                            inputsMap.get(inputName).add(inputFile.getName());
                        } else {
                            File inputDir = new File(downloadDir, ((FileReferenceTD) input).getFileReference());
                            dataManagementService.copyReferenceTDToLocalCompressedFile(componentContext, input, inputDir);
                            inputsMap.get(inputName).add(serializer.serialize(input));
                        }
                        break;
                    default:
                        if (simpleDescriptionFormat) {
                            inputsMap.get(inputName).add(input.toString());
                        } else {
                            inputsMap.get(inputName).add(serializer.serialize(input));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to copy file or directory to download directory", e);
        }
    }

    @Override
    public void tearDown(FinalComponentState state) {
        outputsFile =
            new File(downloadDir,
                componentContext.getConfigurationValue(ScpOutputCollectorComponentConstants.OUTPUTS_DESCRIPTION_FILE_CONFIGURATION_KEY));
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputsFile, inputsMap);
        } catch (IOException e) {
            log.error("Could not write output values to outputs file " + outputsFile.getAbsolutePath());
        }
    }
}
