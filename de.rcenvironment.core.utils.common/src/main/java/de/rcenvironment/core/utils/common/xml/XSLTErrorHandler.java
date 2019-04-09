/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.xml;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Error handler to log XSLT transformation warnings and errors.
 * @author Holger Cornelsen
 * @author Brigitte Boden
 */
public class XSLTErrorHandler implements ErrorListener {
   
    /**
     * Our logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(Source.class);
    
    private static final String ERROR_OCCURED_IN_XSL_TRANSFORMER = "Error occured in XSL transformer: ";
    
    /**
     * {@inheritDoc}
     * @see javax.xml.transform.ErrorListener#warning(javax.xml.transform.TransformerException)
     */
    @Override
    public void warning(final TransformerException exception) throws TransformerException {
        LOGGER.warn(ERROR_OCCURED_IN_XSL_TRANSFORMER + exception.toString());
    }

    /**
     * {@inheritDoc}
     * @see javax.xml.transform.ErrorListener#error(javax.xml.transform.TransformerException)
     */
    @Override
    public void error(final TransformerException exception) throws TransformerException {
        throw exception;
    }

    /**
     * {@inheritDoc}
     * @see javax.xml.transform.ErrorListener#fatalError(javax.xml.transform.TransformerException)
     */
    @Override
    public void fatalError(final TransformerException exception) throws TransformerException {
        throw exception;
    }

}
