/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common.internal;

import de.rcenvironment.components.parametricstudy.common.Study;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility class for identifier construction.
 * @author Christian Weiss
 */
public final class ParametricStudyUtils {

    /** Constant. */
    public static final String STRUCTURE_PATTERN = "study.structure.%s";

    private static final String DATA_PATTERN = "study.data.%s";
    
    private ParametricStudyUtils() {}
    
    protected static String createStructureIdentifier(final Study study) {
        return StringUtils.format(STRUCTURE_PATTERN, study.getIdentifier());
    }

    protected static String createDataIdentifier(final Study study) {
        return StringUtils.format(DATA_PATTERN, study.getIdentifier());
    }
}
