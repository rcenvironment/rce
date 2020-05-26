/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.common;

/**
 * Constant class.
 * 
 * @author Sascha Zur
 */
public final class DOEConstants {

    /** Constant. */
    public static final String COMPONENT_ID = "de.rcenvironment.doe.v2";

    /** Constant. */
    public static final String DOE_ALGORITHM_FULLFACT = "Full factorial design";

    /** Constant. */
    public static final String DOE_ALGORITHM_LHC = "Latin hypercube design";

    /** Constant. */
    public static final String DOE_ALGORITHM_MONTE_CARLO = "Monte Carlo design";

    /** Constant. */
    public static final String DOE_ALGORITHM_CUSTOM_TABLE = "Custom design table";

    /** Constant. */
    public static final String DOE_ALGORITHM_CUSTOM_TABLE_INPUT = "Custom design table as input";

    /** Constant. */
    public static final String[] ALGORITMS = new String[] { DOE_ALGORITHM_FULLFACT, DOE_ALGORITHM_LHC, DOE_ALGORITHM_MONTE_CARLO,
        DOE_ALGORITHM_CUSTOM_TABLE, DOE_ALGORITHM_CUSTOM_TABLE_INPUT };

    /** Constant. */
    public static final String KEY_TABLE = "table";

    /** Constant. */
    public static final String KEY_METHOD = "method";

    /** Constant. */
    public static final String KEY_START_SAMPLE = "startSample";

    /** Constant. */
    public static final String KEY_END_SAMPLE = "endSample";

    /** Constant. */
    public static final String KEY_RUN_NUMBER = "runNumber";

    /** Constants. */
    public static final String OUTPUT_NAME_NUMBER_OF_SAMPLES = "Number of samples";

    /** Constant. */
    public static final String KEY_SEED_NUMBER = "seedNumber";

    /** Constant. */
    public static final String META_KEY_LOWER = "lower";

    /** Constant. */
    public static final String META_KEY_UPPER = "upper";

    /** Constant. */
    public static final String TABLE_FILE_EXTENTION = ".csv";

    /** Constant. */
    public static final String INPUT_ID_NAME = "default";

    /** Constant. */
    public static final String CUSTOM_TABLE_ENDPOINT_NAME = "Custom table";

    /** Constant. */
    public static final String CUSTOM_TABLE_ENDPOINT_ID = "startTable";

    private DOEConstants() {

    }

}
