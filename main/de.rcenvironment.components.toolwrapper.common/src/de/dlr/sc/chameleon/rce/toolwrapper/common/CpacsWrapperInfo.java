/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.dlr.sc.chameleon.rce.toolwrapper.common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reads and checks informations from a tool to wrap via .cpacsWrapper file.
 *
 * @author Markus Litz
 * @author Markus Kunde
 */
public class CpacsWrapperInfo {

    private static final Log LOGGER = LogFactory.getLog(CpacsWrapperInfo.class);
    
    private static final String STR_TOOLNAME = "ToolName";
    private static final String STR_TOOLDESCIPTION = "ToolDescription";
    private static final String STR_TOOLICON = "ToolIcon";
    private static final String STR_TOOLVERSION = "ToolVersion";
    private static final String STR_TOOLDIRECTORY = "ToolDirectory";
    private static final String STR_TOOLCMD = "ToolCommandline";
    private static final String STR_TEMPDIR = "Tempdirectory";
    private static final String STR_ALWAYSRUN = "Alwaysrun";
    private static final String STR_DELTEMPDIR = "Deletetempdirectory";
    private static final String STR_TOOLSPECIFIC = "Toolspecificinputfile";
    private static final String STR_TOOLPOSTCOMMAND = "ToolPostcommand";
    private static final String STR_TOOLPRECOMMAND = "ToolPrecommand";
    private static final String STR_IGNOREFILEPATTERN = "IgnoreFiles";
    
    private String toolName = null;
    private String toolDescription = null;
    private String toolIcon16 = null;
    private String toolIcon32 = null;
    private String toolVersion = null;
    private String toolDirectory = null;
    private String toolCommandline = null;
    private String tempDirectory = null;
    private boolean alwaysrun = false;
    private boolean deletetempdirectory = false;
    private boolean toolspecificinputfile = true;
    private String toolPostcommand = null;
    private String toolPrecommand = null;
    
    private Properties toolProperties = null;
    
    private IgnoreToolDirFileFilter ignoreFilter = null;

    
    /**
     * Constructor.
     * Deserialize from existing file.
     * 
     * @param fileName The file to read
     * @throws IOException  if error occurs on access to file
     */
    public CpacsWrapperInfo(final String fileName) throws IOException {
        toolProperties = new Properties();
        BufferedInputStream stream;
        stream = new BufferedInputStream(new FileInputStream(fileName));
        toolProperties.load(stream);
        stream.close();
        
        if (!parseToolInfos()) {
            throw new IllegalArgumentException("File contains wrong content and cannot be parsed."); 
        }
    }
    
    /**
     * Constructor.
     * Deserialize from a stream instead of from a file.
     * 
     * @param is The stream to read
     * @throws IOException if error occurs on access to inputstream
     */
    public CpacsWrapperInfo(final InputStream is) throws IOException {
        toolProperties = new Properties();
        toolProperties.load(is);
        if (!parseToolInfos()) {
            throw new IllegalArgumentException("Inputstream contains wrong content and cannot be parsed.");
        }
    }
    

    /**
     * Helper to parse the contents of a file or stream.
     * @return Success or not
     */
    private boolean parseToolInfos() {
        // Check if all needed values are there
        if (!checkFields(toolProperties)) {
            return false;
        }
        
        // Fill all needed values 
        fillFields(toolProperties);
        return true;
    }

    /**
     * Checks for mandatory values.
     * 
     * @param tool The ToolInformation.
     * @return true if all necessary values are in the file.
     */
    private boolean checkFields(final Properties tool) {
        if (tool.containsKey(STR_TOOLNAME)
                && tool.containsKey(STR_TOOLDESCIPTION)
                && tool.containsKey(STR_TOOLICON)
                && tool.containsKey(STR_TOOLVERSION)
                && tool.containsKey(STR_TOOLDIRECTORY)
                && tool.containsKey(STR_TOOLCMD)
                && tool.containsKey(STR_ALWAYSRUN)
                && tool.containsKey(STR_DELTEMPDIR)
                && tool.containsKey(STR_TOOLSPECIFIC)
                && tool.containsKey(STR_TOOLPOSTCOMMAND)
                && tool.containsKey(STR_TOOLPRECOMMAND)) {
            return true;
        }
        LOGGER.error("At least one field is missing in the tooldescription of one .cpacsWrapper files!");
        return false;
    }
    
    /**
     * Set this component's values.
     * @param tool The properties as read in
     */
    private void fillFields(final Properties tool) {
        toolName = tool.getProperty(STR_TOOLNAME);
        toolDescription = tool.getProperty(STR_TOOLDESCIPTION);
        toolIcon16 = tool.getProperty(STR_TOOLICON);
        toolIcon32 = tool.getProperty(STR_TOOLICON);
        toolVersion = tool.getProperty(STR_TOOLVERSION);
        toolDirectory = tool.getProperty(STR_TOOLDIRECTORY);
        toolCommandline = tool.getProperty(STR_TOOLCMD);
        tempDirectory = tool.getProperty(STR_TEMPDIR);
        alwaysrun = Boolean.parseBoolean(tool.getProperty(STR_ALWAYSRUN));
        deletetempdirectory = Boolean.parseBoolean(tool.getProperty(STR_DELTEMPDIR));
        toolspecificinputfile = Boolean.parseBoolean(tool.getProperty(STR_TOOLSPECIFIC));
        toolPostcommand = tool.getProperty(STR_TOOLPOSTCOMMAND);
        toolPrecommand = tool.getProperty(STR_TOOLPRECOMMAND);
        String rawPattern = tool.getProperty(STR_IGNOREFILEPATTERN);
        if (rawPattern != null) {
            ignoreFilter  = new IgnoreToolDirFileFilter(rawPattern);
        } else {
            ignoreFilter  = new IgnoreToolDirFileFilter(new String());
        }
    }
    
    public final String getToolName() {
        return toolName;
    }
    
    public final String getToolDescription() {
        return toolDescription;
    }
    
    public final String getToolIcon16() {
        return toolIcon16;
    }

    public final String getToolIcon32() {
        return toolIcon32;
    }
    
    public final String getToolVersion() {
        return toolVersion;
    }

    public final String getToolDirectory() {
        return toolDirectory;
    }
    
    public final String getToolCommandline() {
        return toolCommandline;
    }
    
    public final String getTempdirectory() {
        return tempDirectory;
    }

    public final void setTempdirectory(String tempdirectory) {
        tempDirectory = tempdirectory;
    }

    public final boolean isAlwaysrun() {
        return alwaysrun;
    }
    
    public final boolean isDeletetempdirectory() {
        return deletetempdirectory;
    }
    
    /**
     * Returns if tool has toolspecific input file.
     * 
     * @return true if inputfile is there
     */
    public final boolean hasToolspecificinputfile() {
        return toolspecificinputfile;
    }
    
    public final String getToolPostcommand() {
        return toolPostcommand;
    }

    public final String getToolPrecommand() {
        return toolPrecommand;
    }
    
    public final IgnoreToolDirFileFilter getIgnoredFiles() {
        return ignoreFilter;
    }
    
}
