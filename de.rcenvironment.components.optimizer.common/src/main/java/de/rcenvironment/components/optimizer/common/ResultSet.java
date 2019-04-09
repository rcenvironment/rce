/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;

/**
 * Holding information which describes one result set (i.e. study chart).
 * @author Sascha Zur
 */
public class ResultSet implements Serializable {

    private static final long serialVersionUID = -241147793428099497L;

    private final String identifier;

    private final String title;

    private final ResultStructure structure;

    public ResultSet(final String identifier, final String title, final ResultStructure structure) {
        this.identifier = identifier;
        this.title = title;
        this.structure = structure;
    }

    /**
     * @return the identifier of the {@link ResultSet}.
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * @return the title of the {@link ResultSet}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the underlying {@link ResultStructure}.
     */
    public ResultStructure getStructure() {
        return structure;
    }

}
