/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.xml.api;

import java.io.File;
import java.util.Map;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;



/**
 * Supports writing inputs into XML files and reading outputs from XML files.
 *
 * @author Brigitte Boden
 */
public interface EndpointXMLService {
    
    /**
     * Updates a xml data set with values from inputs that are dynamically added on configuration time.
     * 
     * @param xmlFile The path to the XML file.
     * @param dynamicInputs The inputs to merge into the XML file
     * @param componentContext The component context
     * @throws DataTypeException thrown when data cannot cast to nodetext
     * @throws ComponentException thrown if xpath refers to no node throws {@link NullPointerException} if xmlFile or componentContext is
     *         <code>null</code>
     */
    void updateXMLWithInputs(File xmlFile, Map<String, TypedDatum> dynamicInputs,
        ComponentContext componentContext) throws DataTypeException, ComponentException;
    
    
    /**
     * Updates all output endpoints with data from a XML data set.
     * 
     * @param xmlFile The path to the XML file.
     * @param componentContext The componentContext from which the endpoints are read.
     * @throws DataTypeException thrown when nodetext cannot cast to output type
     * @throws ComponentException if xpath is not evaluable
     * throws {@link NullPointerException} if xmlFile or componentContext is <code>null</code>
     */
    void updateOutputsFromXML(File xmlFile,
        ComponentContext componentContext) throws DataTypeException, ComponentException;
    
    /**
     * 
     * @return Recently used XPath. This is used for error detection.
     */
    String getRecentXpath();

}
