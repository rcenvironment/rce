/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.xml.api;

import java.io.File;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;

import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * Provides mapping between two XML files using XMLMappingInformation or XSLT. (Former classes: XMLMapper, XSLTransformer)
 *
 * @author Brigitte Boden
 */
public interface XMLMapperService {

    /**
     * Executes XSL-transformation on the files.
     * 
     * @param sourceFile Name of source xml-file
     * @param resultFile Name of result-file
     * @param xsltFile Name of xslt-file
     * @param logReceiver TextOutputReceiver to receive log output of the Transformer, or null, if not needed.
     * @throws XMLException Thrown if xml transformation fails
     */
    void transformXMLFileWithXSLT(final File sourceFile, final File resultFile, final File xsltFile, TextLinesReceiver logReceiver)
        throws XMLException;

    /**
     * Does the mapping between the elements of a source document and a target document.
     * 
     * @param sourceFile The name of the source document whose elements should be mapped.
     * @param targetFile The name of the target document.
     * @param mappingsDoc A document with a list of mapping rules.
     * @throws XPathExpressionException Thrown if XPath could not be evaluated.
     * @throws XMLException Mapping error.
     * 
     */
    void transformXMLFileWithXMLMappingInformation(final File sourceFile, final File targetFile, final Document mappingsDoc)
        throws XPathExpressionException, XMLException;

    /**
     * Does the mapping between the elements of a source document and a target document.
     * 
     * @param sourceFile The name of the source document whose elements should be mapped.
     * @param targetFile The name of the target document.
     * @param mappingsFile A document with a list of mapping rules.
     * @throws XPathExpressionException Thrown if XPath could not be evaluated.
     * @throws XMLException Mapping error.
     * 
     */
    void transformXMLFileWithXMLMappingInformation(final File sourceFile, final File targetFile, final File mappingsFile)
        throws XPathExpressionException, XMLException;

}
