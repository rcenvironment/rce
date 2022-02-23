/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A workflow component that is not available to the user, but that it only used by a {@link WorkflowFunction}. Such a function must pass
 * values into a workflow and obtain the result data from the workflow. In order to do so, it injects an instance each of
 * {@link InputAdapterComponent} and {@link OutputAdapterComponent} which, when executed, read inputs from and write outputs to some
 * temporary, to and from which the WorkflowFunction writes and reads input and output data.
 * 
 * All of the above, including the existence and use of {@link InputAdapterComponent} and {@link OutputAdapterComponent} are clearly
 * implementation details and, as such, should be placed in the implementation package
 * de.rcenvironment.core.workflow.execution.function.internal. Doing so, however, would prevent the execution of these components, as
 * classes in implementation packages cannot be accessed by other bundles (i.e., by the bundle containing the workflow engine in this case).
 * Hence, we place this class in the API package instead of the implementation package.
 * 
 * This documentation is duplicated for the class {@link OutputAdapterComponent}. Any changes in either comment should most probably be
 * duplicated with the respective other one.
 * 
 * @author Alexander Weinert
 */
public class InputAdapterComponent extends DefaultComponent {
    
    private ComponentContext context;

    @Override
    public boolean treatStartAsComponentRun() {
        return !context.getOutputs().isEmpty();
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.context = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        final String inputDirectoryPath = context.getConfigurationValue("inputFolder");
        this.logInfo(StringUtils.format("Reading from input directory %s", inputDirectoryPath));

        Map<String, String> inputValueMap = readInputMapFromInputDirectory(inputDirectoryPath);
        
        writeAllInputsToOutputs(inputValueMap);
    }

    private void writeAllInputsToOutputs(Map<String, String> inputValueMap) throws ComponentException {
        for (Entry<String, String> input : inputValueMap.entrySet()) {
            final DataType dataType = context.getOutputDataType(input.getKey());

            final String inputValue = input.getValue();

            final TypedDatum datumToWriteToOutput;
            if (dataType.equals(DataType.FileReference)) {
                final ShortTextTD filePath = (ShortTextTD) getTypedDatumSerializer().deserialize(inputValue);
                datumToWriteToOutput = loadLocalFileIntoDataManagement(filePath);
            } else if (dataType.equals(DataType.DirectoryReference)) {
                final ShortTextTD directoryPath = (ShortTextTD) getTypedDatumSerializer().deserialize(inputValue);
                datumToWriteToOutput = loadLocalDirectoryIntoDataManagement(directoryPath.getShortTextValue());
            } else {
                datumToWriteToOutput = getTypedDatumSerializer().deserialize(inputValue.toString());
            }

            context.writeOutput(input.getKey(), datumToWriteToOutput);
        }
    }

    // Since we are reading a map from a file here, we have to cast that Map to a Map<String, String>. We cannot read that map directly due
    // to type erasure. Hence, we explicitly disable warnings for this method
    @SuppressWarnings("unchecked")
    protected Map<String, String> readInputMapFromInputDirectory(final String inputDirectoryPath) throws ComponentException {
        final File inputConfiguration = new File(new File(inputDirectoryPath), "inputs.json");
        Map<String, String> inputValueMap;
        try {
            inputValueMap = new ObjectMapper().readValue(inputConfiguration, Map.class);
        } catch (IOException e) {
            throw new ComponentException(StringUtils.format("Could not read input values from file '%s'", inputConfiguration), e);
        }
        
        LogFactory.getLog(this.getClass()).debug(StringUtils.format("Read input values '%s'", inputValueMap));
        return inputValueMap;
    }

    private TypedDatum loadLocalFileIntoDataManagement(final ShortTextTD filePath) throws ComponentException {
        try {
            final File fileToLoad = new File(filePath.getShortTextValue());

            final String logMessage = StringUtils.format("Read file at '%s' into datamanagement", fileToLoad.getAbsolutePath());
            logInfo(logMessage);

            return getComponentDataManagementService().createFileReferenceTDFromLocalFile(context, fileToLoad, fileToLoad.getName());
        } catch (IOException e) {
            throw new ComponentException("Could not store file into local data management", e);
        }
    }

    private TypedDatum loadLocalDirectoryIntoDataManagement(final String directoryPathString)
        throws ComponentException {
        try {
            final File dirToLoad = new File(directoryPathString);

            final String logMessage = StringUtils.format("Read directory at '%s' into datamanagement", dirToLoad.getAbsolutePath());
            logInfo(logMessage);

            return getComponentDataManagementService()
                .createDirectoryReferenceTDFromLocalDirectory(context, dirToLoad, dirToLoad.getName());
        } catch (IOException e) {
            throw new ComponentException("Could not store directory into local data management", e);
        }
    }

    private TypedDatumSerializer getTypedDatumSerializer() {
        return context.getService(TypedDatumService.class).getSerializer();
    }

    private ComponentDataManagementService getComponentDataManagementService() {
        return context.getService(ComponentDataManagementService.class);
    }

    private void logInfo(final String logMessage) {
        LogFactory.getLog(this.getClass()).info(logMessage);
    }
}
