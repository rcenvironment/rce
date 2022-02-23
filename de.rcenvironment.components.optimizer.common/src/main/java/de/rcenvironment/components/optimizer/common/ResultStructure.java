/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Defining the structure of the values containing to one study.
 * 
 * @author Christian Weiss
 */
public class ResultStructure implements Serializable {

    private static final long serialVersionUID = 2860262690175198142L;

    private final Set<Dimension> dimensions = new HashSet<Dimension>();

    private final Set<Measure> measures = new HashSet<Measure>();

    /**
     * @return the defined {@link Dimension}s.
     */
    public Collection<Dimension> getDimensions() {
        return Collections.unmodifiableCollection(dimensions);
    }

    /**
     * @param name of the {@link Dimension} to get.
     * @return the {@link Dimension}.
     */
    public Dimension getDimension(String name) {
        for (final Dimension dimension : dimensions) {
            if (name.equals(dimension.getName())) {
                return dimension;
            }
        }
        return null;
    }

    /**
     * @return the definined {@link Measure}s.
     */
    public Collection<Measure> getMeasures() {
        return Collections.unmodifiableCollection(measures);
    }

    /**
     * @param name the name of the {@link Measure} to get.
     * @return the {@link Measure}.
     */
    public Measure getMeasure(String name) {
        for (final Measure measure : measures) {
            if (name.equals(measure.getName())) {
                return measure;
            }
        }
        return null;
    }

    /**
     * @param dimension {@link Dimension} to add.
     */
    public void addDimension(final Dimension dimension) {
        dimensions.add(dimension);
    }

    /**
     * @param measure {@link Measure} to add.
     */
    public void addMeasure(final Measure measure) {
        measures.add(measure);
    }

}
