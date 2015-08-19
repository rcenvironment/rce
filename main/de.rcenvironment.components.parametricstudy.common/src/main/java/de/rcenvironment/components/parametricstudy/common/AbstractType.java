/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common;

import java.io.Serializable;

/**
 * Abstract class for typed structure classes.
 * @author Christian Weiss
 */
public abstract class AbstractType implements Serializable {

    private static final long serialVersionUID = 8717802746103860082L;

    private String name;

    public AbstractType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

