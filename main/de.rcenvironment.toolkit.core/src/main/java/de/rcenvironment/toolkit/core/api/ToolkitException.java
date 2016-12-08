/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.api;

/**
 * A generic exception class for {@link Toolkit} errors, especially during setup or initialization.
 * 
 * @author Robert Mischke
 */
public class ToolkitException extends Exception {

    private static final long serialVersionUID = 7899268830830296522L;

    public ToolkitException(ReflectiveOperationException e) {
        super(e);
    }

    public ToolkitException(String s) {
        super(s);
    }

    public ToolkitException(String s, RuntimeException e) {
        super(s, e);
    }

}
