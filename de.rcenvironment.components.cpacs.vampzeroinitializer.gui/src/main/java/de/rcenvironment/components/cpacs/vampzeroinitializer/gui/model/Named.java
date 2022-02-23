/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model;

/**
 * A named item.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public interface Named {

    /**
     * Returns the name.
     * 
     * @return name
     */
    String getName();

    /**
     * Sets the name.
     * 
     * @param aName name
     * @return name
     */
    Named setName(final String aName);

}
