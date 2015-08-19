/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;


/**
 * Utility class for identifier construction.
 * @author Christian Weiss
 */
public final class OptimizerUtils {

    /** Constant. */
    public static final String STRUCTURE_PATTERN = "optimizer.structure.%s";

    private static final String DATA_PATTERN = "optimizer.data.%s";
    
    private OptimizerUtils() {}
    
    protected static String createStructureIdentifier(final ResultSet study) {
        return String.format(STRUCTURE_PATTERN, study.getIdentifier());
    }

    protected static String createDataIdentifier(final ResultSet study) {
        return String.format(DATA_PATTERN, study.getIdentifier());
    }
}
