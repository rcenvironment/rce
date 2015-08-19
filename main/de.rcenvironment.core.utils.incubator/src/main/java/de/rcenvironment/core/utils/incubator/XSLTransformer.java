/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.incubator.xml.XMLException;

/**
 * Handler for all needed XSLT things.
 * 
 * @author Markus Kunde
 * @author Jan Flink
 */
public class XSLTransformer {

    /**
     * Our logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(XSLTransformer.class);

    /**
     * TransformerFactory for XSL-Transformation.
     */
    private TransformerFactory tFactory = null;

    /**
     * Constructor.
     * 
     */
    public XSLTransformer() {
        tFactory = TransformerFactory.newInstance();
    }

    /**
     * Executes XSL-transformation on the files.
     * 
     * For toolInput usage: InitialCpacs, InputMappingXSLT, ToolInput For toolspecific usage: ToolInput, InputToolSpecificMappingXSLT,
     * NewToolInput For toolOutput usage: InitialCpacs, OutputMappingXSLT, ResultCpacs
     * 
     * @param fileNameXml Name of source xml-file
     * @param fileNameXslt Name of xslt-file
     * @param fileNameResult Name of result-file
     * @throws XMLException Thrown if xml transformation fails
     */
    public void transformXMLFile(final String fileNameXml,
        final String fileNameXslt,
        final String fileNameResult) throws XMLException {
        Transformer transformer = null;

        try {
            transformer = tFactory.newTransformer(new StreamSource(fileNameXslt));
            transformer.transform(new StreamSource(fileNameXml), new StreamResult(
                new FileOutputStream(fileNameResult)));
        } catch (final TransformerConfigurationException e) {
            throw new XMLException(e);
        } catch (final FileNotFoundException e) {
            throw new XMLException(e);
        } catch (final TransformerException e) {
            throw new XMLException(e);
        }
    }

    /**
     * Executes XSL-transformation on the files.
     * 
     * For toolInput usage: InitialCpacs, InputMappingXSLT, ToolInput For toolspecific usage: ToolInput, InputToolSpecificMappingXSLT,
     * NewToolInput For toolOutput usage: InitialCpacs, OutputMappingXSLT, ResultCpacs
     * 
     * @param inputStreamXml Name of source inputStream Xml
     * @param fileNameXslt Name of xslt-file
     * @param fileNameResult Name of result-file
     * @throws XMLException Thrown if xml transformation fails
     */
    public void transformXMLFile(final InputStream inputStreamXml,
        final String fileNameXslt,
        final String fileNameResult) throws XMLException {

        Transformer transformer = null;

        try {
            transformer = tFactory.newTransformer(new StreamSource(fileNameXslt));
            transformer.transform(new StreamSource(inputStreamXml),
                new StreamResult(new FileOutputStream(fileNameResult)));
        } catch (final TransformerConfigurationException e) {
            throw new XMLException(e);
        } catch (final FileNotFoundException e) {
            throw new XMLException(e);
        } catch (final TransformerException e) {
            throw new XMLException(e);
        }
    }

    /**
     * Executes XSL-transformation on the files.
     * 
     * For toolInput usage: InitialCpacs, InputMappingXSLT, ToolInput For toolspecific usage: ToolInput, InputToolSpecificMappingXSLT,
     * NewToolInput For toolOutput usage: InitialCpacs, OutputMappingXSLT, ResultCpacs
     * 
     * @param inputStreamXml Name of source inputStream Xml
     * @param fileNameXslt Name of xslt-file
     * @param outputStreamResult Name of result outputStream
     * @throws XMLException Thrown if xml transformation fails
     */
    public void transformXMLFile(final InputStream inputStreamXml,
        final String fileNameXslt,
        final OutputStream outputStreamResult) throws XMLException {

        Transformer transformer = null;

        try {
            transformer = tFactory.newTransformer(new StreamSource(fileNameXslt));
            transformer.transform(new StreamSource(inputStreamXml), new StreamResult(outputStreamResult));
        } catch (final TransformerConfigurationException e) {
            throw new XMLException(e);
        } catch (final TransformerException e) {
            throw new XMLException(e);
        }
    }

}
