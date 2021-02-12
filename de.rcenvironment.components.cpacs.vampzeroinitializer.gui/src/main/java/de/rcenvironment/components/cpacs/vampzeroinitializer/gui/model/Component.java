/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
public class Component extends AbstractNamed {

    private List<Discipline> disciplines = new ArrayList<Discipline>();

    public List<Discipline> getDisciplines() {
        return disciplines;
    }

    /**
     * Set disciplines.
     * 
     * @param theDisciplines disciplines
     * @return component
     */
    public Component setDisciplines(final List<Discipline> theDisciplines) {
        disciplines.clear();
        for (final Discipline discipline : theDisciplines) {
            disciplines.add(discipline);
            discipline.setComponent(this);
        }
        return this;
    }

    /**
     * Get discipline for name.
     * 
     * @param name name
     * @return discipline
     */
    public Discipline getDisciplineForName(final String name) {
        for (final Discipline discipline : disciplines) {
            if (discipline.getName().equals(name)) {
                return discipline;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getName() + " (" + Integer.toString(disciplines.size()) + ")";
    }

}
