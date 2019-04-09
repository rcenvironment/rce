/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common;

import java.io.Serializable;

/**
 * Describing the dimension of values.
 * @author Christian Weiss.
 */
public class Dimension extends AbstractType implements Serializable {

    private static final long serialVersionUID = -4924409826702553273L;

    private Boolean defaultX;

    public Dimension(final String name, final Boolean defaultX) {
        super(name);
        this.defaultX = defaultX;
    }

    /**
     * @return <code>true</code> if x is default, else <code>false</code>.
     */
    public Boolean isDefaultX() {
        return defaultX;
    }

}
