/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/**
 * Error handler to log XSLT transformation warnings and errors to log4j.
 * @author Holger Cornelsen
 */
public class XSLTErrorHandler implements ErrorListener {

    /**
     * Our logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(Source.class);
    
    
    /**
     * {@inheritDoc}
     * @see javax.xml.transform.ErrorListener#warning(javax.xml.transform.TransformerException)
     */
    @Override
    public void warning(final TransformerException exception) throws TransformerException {
        final String message = "XSLT transformation warning: " + exception.getMessageAndLocation();
        LOGGER.warn(message);
    }

    /**
     * {@inheritDoc}
     * @see javax.xml.transform.ErrorListener#error(javax.xml.transform.TransformerException)
     */
    @Override
    public void error(final TransformerException exception) throws TransformerException {
        final String message = "XSLT transformation error: " + exception.getMessageAndLocation();
        LOGGER.error(message);
    }

    /**
     * {@inheritDoc}
     * @see javax.xml.transform.ErrorListener#fatalError(javax.xml.transform.TransformerException)
     */
    @Override
    public void fatalError(final TransformerException exception) throws TransformerException {
        final String message = "XSLT fatal transformation error, " + exception.getMessageAndLocation();
        LOGGER.error(message);
        // Abort transformation process
        throw new TransformerException("Fatal XSLT transformation error, aborting ...");
    }

}
