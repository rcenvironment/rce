/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common;

import java.io.Serializable;

/**
 * Holding information which describes one study case (i.e. study chart).
 * @author Christian Weiss
 */
public class Study implements Serializable {

    private static final long serialVersionUID = -241147793428099497L;

    private final String identifier;

    private final String title;

    private final StudyStructure structure;

    public Study(final String identifier, final String title, final StudyStructure structure) {
        this.identifier = identifier;
        this.title = title;
        this.structure = structure;
    }

    /**
     * @return the identifier of the {@link Study}.
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * @return the title of the {@link Study}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the underlying {@link StudyStructure}.
     */
    public StudyStructure getStructure() {
        return structure;
    }

}
