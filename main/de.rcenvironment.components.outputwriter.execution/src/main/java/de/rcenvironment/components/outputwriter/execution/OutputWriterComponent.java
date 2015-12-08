/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * The default Outputwriter backend.
 * 
 * @author Hendrik Abbenhaus
 * @author Sascha Zur
 * @author Brigitte Boden
 * 
 */
public class OutputWriterComponent extends DefaultComponent {

    private static final String BACKSLASHES = "\\";

    private static final String DOT = ".";

    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss-S";

    private ComponentContext componentContext;
    
    private ComponentLog componentLog;

    private ComponentDataManagementService dataManagementService;
    
    private TempFileService tempFileService = TempFileServiceAccess.getInstance();

    private String root = "";

    private String wfStartTimeStamp;

    private Map<String, OutputLocationWriter> inputNameToOutputLocationWriter;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        componentLog = componentContext.getLog();
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);

        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
        wfStartTimeStamp = df.format(dt);

        String rootonworkflowstart = componentContext.getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART);
        boolean onwfstart = Boolean.parseBoolean(rootonworkflowstart);
        if (onwfstart) {
            this.root = componentContext.getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART_ROOT);
        } else {
            this.root = componentContext.getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_ROOT);
        }

        // Parse list of outputLocations and initialize corresponding objects
        String jsonString = componentContext.getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
        inputNameToOutputLocationWriter = new HashMap<String, OutputLocationWriter>();
        // For "old" outputWriters that only have file/directory inputs, the jsonString may not be set
        if (jsonString != null && !jsonString.isEmpty()) {
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.setVisibility(JsonMethod.ALL, Visibility.ANY);
            try {
                OutputLocationList outputList = jsonMapper.readValue(jsonString, OutputLocationList.class);
                for (OutputLocation out : outputList.getOutputLocations()) {
                    OutputLocationWriter writer =
                        new OutputLocationWriter(out.getInputs(), out.getHeader(), out.getFormatString(),
                            out.getHandleExistingFile(), componentLog);
                    for (String input : out.getInputs()) {
                        inputNameToOutputLocationWriter.put(input, writer);
                    }
                    // Initialize the file for the outputLocation. Checks if the file already exists at component start. If yes, the
                    // filename is changed.
                    // NOTE: The handling of a file existing at workflow start (always AUTORENAME) is done here (as it is the same as for
                    // files and directories), whereas the handling of files in later iterations is done in the OutputLocationWriter.
                    String path = out.getFolderForSaving() + File.separator + out.getFilename();
                    path = path.substring(OutputWriterComponentConstants.ROOT_DISPLAY_NAME.length() + 1);
                    path = replacePlaceholder(path, "");
                    File fileToWrite = new File(root + File.separator + path);
                    writer.initializeFile(fileToWrite);

                }
            } catch (IOException e) {
                throw new ComponentException("Failed to parse (internal) configuration (JSON string)", e);
            }
        }
    }

    @Override
    public void processInputs() throws ComponentException {

        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);

        String inputName = componentContext.getInputsWithDatum().iterator().next();
        TypedDatum input = componentContext.readInput(inputName);

        if (input.getDataType().equals(DataType.DirectoryReference) || input.getDataType().equals(DataType.FileReference)) {
            processFileOrDirectory(inputName, input);
        } else if (inputNameToOutputLocationWriter.get(inputName) != null) {
            Map<String, TypedDatum> inputMap = new HashMap<String, TypedDatum>();
            for (String name : componentContext.getInputsWithDatum()) {
                inputMap.put(name, componentContext.readInput(name));
            }
            inputNameToOutputLocationWriter.get(inputName).writeOutput(inputMap, df.format(dt));
        } else {
            componentLog.componentWarn(StringUtils.format("Received value for input '%s' that is"
                + " not associated with any target for simple data types", inputName));
        }

    }

    private void processFileOrDirectory(String inputName, TypedDatum input) throws ComponentException {
        String path = componentContext.getInputMetaDataValue(inputName, OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING)
            + File.separator
            + componentContext.getInputMetaDataValue(inputName, OutputWriterComponentConstants.CONFIG_KEY_FILENAME);
        path = path.substring(OutputWriterComponentConstants.ROOT_DISPLAY_NAME.length() + 1);
        path = replacePlaceholder(path, inputName);
        File fileToWrite = new File(root + File.separator + path);
        if (!fileToWrite.exists()) {
            writeFile(input, root + File.separator + path, inputName);
        } else {
            File possibleFile = autoRename(fileToWrite);
            writeFile(input, possibleFile.getAbsolutePath(), inputName);
        }

        if (input.getDataType().equals(DataType.DirectoryReference)) {
            componentLog.componentInfo(StringUtils.format("Wrote directory '%s' of input '%s' to: %s",
                ((DirectoryReferenceTD) input).getDirectoryName(), inputName, fileToWrite.getAbsolutePath()));
        } else if (input.getDataType().equals(DataType.FileReference)) {
            componentLog.componentInfo(StringUtils.format("Wrote file '%s' of input '%s' to: %s",
                ((FileReferenceTD) input).getFileName(), inputName, fileToWrite.getAbsolutePath()));
        }
    }

    protected File autoRename(File fileToWrite) {
        String folderpath = fileToWrite.getParent();
        String fileName = fileToWrite.getName();
        String extension = "";

        if (fileName.contains(DOT)) {
            extension = fileName.substring(fileName.lastIndexOf(DOT));
            fileName = fileName.substring(0, fileName.lastIndexOf(DOT));
        }
        int i = 1;
        File possibleFile = new File(folderpath, fileName + " (" + i + ")" + extension);

        while (possibleFile.exists()) {
            possibleFile = new File(folderpath, fileName + " (" + ++i + ")" + extension);
        }
        componentLog.componentInfo(StringUtils.format("File '%s' already exists, "
            + "renamed to: %s", fileToWrite.getAbsolutePath(), possibleFile.getAbsolutePath()));
        return possibleFile;
    }

    /**
     * Replaces the placeholder.
     * 
     * @param pathinput contains a input
     * @param currentInputName contains a current input name
     * @return the input without placeholder
     * @throws ComponentException when function was not able to replace all placeholders
     */
    public String replacePlaceholder(String pathinput, String currentInputName) throws ComponentException {
        String output = pathinput;
        output =
            output.replaceAll(escapePlaceholder(OutputWriterComponentConstants.PH_WORKFLOWNAME), componentContext.getWorkflowInstanceName()
                .replaceAll(":", "-"));
        output =
            output.replaceAll(escapePlaceholder(OutputWriterComponentConstants.PH_INPUTNAME), currentInputName);
        output =
            output.replaceAll(escapePlaceholder(OutputWriterComponentConstants.PH_WF_START_TS), wfStartTimeStamp);
        output =
            output.replaceAll(escapePlaceholder(OutputWriterComponentConstants.PH_COMP_NAME), componentContext.getInstanceName());
        output =
            output.replaceAll(escapePlaceholder(OutputWriterComponentConstants.PH_COMP_TYPE), componentContext.getComponentName());
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
        output =
            output.replaceAll(escapePlaceholder(OutputWriterComponentConstants.PH_TIMESTAMP), df.format(dt));
        return output;
    }

    private String escapePlaceholder(String placeholder) {
        placeholder = placeholder.replace(OutputWriterComponentConstants.PH_PREFIX, BACKSLASHES + OutputWriterComponentConstants.PH_PREFIX);
        return placeholder.replace(OutputWriterComponentConstants.PH_SUFFIX, BACKSLASHES + OutputWriterComponentConstants.PH_SUFFIX);
    }

    private void writeFile(TypedDatum input, String path, String inputName) throws ComponentException {
        File file = new File(path);
        String filename = file.getName();
        file = new File(file.getAbsolutePath().replace(File.separator + filename, ""));
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        final File incFileOrDir;
        switch (input.getDataType()) {
        case FileReference:
            incFileOrDir = new File(path);
            try {
                dataManagementService.copyReferenceToLocalFile(((FileReferenceTD) input).getFileReference(),
                    incFileOrDir, componentContext.getDefaultStorageNodeId());
            } catch (IOException e) {
                throw new ComponentException(StringUtils.format("Failed to write file of input '%s' to %s",
                    inputName, incFileOrDir.getAbsolutePath()), e);
            }
            componentLog.componentInfo(StringUtils.format("Wrote file of input '%s' to: %s", inputName, incFileOrDir.getAbsolutePath()));
            break;
        case DirectoryReference:
            incFileOrDir = new File(path);
            File tempDir;
            try {
                tempDir = tempFileService.createManagedTempDir();
            } catch (IOException e) {
                throw new ComponentException("Failed to create temporary directory that is required by Output Writer", e);
            }
            try {
                dataManagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext, ((DirectoryReferenceTD) input),
                    tempDir);
                FileUtils.moveDirectory(new File(tempDir, ((DirectoryReferenceTD) input).getDirectoryName()), incFileOrDir);
            } catch (IOException e) {
                throw new ComponentException(StringUtils.format("Failed to write directory of input '%s' to %s",
                    inputName, incFileOrDir.getAbsolutePath()), e);
            } finally {
                try {
                    tempFileService.disposeManagedTempDirOrFile(tempDir);
                } catch (IOException e) {
                    LogFactory.getLog(getClass()).error("Failed to delete temporary directory", e);
                }
            }
            componentLog.componentInfo(StringUtils.format("Wrote directory of input '%s' to: %s",
                inputName, incFileOrDir.getAbsolutePath()));
            break;
        default:
            break;
        }

    }

    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        // Close all the output streams
        for (OutputLocationWriter out : inputNameToOutputLocationWriter.values()) {
            out.close();
        }
    }

}
