/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.xml;




/**
 * Class to store mapping informations between source and target XML documents.
 * 
 * @author Holger Cornelsen
 * @author Markus Kunde
 */

public class XMLMappingInformation {

    /** XPath expression of source element. */
    private String mySourceXPath = "";
    
    /** XPath expression of target element. */
    private String myTargetXPath = "";
    
    /** Mapping mode to use for the mapping. */
    private EMappingMode myMode = EMappingMode.Delete;

    /**
     * Default constructor.
     */
    public XMLMappingInformation() {
        /* all variables are set on object creation */
    }

    /**
     * Currently unused constructor.
     * @param source path
     * @param target path
     */
    public XMLMappingInformation(final String source, final String target) {
        mySourceXPath = source;
        myTargetXPath = target;
    }

    /**
     * Gets source XPath.
     * @return Source XPath as string.
     */
    public String getSourceXPath() {
        return mySourceXPath;
    }

    /**
     * Sets source XPath.
     * @param sourceXPath The source XPath as string.
     */
    public void setSourceXPath(final String sourceXPath) {
        mySourceXPath = sourceXPath;
    }

    /**
     * Gets target XPath.
     * @return Target XPath as string.
     */
    public String getTargetXPath() {
        return myTargetXPath;
    }

    /**
     * Sets target XPath.
     * @param targetXPath The target XPath as string.
     */
    public void setTargetXPath(final String targetXPath) {
        myTargetXPath = targetXPath;
    }

    /**
     * Gets the mapping mode.
     * @return Returns the mode.
     */
    public EMappingMode getMode() {
        return myMode;
    }

    /**
     * Sets the mapping mode.
     * @param mode The mode to set.
     */
    public void setMode(final EMappingMode mode) {
        myMode = mode;
    }

}
