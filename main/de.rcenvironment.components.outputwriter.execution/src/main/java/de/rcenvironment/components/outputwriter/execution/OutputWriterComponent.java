/*
 * Copyright (C) 2006-2014 DLR, Germany
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
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;

/**
 * The default Outputwriter backend.
 * 
 * @author Hendrik Abbenhaus
 * @author Sascha Zur
 * 
 */
public class OutputWriterComponent extends DefaultComponent {

    private static final String BACKSLASHES = "\\";

    private static final String DOT = ".";

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private String root = "";

    private String wfStartTimeStamp;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);

        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
        wfStartTimeStamp = df.format(dt);

        String rootonworkflowstart = componentContext.getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART);
        boolean onwfstart = Boolean.parseBoolean(rootonworkflowstart);
        if (onwfstart) {
            this.root = componentContext.getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART_ROOT);
        } else {
            this.root = componentContext.getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_ROOT);
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        String inputName = componentContext.getInputsWithDatum().iterator().next();
        TypedDatum input = componentContext.readInput(inputName);
        String path = componentContext.getInputMetaDataValue(inputName, OutputWriterComponentConstants.CONFIG_KEY_FOLDERFORSAVING)
            + File.separator
            + componentContext.getInputMetaDataValue(inputName, OutputWriterComponentConstants.CONFIG_KEY_FILENAME);
        path = path.substring(OutputWriterComponentConstants.ROOT_DISPLAY_NAME.length() + 1);
        path = replacePlaceholder(path, inputName);
        File fileToWrite = new File(root + File.separator + path);
        if (!fileToWrite.exists()) {
            writeFile(input, root + File.separator + path);
        } else {
            switch ("AUTORENAME") {
            case "OVERRIDE":
                FileUtils.deleteQuietly(fileToWrite);
                writeFile(input, root + File.separator + path);
                break;
            case "APPEND":
                try {
                    String oldFile = FileUtils.readFileToString(fileToWrite);
                    File temp = new File(root, UUID.randomUUID().toString());
                    dataManagementService.copyReferenceToLocalFile(((FileReferenceTD) input).getFileReference(),
                        temp, componentContext.getDefaultStorageNodeId());
                    String newfile = FileUtils.readFileToString(temp);
                    // TODO review: this is not safe - when writing fails, the old content is
                    // destroyed! - misc_ro
                    FileUtils.deleteQuietly(fileToWrite);
                    FileUtils.writeStringToFile(fileToWrite, oldFile + newfile);
                    FileUtils.deleteQuietly(temp);
                } catch (IOException e) {
                    throw new ComponentException("Failed to append to file " + fileToWrite.getAbsolutePath(), e);
                }
                break;
            case "AUTORENAME":
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
                writeFile(input, possibleFile.getAbsolutePath());
                break;
            default:
                // TODO log a warning etc.? otherwise, add a comment that "no action" is intentional
                // here - misc_ro
                break;
            }
        }
        componentContext.printConsoleLine("Wrote to file '" + fileToWrite.getAbsolutePath() + "': " + inputName,
            ConsoleRow.Type.COMPONENT_OUTPUT);
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
        if (pathinput.isEmpty() || pathinput == null || pathinput.equals("")) {
            throw new ComponentException("No input");
        }
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
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
        output =
            output.replaceAll(escapePlaceholder(OutputWriterComponentConstants.PH_TIMESTAMP), df.format(dt));
        return output;
    }

    private String escapePlaceholder(String placeholder) {
        placeholder = placeholder.replace(OutputWriterComponentConstants.PH_PREFIX, BACKSLASHES + OutputWriterComponentConstants.PH_PREFIX);
        return placeholder.replace(OutputWriterComponentConstants.PH_SUFFIX, BACKSLASHES + OutputWriterComponentConstants.PH_SUFFIX);
    }

    /**
     * Writes a File.
     * 
     * @param input contains content of the file
     * @param path contains a path for the new file
     * 
     * @throws ComponentException on critical failure
     */
    public void writeFile(TypedDatum input, String path) throws ComponentException {
        File file = new File(path);
        String filename = file.getName();
        file = new File(file.getAbsolutePath().replace(File.separator + filename, ""));
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        final File incFile;
        switch (input.getDataType()) {
        case FileReference:
            incFile = new File(path);
            try {
                dataManagementService.copyReferenceToLocalFile(((FileReferenceTD) input).getFileReference(),
                    incFile, componentContext.getDefaultStorageNodeId());
            } catch (IOException e) {
                // TODO add more information to message?
                throw new ComponentException("Failed to write File output: " + incFile.getAbsolutePath(), e);
            }
            break;
        case DirectoryReference:
            incFile = new File(path);
            File tempFile = new File(incFile.getParentFile(), "__temp__");
            try {
                dataManagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext, ((DirectoryReferenceTD) input),
                    tempFile);
                FileUtils.moveDirectory(new File(tempFile, ((DirectoryReferenceTD) input).getDirectoryName()), incFile);
                FileUtils.deleteDirectory(tempFile);
            } catch (IOException e) {
                // TODO add more information to message?
                throw new ComponentException("Failed to write Directory output: " + incFile.getAbsolutePath(), e);
            }
            break;
        default:
            break;
        }

    }

}
