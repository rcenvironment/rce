/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;


/**
 * Exception thrown by Excel service. 
 *
 * @author Markus Kunde
 */
public class ExcelException extends RuntimeException {
    
    /** serial version uid. */
    private static final long serialVersionUID = 205688466866934547L;

    public ExcelException(final String message) {
        super(message);
    }
    
    public ExcelException(final Throwable throwable) {
        super(throwable);
    }
    
    public ExcelException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

}
