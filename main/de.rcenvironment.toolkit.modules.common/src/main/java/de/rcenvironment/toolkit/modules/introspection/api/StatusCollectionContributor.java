/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.introspection.api;

import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * SPI for classes that contribute information about their runtime state and/or unfinished operations. To disable contributions to either
 * category, return "null" from the corresponding get...Description() method.
 * 
 * @author Robert Mischke
 */
public interface StatusCollectionContributor {

    /**
     * @return the human-readable title or description for the standard state; return null to disable standard state contribution
     */
    String getStandardDescription();

    /**
     * Generates output describing the current standard state, if applicable.
     * 
     * @param receiver the receiver to send the output to
     */
    void printDefaultStateInformation(TextLinesReceiver receiver);

    /**
     * @return the human-readable title or description for the unfinished operations state; return null to disable standard state
     *         contribution
     */
    String getUnfinishedOperationsDescription();

    /**
     * Generates output describing the current unfinished operations state, if applicable.
     * 
     * @param receiver the receiver to send the output to
     */
    void printUnfinishedOperationsInformation(TextLinesReceiver receiver);
}
