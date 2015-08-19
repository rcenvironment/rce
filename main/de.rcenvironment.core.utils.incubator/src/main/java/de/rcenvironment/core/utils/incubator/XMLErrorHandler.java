/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * 
 * Error handler to handle XML parsing errors and log them to log4j.
 * 
 * @author Holger Cornelsen
 */
public class XMLErrorHandler implements ErrorHandler {

    /**
     * Our logger instance.
     */
    protected static final Log LOGGER = LogFactory.getLog(XMLErrorHandler.class);
    
    
    /**
     * {@inheritDoc}
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        String message = "XML parsing warning";
        message = addMessage(exception, message);
        LOGGER.warn(message);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        String message = "XML parsing error";
        message = addMessage(exception, message);
        LOGGER.error(message);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        String message = "XML fatal parsing error";
        message = addMessage(exception, message);
        LOGGER.fatal(message);

        // Throwing a SAX exception terminates the parsing process after a fatal error.
        throw new SAXException("Fatal XML parsing error, aborting ...");
    }

    /**
     * Adds error details to a given message string.
     * @param exception The exception describing the error.
     * @param message  The message string to which the details are added.
     * @return A message string with error details.
     */
    private String addMessage(SAXParseException exception, final String message) {
        return ", line " + exception.getLineNumber()
             + ", column " + exception.getColumnNumber()
             + ", URI: " + exception.getSystemId()
             + ", message: " + exception.getMessage();
    }

}
