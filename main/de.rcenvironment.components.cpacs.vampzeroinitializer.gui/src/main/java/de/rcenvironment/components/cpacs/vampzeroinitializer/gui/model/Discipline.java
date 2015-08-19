/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class Discipline extends AbstractNamed {

    private List<Parameter> parameters = new ArrayList<Parameter>();

    private Component parent;

    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Set parameters.
     * 
     * @param theParameters p
     * @return discipline
     */
    public Discipline setParameters(final List<Parameter> theParameters) {
        parameters.clear();
        for (final Parameter parameter : theParameters) {
            parameters.add(parameter);
            parameter.setDiscipline(this);
        }
        return this;
    }

    public Component getComponent() {
        return parent;
    }

    /**
     * Set component.
     * 
     * @param theParent parent
     * @return discipline
     */
    public Discipline setComponent(final Component theParent) {
        parent = theParent;
        return this;
    }

    /**
     * Get the named parameter or null if not found.
     * 
     * @param name The parameter name
     * @return The parameter found or null
     */
    public Parameter getParameterForName(final String name) {
        for (final Parameter parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getName() + " (" + Integer.toString(parameters.size()) + ")";
    }

}
