/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.dlr.sc.chameleon.rce.toolwrapper.common;

import java.util.ResourceBundle;

import de.rcenvironment.core.component.api.ComponentConstants;


/**
 * Constants used in ToolWrapper class.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 */
public interface CpacsComponentConstants {

    /**
     * The resources to read from.
     */
    ResourceBundle RES = ResourceBundle.getBundle("resources.CpacsComponent");
    
    /** Filename of the output mapping file for that tool. */
    String MAPPING_OUTPUT_FILENAME = RES.getString("MAPPING_OUTPUT_FILENAME");
    
    /** Filename of the raw output mapping XSLT file for that tool. */
    String MAPPING_OUTPUT_FILENAME_RAW_XSLT = RES.getString("MAPPING_OUTPUT_FILENAME_RAW_XSLT");
    
    /** Filename of the Tool Output File. */
    String TOOL_OUTPUT_FILENAME = RES.getString("TOOL_OUTPUT_FILENAME");

    /** The filename of the actual CPACS data record from CPACS MC_plugin. */
    String CPACS_INITIAL_FILENAME = RES.getString("CPACS_INITIAL_FILENAME");

    /** Filename of the input mapping file. */
    String MAPPING_INPUT_FILENAME = RES.getString("MAPPING_INPUT_FILENAME");
    
    /** Filename of the raw input mapping XSLT file. */
    String MAPPING_INPUT_FILENAME_RAW_XSLT = RES.getString("MAPPING_INPUT_FILENAME_RAW_XSLT");
    
    /** Filename of the Tool input file. */
    String TOOL_INPUT_FILENAME = RES.getString("TOOL_INPUT_FILENAME"); 

    /** Directoryname for Toolinput. */
    String TOOL_INPUT_DIRNAME = RES.getString("TOOL_INPUT_DIRNAME");

    /** Directoryname for Tooloutput. */
    String TOOL_OUTPUT_DIRNAME = RES.getString("TOOL_OUTPUT_DIRNAME");
    
    /** Filename of the result CPACS file. */
    String CPACS_RESULT_FILENAME = RES.getString("CPACS_RESULT_FILENAME");

    /** Filename for toolspecific input data xml file. */
    String TOOLSPECIFICINPUTDATA_FILENAME = RES.getString("TOOLSPECIFICINPUTDATA_FILENAME");

    /** Filename of the mapping file for toolSpecificInputData. */
    String TOOLSPECIFICMAPPING_FILENAME = RES.getString("TOOLSPECIFICMAPPING_FILENAME");
    
    /** Filename of the raw mapping XSLT file for toolSpecificInputData. */
    String TOOLSPECIFICMAPPING_FILENAME_RAW_XSLT = RES.getString("TOOLSPECIFICMAPPING_FILENAME_RAW_XSLT");  
    
    /** Name of the directory with data to be zipped for the user. */
    String RETURN_DIRECTORY_NAME = RES.getString("RETURN_DIRECTORY_NAME");
    
    /** Name of the directory with incoming data. */
    String INCOMING_DIRECTORY_NAME = RES.getString("INCOMING_DIRECTORY_NAME");
    
    /** Name of the temporary file created when zipping the return directory. */
    String RETURN_ZIP_NAME = RES.getString("RETURN_ZIP_NAME");
    
    /** Name of the logfile with stdErr in it. */
    String LOGFILE_NAME_STDERR = RES.getString("LOGFILE_NAME_STDERR"); 
    
    /** Name of the logfile with stdOut in it. */
    String LOGFILE_NAME_STDOUT = RES.getString("LOGFILE_NAME_STDOUT"); 

    /** Identifier of the Joiner component. */
    String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "oldcpacswrapper";
    
    /** Identifiers of the Joiner component. */
    String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.dlr.sc.chameleon.rce.toolwrapper.component.ToolWrapper" };
    
    /** Name of the component name as java-name. */
    String COMPONENTUPDATERFILE = RES.getString("COMPONENTUPDATERFILE");
    
    /** CpacsWrapper file extension. */
    String CPACSWRAPPER_FILEEXTENTION = RES.getString("CPACSWRAPPER_FILEEXTENTION");
    
    /** Default ChameleonWrapper configuration directory. */
    String CPACSWRAPPER_CONFIGDIR = RES.getString("CPACSWRAPPER_CONFIGDIR");
    
    /** Constant. */
    String CONSUMECPACS_CONFIGNAME = "consumeCPACS";
    
    /** Constant. */
    String CONSUMEDIRECTORY_CONFIGNAME = "consumeDirectory";

    /** Dummy method. */
    void getString();
}
