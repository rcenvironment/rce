/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model;

/**
 * Abstract named class.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public abstract class AbstractNamed implements Named, Comparable<Named> {

    private String name;

    public AbstractNamed() {}

    public AbstractNamed(final AbstractNamed from) {
        name = from.name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Named setName(final String aName) {
        name = aName;
        return this;
    }

    @Override
    public int compareTo(final Named other) {
        return name.compareTo(other.getName());
    }

}
