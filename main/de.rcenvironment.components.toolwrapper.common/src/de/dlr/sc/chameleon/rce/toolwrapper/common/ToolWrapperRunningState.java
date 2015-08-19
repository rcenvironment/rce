/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.dlr.sc.chameleon.rce.toolwrapper.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Running state of the toolwrapper.
 *
 * @author Markus Kunde
 */
@Deprecated
public class ToolWrapperRunningState extends CpacsRunningState implements Serializable {

    /** serial uid. */
    private static final long serialVersionUID = -805023503320819923L;
    
    /** toolInput.xml. */
    private String toolIn;
    
    /** toolOutput.xml. */
    private String toolOut;
    
    /** logfile-stdout.txt. */
    private String outLog;
    
    /** logfile-stderr.txt. */
    private String errLog;
        
    /**
     * Incoming directory.
     * 
     * Key: String is logical path representation, e. g., "c:\temp\file.txt
     * Value: reference of file in datamanagement context
     * 
     */
    private Map<String, String> incomingDirectory = new HashMap<String, String>();
    
    /**
     * Outgoing directory.
     * 
     * Key: String is logical path representation, e. g., "c:\temp\file.txt
     * Value: reference of file in datamanagement context
     * 
     */
    private Map<String, String> outgoingDirectory = new HashMap<String, String>();
    
    /**
     * Constructor.
     * 
     */
    public ToolWrapperRunningState() {}

    
    /**
     * Returns tool input.
     * 
     * @return Toolinput
     */
    public String getToolIn() {
        return toolIn;
    }

    
    /**
     * Sets tool input.
     * 
     * @param toolIncoming tool input
     */
    public void setToolIn(String toolIncoming) {
        this.toolIn = toolIncoming;
    }

    
    /**
     * Returns tool log output.
     * 
     * @return Tooloutput log
     */
    public String getToolLogOut() {
        return outLog;
    }
    
    /**
     * Sets tool log input.
     * 
     * @param toolIncomingLog tool input log
     */
    public void setToolLogOutIn(String toolIncomingLog) {
        this.outLog = toolIncomingLog;
    }
    
    /**
     * Returns tool err log output.
     * 
     * @return Tooloutput err log
     */
    public String getToolLogErr() {
        return errLog;
    }
    
    /**
     * Sets tool err log input.
     * 
     * @param toolIncomingLog tool input err log
     */
    public void setToolLogErrIn(String toolIncomingLog) {
        this.errLog = toolIncomingLog;
    }
    
    /**
     * Returns tool output.
     * 
     * @return Tooloutput
     */
    public String getToolOut() {
        return toolOut;
    }

    
    /**
     * Sets tool output.
     * 
     * @param toolOutgoing tool output
     */
    public void setToolOut(String toolOutgoing) {
        this.toolOut = toolOutgoing;
    }

    
    /**
     * Returns all files of the incoming directory.
     * 
     * @return map of all files in incoming directory. 
     * Key is logical path of file, Value is datamanagement-reference of file
     */
    public Map<String, String> getIncomingDirectory() {
        return incomingDirectory;
    }

    /**
     * Sets an entry regarding a file in incoming directory.
     * 
     * @param key logical path of file
     * @param value datamanagement-reference of file
     */
    public void setIncomingDirectoryEntry(final String key, final String value) {
        incomingDirectory.put(key, value);
    }

    /**
     * Returns all files of the outgoing directory.
     * 
     * @return map of all files in outgoing directory. 
     * Key is logical path of file, Value is datamanagement-reference of file
     */
    public Map<String, String> getOutgoingDirectory() {
        return outgoingDirectory;
    }

    /**
     * Sets an entry regarding a file in outgoing directory.
     * 
     * @param key logical path of file
     * @param value datamanagement-reference of file
     */
    public void setOutgoingDirectoryEntry(final String key, final String value) {
        outgoingDirectory.put(key, value);
    }
}
