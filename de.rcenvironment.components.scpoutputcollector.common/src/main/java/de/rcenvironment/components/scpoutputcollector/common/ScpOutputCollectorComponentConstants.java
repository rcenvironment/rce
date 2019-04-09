/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.scpoutputcollector.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants for ScpOutputCollector.
 * 
 * @author Brigitte Boden
 * 
 */
public final class ScpOutputCollectorComponentConstants {

    /** Identifier of component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "scpoutputcollector";

    /** Configuration key for upload directory. **/
    public static final String DOWNLOAD_DIRECTORY_CONFIGURATION_KEY = "DownloadDirectory";

    /** Configuration key for inputs description file. **/
    public static final String OUTPUTS_DESCRIPTION_FILE_CONFIGURATION_KEY = "OutputsDescriptionFile";

    /** Configuration key indicating if the file/directories for download should be uncompressed. **/
    public static final String UNCOMPRESSED_DOWNLOAD_CONFIGURATION_KEY = "UncompressedDownload";
    
    /** Configuration key stating if the simple output description format should be used. **/
    public static final String SIMPLE_DESCRIPTION_CONFIGURATION_KEY = "SimpleDescriptionFormat";

    private ScpOutputCollectorComponentConstants() {

    }

}
