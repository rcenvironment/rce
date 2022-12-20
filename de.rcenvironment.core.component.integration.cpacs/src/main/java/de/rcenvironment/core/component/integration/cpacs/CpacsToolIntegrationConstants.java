/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.cpacs;

import java.util.UUID;

/**
 * Constants for CPACS tool integration.
 * 
 * @author Jan Flink
 */
public final class CpacsToolIntegrationConstants {

    /** Constant. */
    public static final String CPACS_COMPONENT_ID_PREFIX = "de.rcenvironment.integration.cpacs.";

    /** Constant. */
    public static final String[] COMPONENT_IDS = new String[] { CPACS_COMPONENT_ID_PREFIX };

    /** Constant. */
    // TODO review: is there an actual need for this ID to be dynamic?
    public static final String CPACS_TOOL_INTEGRATION_CONTEXT_UID = UUID.randomUUID().toString() + "_CPACS";

    /** Name of the directory with data to be zipped for the user. */
    public static final String RETURN_DIRECTORY_NAME = "ReturnDirectory";

    /** Name of the directory with incoming data. */
    public static final String INCOMING_DIRECTORY_NAME = "IncomingDirectory";

    // /** Name of the temporary file created when zipping the return directory. */
    // public static final String RETURN_ZIP_NAME = "returnzip.tmp";
    //
    // /** Name of the logfile with stdErr in it. */
    // public static final String LOGFILE_NAME_STDERR = "logfile-stderr.txt";
    //
    // /** Name of the logfile with stdOut in it. */
    // public static final String LOGFILE_NAME_STDOUT = "logfile-stdout.txt";

    /** Name of the component name as java-name. */
    public static final String COMPONENTUPDATERFILE = "persistentComponentDescriptionUpdater.xml";

    /** CpacsWrapper file extension. */
    public static final String CPACSWRAPPER_FILEEXTENTION = ".cpacsWrapper";

    /** Constant. */
    public static final String CONSUMECPACS_CONFIGNAME = "consumeCPACS";

    /** Constant. */
    public static final String CONSUMEDIRECTORY_CONFIGNAME = "consumeDirectory";

    /** Constant. */
    public static final String KEY_HAS_TOOLSPECIFIC_INPUT = "hasToolSpecificInput";

    /** Constant. */
    public static final String KEY_TOOL_INPUT_FILENAME = "toolInputFileName";

    /** Constant. */
    public static final String KEY_MAPPING_INPUT_FILENAME = "mappingInputFilename";

    /** Constant. */
    public static final String KEY_TOOLSPECIFICMAPPING_FILENAME = "toolspecificMappingFilename";

    /** Constant. */
    public static final String KEY_TOOLSPECIFICINPUTDATA_FILENAME = "toolspecificInputdataFilename";

    /** Constant. */
    public static final String KEY_CPACS_RESULT_FILENAME = "cpacsResultFilename";

    /** Constant. */
    public static final String KEY_MAPPING_OUTPUT_FILENAME = "mappingOutputFilename";

    /** Constant. */
    public static final String KEY_TOOL_OUTPUT_FILENAME = "toolOutputFilename";

    /** Constant. */
    // To avoid dependency cycles this key exists also in the file ScriptConfigurationPage.java,
    // see CPACS_MOCK_TOOL_OUTPUT_FILENAME!
    public static final String KEY_MOCK_TOOL_OUTPUT_FILENAME = "imitationToolOutputFilename";

    /** Constant. */
    public static final String KEY_CPACS_INITIAL_ENDPOINTNAME = "cpacsInitialEndpointName";

    /** Constant. */
    public static final String KEY_CPACS_OUTGOING_ENDPOINTNAME = "cpacsOutgoingEndpointName";

    /** Constant. */
    public static final String KEY_ALWAYS_RUN = "alwaysRun";

    /** Constant. */
    public static final String FILE_SUFFIX_XML = "xml";

    /** Constant. */
    public static final String FILE_SUFFIX_XSL = "xsl";

    private CpacsToolIntegrationConstants() {

    }

}
