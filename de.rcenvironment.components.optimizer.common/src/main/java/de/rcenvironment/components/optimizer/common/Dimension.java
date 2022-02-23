/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;

/**
 * Describing the dimension of values.
 * @author Christian Weiss.
 */
public class Dimension extends AbstractType implements Serializable {

    private static final long serialVersionUID = -4924409826702553273L;

    private Boolean defaultX;

    public Dimension(final String name, final String type,
            final Boolean defaultX) {
        super(name, type);
        this.defaultX = defaultX;
    }

    /**
     * @return <code>true</code> if x is default, else <code>false</code>.
     */
    public Boolean isDefaultX() {
        return defaultX;
    }

}
