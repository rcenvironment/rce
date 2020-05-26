/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.common;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants.HandleExistingFile;

/**
 * Class for OutputLocation Information. The filename is also used as the key in the outputLocation List.
 *
 * @author Brigitte Boden
 */
public class OutputLocation {

    // Randomly generated identifier, which is also used as the name of the input group corresponding to this location.
    private final String identifier;

    private String filename;

    private String folderForSaving;

    private String header;

    private String formatString;

    private HandleExistingFile handleExistingFile;

    private List<String> inputs;

    /**
     * 
     * Default constructor; required by JSON.
     *
     */
    public OutputLocation() {
        identifier = "";
    }

    public OutputLocation(String filename, String folderForSaving, String header, String formatString,
        HandleExistingFile handleExistingFile,
        List<String> inputs) {
        this(UUID.randomUUID().toString(), filename, folderForSaving, header, formatString, handleExistingFile, inputs);
    }

    public OutputLocation(String identifier, String filename, String folderForSaving, String header, String formatString,
        HandleExistingFile handleExistingFile,
        List<String> inputs) {
        this.identifier = identifier;
        this.filename = filename;
        this.folderForSaving = folderForSaving;
        this.header = header;
        this.formatString = formatString;
        this.handleExistingFile = handleExistingFile;
        this.inputs = inputs;
    }

    public String getFilename() {
        return filename;
    }

    @JsonIgnore
    public String getGroupId() {
        return identifier;
    }

    public String getFolderForSaving() {
        return folderForSaving;
    }

    public String getHeader() {
        return header;
    }

    public String getFormatString() {
        return formatString;
    }

    public HandleExistingFile getHandleExistingFile() {
        return handleExistingFile;
    }

    public List<String> getInputs() {
        return inputs;
    }

}
