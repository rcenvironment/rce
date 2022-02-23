/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common;

import java.io.Serializable;

/**
 * Describes one measured value.
 * @author Christian Weiss
 */
public class Measure extends AbstractType implements Serializable {

    private static final long serialVersionUID = 6024154906948612948L;

    public Measure(final String name) {
        super(name);
    }

}
