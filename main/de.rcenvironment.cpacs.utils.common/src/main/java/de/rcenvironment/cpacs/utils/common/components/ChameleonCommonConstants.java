/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.utils.common.components;

/**
 * Common constants e.g. for preferences keys.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public interface ChameleonCommonConstants {

    /** Constant. */
    String CHAMELEON_CPACS_NAME = "CPACS";

    /** Constant. */
    String DIRECTORY_CHANNELNAME = "Directory";

    /** Constant. */
    String CHAMELEON_CPACS_FILENAME = "cpacs.xml";

    /** Constant. */
    String XML_APPENDIX_FILENAME = ".xml";

    /** Constant. */
    String XML_PLAIN_FILENAME = "plain.xml";

    /**
     * Channel metadata for variable xpath.
     */
    String CHANNEL_XPATH = "variable.xpath";

    /** ID of input pane. */
    String ID_INPUT_PANE = "default";

    /** ID of output pane. */
    String ID_OUTPUT_PANE = "default";

    /** ID of input pane. */
    String ID_DIRECTORY_PANE = "directory";

    /**
     * Returns nothing.
     * 
     */
    void getString();

}
