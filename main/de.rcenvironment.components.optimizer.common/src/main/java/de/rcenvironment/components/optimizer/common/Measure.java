/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;

/**
 * Describes one measured value.
 * @author Christian Weiss
 */
public class Measure extends AbstractType implements Serializable {

    private static final long serialVersionUID = 6024154906948612948L;

    public Measure(final String name, final String type) {
        super(name, type);
    }

}
