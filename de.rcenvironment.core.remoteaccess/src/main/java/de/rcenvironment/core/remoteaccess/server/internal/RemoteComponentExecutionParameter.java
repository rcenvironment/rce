/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.server.internal;

import java.io.File;

/**
 * A container class for all the parameters necessary to execute a remote tool or workflow.
 *
 * @author Brigitte Boden
 */
public class RemoteComponentExecutionParameter {

    private String toolId;

    private String toolVersion;

    private String toolNodeId;

    private String sessionToken;

    private File inputFilesDir;

    private File outputFilesDir;

    private String dynInputDesc;

    private String dynOutputDesc;

    private String notRequiredInputs;

    private boolean uncompressedUpload;
    
    private boolean simpleDescriptionFormat;

    public RemoteComponentExecutionParameter(String toolId, String toolVersion, String toolNodeId,
        String sessionToken, File inputFilesDir, File outputFilesDir, String dynInputDesc, String dynOutputDesc,
        String notRequiredInputs, boolean uncompressedUpload, boolean simpleDescriptionFormat) {
        this.toolId = toolId;
        this.toolVersion = toolVersion;
        this.toolNodeId = toolNodeId;
        this.sessionToken = sessionToken;
        this.inputFilesDir = inputFilesDir;
        this.outputFilesDir = outputFilesDir;
        this.dynInputDesc = dynInputDesc;
        this.dynOutputDesc = dynOutputDesc;
        this.notRequiredInputs = notRequiredInputs;
        this.uncompressedUpload = uncompressedUpload;
        this.simpleDescriptionFormat = simpleDescriptionFormat;
    }

    
    public String getSessionToken() {
        return sessionToken;
    }

    public String getNotRequiredInputs() {
        return notRequiredInputs;
    }

    public String getToolId() {
        return toolId;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public String getToolNodeId() {
        return toolNodeId;
    }

    public File getInputFilesDir() {
        return inputFilesDir;
    }

    public File getOutputFilesDir() {
        return outputFilesDir;
    }

    public String getDynInputDesc() {
        return dynInputDesc;
    }

    public String getDynOutputDesc() {
        return dynOutputDesc;
    }

    public boolean isUncompressedUpload() {
        return uncompressedUpload;
    }
    
    public boolean isSimpleDescriptionFormat() {
        return simpleDescriptionFormat;
    }

}
