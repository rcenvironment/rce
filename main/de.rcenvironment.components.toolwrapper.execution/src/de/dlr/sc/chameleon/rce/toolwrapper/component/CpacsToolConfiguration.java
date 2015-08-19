/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.dlr.sc.chameleon.rce.toolwrapper.component;

import java.io.File;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsComponentConstants;



/**
 * Holds configuration of server.
 *
 * @author Markus Kunde
 * @author Arne Bachmann
 */
public class CpacsToolConfiguration {
    
    // dirs
    private String toolInputDir;
    private String toolOutputDir;
    private String tempDir;
    private String returnDir;
    private String incomingDir;
    private String toolOriginDir;

    // filenames
    private String initialCpacs;
    private String toolInput;
    private String toolOutput;
    private String cpacsResult;
    private String inputMapping;
    private String inputMappingRawXslt;
    private String outputMapping;
    private String outputMappingRawXslt;
    private String toolspecificInputData;
    private String toolspecificInputMapping;
    private String toolspecificInputMappingRawXslt;
    private String logFileNameStdErr;
    private String logFileNameStdOut;
    private String returnZip;
    
    /**
     * Constructor.
     * 
     * @param toolTempDir Tool temp dir
     * @param aToolOriginDir tool origin dir
     */
    public CpacsToolConfiguration(final String toolTempDir, final String aToolOriginDir) {
        // dirs
        toolInputDir = toolTempDir + File.separator + CpacsComponentConstants.TOOL_INPUT_DIRNAME;
        toolOutputDir = toolTempDir + File.separator + CpacsComponentConstants.TOOL_OUTPUT_DIRNAME;
        tempDir = toolTempDir;
        returnDir = toolTempDir + File.separator + CpacsComponentConstants.RETURN_DIRECTORY_NAME;
        incomingDir = toolTempDir + File.separator + CpacsComponentConstants.INCOMING_DIRECTORY_NAME;
        toolOriginDir = aToolOriginDir; 

        // filenames
        initialCpacs = toolTempDir + File.separator + CpacsComponentConstants.CPACS_INITIAL_FILENAME;
        toolInput = toolInputDir + File.separator + CpacsComponentConstants.TOOL_INPUT_FILENAME;
        toolOutput = toolOutputDir + File.separator + CpacsComponentConstants.TOOL_OUTPUT_FILENAME;
        cpacsResult = toolTempDir + File.separator + CpacsComponentConstants.CPACS_RESULT_FILENAME;
        inputMapping = toolTempDir + File.separator + CpacsComponentConstants.MAPPING_INPUT_FILENAME;
        inputMappingRawXslt = toolTempDir + File.separator + CpacsComponentConstants.MAPPING_INPUT_FILENAME_RAW_XSLT;
        outputMapping = toolTempDir + File.separator + CpacsComponentConstants.MAPPING_OUTPUT_FILENAME;
        outputMappingRawXslt = toolTempDir + File.separator + CpacsComponentConstants.MAPPING_OUTPUT_FILENAME_RAW_XSLT;
        toolspecificInputData = toolTempDir + File.separator + CpacsComponentConstants.TOOLSPECIFICINPUTDATA_FILENAME;
        toolspecificInputMapping = toolTempDir + File.separator + CpacsComponentConstants.TOOLSPECIFICMAPPING_FILENAME;
        toolspecificInputMappingRawXslt = toolTempDir + File.separator + CpacsComponentConstants.TOOLSPECIFICMAPPING_FILENAME_RAW_XSLT;
        logFileNameStdErr = toolTempDir + File.separator + CpacsComponentConstants.LOGFILE_NAME_STDERR;
        logFileNameStdOut = toolTempDir + File.separator + CpacsComponentConstants.LOGFILE_NAME_STDOUT;
        returnZip = toolTempDir + File.separator + CpacsComponentConstants.RETURN_ZIP_NAME;
    }
    
    
    /**
     * Returns the temp directory.
     * 
     * @return String with path to temp dir
     */
    public final String getTempDir() {
        return tempDir;
    }

    /**
     * Returns the input directory of the tool.
     * 
     * @return String with path to input dir
     */
    public String getInputDir() {
        return toolInputDir;
    }
    
    /**
     * Returns the output directory of the tool.
     * 
     * @return String with path to output dir
     */
    public String getOutputDir() {
        return toolOutputDir;
    }
    
    /**
     * Returns the origin tool directory.
     * 
     * @return String with path to origin tool dir
     */
    public String getToolOriginDir() {
        return toolOriginDir;
    }
    
    /**
     * Returns path to initial CPACS file.
     * 
     * @return String with path to cpacs initial
     */
    public String getCpacsInitial() {
        return initialCpacs;
    }
    
    /**
     * Returns path to tool input file.
     * 
     * @return String with path to tool input file
     */
    public String getToolInput() {
        return toolInput;
    }
    
    /**
     * Returns path to tool output file.
     * 
     * @return String with path to  tool output file
     */
    public String getToolOutput() {
        return toolOutput;
    }
    
    /**
     * Returns path to resulting CPACS file.
     * 
     * @return String with path to cpacs result
     */
    public String getCpacsResult() {
        return cpacsResult;
    }

    /**
     * Returns path classic input mapping file.
     * 
     * @return String with path to input mapping file
     */
    public String getInputMapping() {
        return inputMapping;
    }
    
    /**
     * Returns path to xslt input mapping file.
     * 
     * @return String with path to xslt input mapping file
     */
    public String getInputMappingRawXslt() {
        return inputMappingRawXslt;
    }
    
    /**
     * Returns path to classic output mapping file.
     * 
     * @return String with path to output mapping file
     */
    public String getOutputMapping() {
        return outputMapping;
    }
    
    /**
     * Returns path to xslt output mapping file.
     * 
     * @return String with path to xslt output mapping file
     */
    public String getOutputMappingRawXslt() {
        return outputMappingRawXslt;
    }

    /**
     * Returns path to return directory.
     * 
     * @return String with path to return directory
     */
    public String getReturnDirectory() {
        return returnDir;
    }
    
    /**
     * Returns path to incoming directory.
     * 
     * @return String with path to incoming directory
     */
    public String getIncomingDirectory() {
        return incomingDir;
    }
    
    /**
     * Returns path to tool specific input data file.
     * 
     * @return String with path to tool specific input data file
     */
    public String getToolspecificInputData() {
        return toolspecificInputData;
    }
    
    /**
     * Returns path to classic tool specific input data mapping file.
     * 
     * @return String with path to classic tool specific input data mapping file
     */
    public String getToolspecificInputMapping() {
        return toolspecificInputMapping;
    }
    
    /**
     * Returns path to xslt tool specific input data mapping file.
     * 
     * @return String with path to xslt tool specific input data mapping file
     */
    public String getToolspecificInputMappingRawXslt() {
        return toolspecificInputMappingRawXslt;
    }
    
    /**
     * Return the full path to the logfile that contains the error messages from the tool.
     * 
     * @return the full path to the logfile that contains the error messages from the tool
     */
    public String getLogFileNameStdErr() {
        return logFileNameStdErr;
    }
    
    /**
     * Return the full path to the logfile that contains the stdOut messages from the tool.
     * 
     * @return the full path to the logfile that contains the stdOut messages from the tool
     */
    public String getLogFileNameStdOut() {
        return logFileNameStdOut;
    }
    
    /**
     * Return the full path to the return zip file name.
     * 
     * @return the full path to the return zip file name
     */
    public String getReturnZipName() {
        return returnZip;
    }
}
