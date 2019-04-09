/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;

import de.rcenvironment.core.utils.common.StringUtils;

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
        return StringUtils.format(STRUCTURE_PATTERN, study.getIdentifier());
    }

    protected static String createDataIdentifier(final ResultSet study) {
        return StringUtils.format(DATA_PATTERN, study.getIdentifier());
    }
}
