/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants.HandleExistingFile;
import de.rcenvironment.components.outputwriter.common.OutputWriterValidatorHelper;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A class for collecting simple input Data for an outputWriter and writing them into a formatted file.
 *
 * @author Brigitte Boden
 * @author Dominik Schneider
 */
public class OutputLocationWriter {

    private static final String ITERATION = " - iteration ";

    private static final String DOT = ".";

    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss-S";

    private static final String LINE_SEP = System.getProperty("line.separator");

    private File outputFile;

    private String basicName;

    private FileOutputStream outputStream;

    // private List<String> inputNames; May be useful for future

    private final String header;

    private final String formatString;

    private final HandleExistingFile handleExistingFile;

    private final ComponentLog componentLog;

    // Iteration counter; used for AUTORENAME option
    private long iterations;

    protected OutputLocationWriter(List<String> inputNames, String header, String formatString,
        HandleExistingFile handle, ComponentLog componentLog) {
        this.header = header;
        this.formatString = formatString;
        this.handleExistingFile = handle;
        this.iterations = 0;
        // this.inputNames = inputNames;
        this.componentLog = componentLog;
    }

    /**
     * 
     * In case of the APPEND or OVERRIDE option, the file is opened here and kept open.
     * 
     * @throws ComponentException if creating/initializing the file failed
     *
     */
    protected void initializeFile(File fileToWrite) throws ComponentException {
        this.basicName = fileToWrite.getName();
        // Check for invalid filename
        List<String> forbiddenFilenames = Arrays.asList(OutputWriterComponentConstants.PROBLEMATICFILENAMES_WIN);
        if (forbiddenFilenames.contains(basicName) || basicName.contains("/") || basicName.contains("\\")) {
            throw new ComponentException(StringUtils.format("Failed to write file because '%s' "
                + "is a forbidden filename", basicName));
        }
        this.outputFile = fileToWrite;
        if (handleExistingFile == HandleExistingFile.APPEND || handleExistingFile == HandleExistingFile.OVERRIDE) {
            if (fileToWrite.exists()) {
                fileToWrite = autoRename(fileToWrite);
                this.outputFile = fileToWrite;
            }
            try {
                outputStream = FileUtils.openOutputStream(fileToWrite, true);

                // In case of the APPEND option, write the file header
                if (handleExistingFile == HandleExistingFile.APPEND && !header.isEmpty()) {
                    Date dt = new Date();
                    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                    String timeStamp = df.format(dt);
                    final String cleanedHeaderString = removeNewlines(header);
                    FileUtils.writeStringToFile(outputFile,
                        OutputWriterValidatorHelper.formatHeader(cleanedHeaderString, timeStamp, 0) + LINE_SEP, true);
                }

            } catch (IOException e) {
                throw new ComponentException("Failed to create/initialize file used as target for simple data types: "
                    + outputFile.getAbsolutePath(), e);
            }
        }
        componentLog.componentInfo("Created and initialized file used as target for simple data types: " + outputFile.getAbsolutePath());
    }

    protected void writeOutput(Map<String, TypedDatum> inputMap, String timestamp, int executionCount) throws ComponentException {
        final String cleanedFormatString = removeNewlines(formatString);
        final String cleanedHeaderString = removeNewlines(header);
        String outputString = OutputWriterValidatorHelper.replacePlaceholders(cleanedFormatString, inputMap, timestamp, executionCount);
        iterations++;
        try {
            if (handleExistingFile == HandleExistingFile.APPEND) {
                // For option APPEND, the file is already open. Append the outputString.
                FileUtils.writeStringToFile(outputFile, outputString, true);

            } else if (handleExistingFile == HandleExistingFile.OVERRIDE) {
                if (!cleanedHeaderString.isEmpty()) {
                    // For option OVERRIDE, write to the existing file without appending.
                    Date dt = new Date();
                    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                    String timeStamp = df.format(dt);
                    FileUtils.writeStringToFile(outputFile,
                        OutputWriterValidatorHelper.formatHeader(cleanedHeaderString, timeStamp, executionCount) + LINE_SEP + outputString,
                        false);
                } else {
                    FileUtils.writeStringToFile(outputFile, outputString, false);
                }

            } else if (handleExistingFile == HandleExistingFile.AUTORENAME) {
                if (!cleanedHeaderString.isEmpty()) {
                    // For option AUTORENAME, create a file with new name and write to it.
                    Date dt = new Date();
                    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                    String timeStamp = df.format(dt);
                    outputFile = getNamePerIteration(outputFile);
                    FileUtils.writeStringToFile(outputFile,
                        OutputWriterValidatorHelper.formatHeader(cleanedHeaderString, timeStamp, executionCount) + LINE_SEP + outputString,
                        false);
                } else {
                    outputFile = getNamePerIteration(outputFile);
                    FileUtils.writeStringToFile(outputFile, outputString, false);
                }
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to write file used as target for simple data types: " + outputFile.getAbsolutePath(), e);
        }

        componentLog.componentInfo(StringUtils.format("Wrote '%s' to: %s", inputMap, outputFile.getAbsolutePath()));
    }

    protected File getNamePerIteration(File fileToWrite) {
        String folderpath = fileToWrite.getParent();
        String fileName = basicName;
        String extension = "";

        if (fileName.contains(DOT)) {
            extension = fileName.substring(fileName.lastIndexOf(DOT));
            fileName = fileName.substring(0, fileName.lastIndexOf(DOT));
        }
        File possibleFile = new File(folderpath, fileName + ITERATION + iterations + extension);

        if (possibleFile.exists()) {
            possibleFile = autoRename(possibleFile);
        }
        return possibleFile;
    }

    protected void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).error("Failed to close output stream: ", e);
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

    private String removeNewlines(String input) {
        return input.replaceAll("\\n", "").replaceAll("\\r", "");
    }

}
