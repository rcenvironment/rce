/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.scpinputloader.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * ScpInputLoaderComponentConstants class.
 * 
 * @author Brigitte Boden
 */
public final class ScpInputLoaderComponentConstants {

    /** Identifier of the component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "scpinputloader";

    /** Identifiers of the component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.components.scpinputloader.execution.ScpInputLoaderComponent_SCP Input Loader" };

    /** Configuration key for upload directory. **/
    public static final String UPLOAD_DIRECTORY_CONFIGURATION_KEY = "UploadDirectory";
    
    /** Configuration key for inputs description file. **/
    public static final String INPUTS_DESCRIPTION_FILE_CONFIGURATION_KEY = "InputsDescriptionFile";
    
    /** Configuration key stating if input files and directories are compressed. **/
    public static final String UNCOMPRESSED_UPLOAD_CONFIGURATION_KEY = "UncompressedUpload";
    
    /** Configuration key stating if the simple input description format should be used. **/
    public static final String SIMPLE_DESCRIPTION_CONFIGURATION_KEY = "SimpleDescriptionFormat";

    private ScpInputLoaderComponentConstants() {

    }

}
