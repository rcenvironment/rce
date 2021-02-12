/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

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
 * This documentation is duplicated for the class {@link InputAdapterComponent}. Any changes in either comment should most probably be
 * duplicated with the respective other one.
 * 
 * @author Alexander Weinert
 */
public class OutputAdapterComponent extends DefaultComponent {

    private ComponentContext context;

    @Override
    public boolean treatStartAsComponentRun() {
        return false;
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.context = componentContext;
    }

    @Override
    public void processInputs() throws ComponentException {
        final String outputDirectoryPath = context.getConfigurationValue("outputFolder");
        final Map<String, Object> outputsMap = new HashMap<>();

        for (final String inputName : context.getInputs()) {
            final TypedDatum nonreplacedInput = context.readInput(inputName);
            final DataType inputType = context.getInputDataType(inputName);

            final String replacedInput = copyInputToFilesystemIfFile(nonreplacedInput, inputType);

            outputsMap.put(inputName, replacedInput);
        }

        final File outputDirectory = new File(outputDirectoryPath);
        writeOutputsMapToDirectory(outputsMap, outputDirectory);
    }

    private String copyInputToFilesystemIfFile(final TypedDatum nonreplacedInput, final DataType inputType) throws ComponentException {
        final TypedDatum replacedInput;
        if (DataType.FileReference.equals(inputType)) {
            replacedInput = tryCopyFileToLocalFilesystem((FileReferenceTD) nonreplacedInput);
        } else if (DataType.DirectoryReference.equals(inputType)) {
            replacedInput = tryCopyDirectoryToLocalFilesystem((DirectoryReferenceTD) nonreplacedInput);
        } else {
            replacedInput = nonreplacedInput;
        }

        final TypedDatumSerializer typedDatumSerializer = getTypedDatumSerializer();
        return typedDatumSerializer.serialize(replacedInput);
    }

    /**
     * @param nonreplacedDatum The reference to the file to be copied in the local data management.
     * @return The absolute path to which the file was copied.
     * @throws ComponentException Thrown if any exception occurs during copying a file reference to the local filesystem. Contains the
     *         underlying exception as the cause
     */
    private ShortTextTD tryCopyFileToLocalFilesystem(final FileReferenceTD nonreplacedDatum) throws ComponentException {
        try {
            final File tempFile = createTemporaryFile();
            getDataManagementService().copyReferenceToLocalFile(
                nonreplacedDatum.getFileReference(), tempFile, context.getStorageNetworkDestination());

            LogFactory.getLog(this.getClass()).info(StringUtils.format("Copied file to '%s'", tempFile));

            final TypedDatumFactory typedDatumFactory = getTypedDatumFactory();
            return typedDatumFactory.createShortText(tempFile.getAbsolutePath());
        } catch (AuthorizationException | IOException | CommunicationException e) {
            throw new ComponentException("Error when copying outer inputs to inner ones", e);
        }
    }

    /**
     * @param nonreplacedDatum The reference to the directory to be copied in the local data management.
     * @return The absolute path of the directory in the local file system.
     * @throws ComponentException Thrown if any exception occurs during copying a file reference to the local filesystem. Contains the
     *         underlying exception as the cause
     */
    private ShortTextTD tryCopyDirectoryToLocalFilesystem(final DirectoryReferenceTD nonreplacedDatum)
        throws ComponentException {
        try {
            final File tempDir = createTemporaryDirectory();

            getDataManagementService().copyReferenceToLocalDirectory(
                nonreplacedDatum.getDirectoryReference(), tempDir, context.getStorageNetworkDestination());

            final String absolutePathToCopiedDirectory =
                Paths.get(tempDir.getAbsolutePath(), nonreplacedDatum.getDirectoryName()).toString();

            LogFactory.getLog(this.getClass()).info(StringUtils.format("Copied directory to '%s'", tempDir));

            final TypedDatumFactory typedDatumFactory = getTypedDatumFactory();
            return typedDatumFactory.createShortText(absolutePathToCopiedDirectory);
        } catch (AuthorizationException | IOException | CommunicationException e) {
            throw new ComponentException("Error when copying outer inputs to inner ones", e);
        }
    }

    private DataManagementService getDataManagementService() {
        return context.getService(DataManagementService.class);
    }

    private TypedDatumFactory getTypedDatumFactory() {
        return context.getService(TypedDatumService.class).getFactory();
    }

    private TypedDatumSerializer getTypedDatumSerializer() {
        return context.getService(TypedDatumService.class).getSerializer();
    }

    protected File createTemporaryFile() throws IOException {
        return TempFileServiceAccess.getInstance().createTempFileFromPattern("*");
    }

    protected File createTemporaryDirectory() throws IOException {
        return TempFileServiceAccess.getInstance().createManagedTempDir();
    }

    protected void writeOutputsMapToDirectory(final Map<String, Object> outputsMap, final File outputDirectory) throws ComponentException {
        final File outputsFile = new File(outputDirectory, "outputs.json");
        LogFactory.getLog(this.getClass()).info(StringUtils.format("Writing to directory %s", outputDirectory));
        ObjectWriter writer = new ObjectMapper().writer();
        try {
            writer.writeValue(outputsFile, outputsMap);
        } catch (IOException e) {
            throw new ComponentException("Could not write outputs file", e);
        }
    }
}
