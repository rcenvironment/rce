/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model;

/**
 * Model class.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class Parameter extends AbstractNamed {

    private String description;

    private String value;

    private String factor;

    private Discipline parent;

    public Parameter() {}

    /**
     * Copy constructor.
     * 
     * @param from The object to copy from
     */
    public Parameter(final Parameter from) {
        description = from.description;
        value = from.value;
        factor = from.factor;
        parent = from.parent;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Set description.
     * 
     * @param aDescription d
     * @return parameter
     */
    public Parameter setDescription(final String aDescription) {
        description = aDescription;
        return this;
    }

    public String getValue() {
        return value;
    }

    /**
     * Set value.
     * 
     * @param aValue v
     * @return parameter
     */
    public Parameter setValue(final String aValue) {
        value = aValue;
        return this;
    }

    public String getFactor() {
        return factor;
    }

    /**
     * Set factor.
     * 
     * @param aFactor f
     * @return parameter
     */
    public Parameter setFactor(final String aFactor) {
        factor = aFactor;
        return this;
    }

    public Discipline getDiscipline() {
        return parent;
    }

    /**
     * Set discipline.
     * 
     * @param theParent p
     * @return parameter
     */
    public Parameter setDiscipline(final Discipline theParent) {
        parent = theParent;
        return this;
    }

    @Override
    public String toString() {
        return getName() + ": Value = " + value + " [Factor = " + factor + "]";
    }

}
