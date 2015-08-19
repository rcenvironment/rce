/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;

import java.util.Random;

/**
 * Provides the method dependent options for dakota algorithms.
 * 
 *  @author Sascha Zur
 */
public final class DakotaMethodConstants {

    /**
     * Quasi Newton
     */
    /** Constant. */
    public static final String QN_MAX_STEPSIZE = "qn_maxStep";
    /** Constant. */
    public static final String QN_GRAD_TOLERANCE = "qn_grad_tolerance";
    /** Constant. */
    public static final String QN_CENTERING_PARAMETER = "qn_centering"; 
    /** Constant. */
    public static final String QN_CENTRAL_PATH = "qn_cent_path";
    /** Constant. */
    public static final String QN_MERIT_FCN = "qn_merit_function";
    /** Constant. */
    public static final String QN_SEARCH_METHOD = "qn_search_method";
    /** Constant. */
    public static final String QN_STEP_TO_BOUND = "qn_step_to_bound";
    
    /** Constant. */
    public static final String QN_MAX_STEPSIZE_DEF = "1000";
    /** Constant. */
    public static final String QN_GRAD_TOLERANCE_DEF = "0.0001";
    /** Constant. */
    public static final String QN_CENTERING_PARAMETER_DEF = "0.2"; 
    /** Constant. */
    public static final String QN_CENTRAL_PATH_DEF = "argaez_tapia";
    /** Constant. */
    public static final String QN_MERIT_FCN_DEF = "argaez_tapia";
    /** Constant. */
    public static final String QN_SEARCH_METHOD_DEF = "gradient_based_line_search";
    /** Constant. */
    public static final String QN_STEP_TO_BOUND_DEF = "0.99995";
     
    
    /**
     * APPS
     */
    /** */
    public static final String APPS_CONST_PENALTY_DEF = "1 ";
    /** Constant. */
    public static final String APPS_CONTR_FACTOR_DEF = "0.5";
    /** Constant. */
    public static final String APPS_INIT_DELTA_DEF = "1.0 ";
    /** Constant. */
    public static final String APPS_SMOOTH_DEF = "0";
    /** Constant. */
    public static final String APPS_SOL_TARGET_DEF = "";
    /** Constant. */
    public static final String APPS_TRESDELTA_DEF = "0.01";
    /** Constant. */
    public static final String APPS_MERIT_DEF = "merit1";
    /** Constant. */
    public static final String APPS_CONTR_FACTOR = "appsContr";
    /** Constant. */
    public static final String APPS_INIT_DELTA = "appsInitDelta";
    /** Constant. */
    public static final String APPS_MERIT = "appsMerit";
    /** Constant. */
    public static final String APPS_SMOOTH = "appsSmooth";
    /** Constant. */
    public static final String APPS_SOL_TARGET = "appsSolTarget";
    /** Constant. */
    public static final String APPS_TRESDELTA = "appsTresDelta";
    /** Constant. */
    public static final String APPS_CONST_PENALTY = "appsConstPenalty";
    
    /**
     * Coliny EA
     */
    /** */
    public static final String EA_CROSSOVER_TYPE = "eaCrossType";
    /** Constant. */
    public static final String EA_CROSSOVER_RATE = "eaCrossRate";
    /** Constant. */
    public static final String EA_FITNESS_TYPE = "eaFitnessType";
    /** Constant. */
    public static final String EA_INIT_TYPE = "eaInitType";
    /** Constant. */
    public static final String EA_MUT_RANGE = "eaMutRange";
    /** Constant. */
    public static final String EA_MUT_RATE = "eaMutRate";
    /** Constant. */
    public static final String EA_MUT_RATIO = "eaMutRatio";
    /** Constant. */
    public static final String EA_MUT_SCALE = "eaMutScale";
    /** Constant. */
    public static final String EA_MUT_TYPE = "eaMutType";
    /** Constant. */
    public static final String EA_NEW_SOL = "eaNewSol";
    /** Constant. */
    public static final String EA_POPULATION = "eaPopulation";
    /** Constant. */
    public static final String EA_REPLACEMENT_TYPE = "eaReplacementType";
    /** Constant. */
    public static final String EA_REPLACEMENT_TYPE_VALUE = "eaReplacementTypeValue";
    /** Constant. */
    public static final String EA_CROSSOVER_TYPE_DEF = "two_point";
    /** Constant. */
    public static final String EA_CROSSOVER_RATE_DEF = "0.8";
    /** Constant. */
    public static final String EA_FITNESS_TYPE_DEF = "linear_rank";
    /** Constant. */
    public static final String EA_INIT_TYPE_DEF = "unique_random";
    /** Constant. */
    public static final String EA_MUT_RANGE_DEF = "1";
    /** Constant. */
    public static final String EA_MUT_RATE_DEF = "1.0";
    /** Constant. */
    public static final String EA_MUT_RATIO_DEF = "1.0";
    /** Constant. */
    public static final String EA_MUT_SCALE_DEF = "0.1";
    /** Constant. */
    public static final String EA_MUT_TYPE_DEF = "offset_normal";
    /** Constant. */
    public static final String EA_NEW_SOL_DEF = "50";
    /** Constant. */
    public static final String EA_POPULATION_DEF = "49";
    /** Constant. */
    public static final String EA_REPLACEMENT_TYPE_DEF = "elitist";
    /** Constant. */
    public static final String EA_REPLACEMENT_TYPE_VALUE_DEF = "1";

    
    
    /**
     * Coliny Coblya
     */
    
    /** Constant. */
    public static final String CC_INIT_DELTA = "initDelta";
    /** Constant. */
    public static final String CC_INIT_DELTA_DEF = "0.1";
    /** Constant. */
    public static final String CC_THRES_DELTA = "thresDelta";
    /** Constant. */
    public static final String CC_THRES_DELTA_DEF = "0.01";
    
    
    /**
     * DOE LHS.
     */
    
    /** Constant. */
    public static final String DOE_LHS_SEED = "lhsSeed";

    /** Constant. */
    public static final int DOE_LHS_SEED_DEF = new Random().nextInt(Integer.MAX_VALUE);
    
    /** Constant. */
    public static final String DOE_LHS_FIXED_SEED = "lhsFixedSeed";

    /** Constant. */
    public static final boolean DOE_LHS_FIXED_SEED_DEF = false;
    /** Constant. */
    public static final String DOE_LHS_SAMPLES = "lhsSamples";
    /** Constant. */
    public static final int DOE_LHS_SAMPLES_DEF = 1;
    /** Constant. */
    public static final String DOE_LHS_SYMBOLS = "lhsSymbols";
    /** Constant. */
    public static final int DOE_LHS_SYMBOLS_DEF = 10;
    /** Constant. */
    public static final String DOE_LHS_MAIN_EFFECTS = "lhsMainEffects";
    /** Constant. */
    public static final boolean DOE_LHS_MAIN_EFFECTS_DEF = false;
    /** Constant. */
    public static final String DOE_LHS_QUALITY_METRICS = "lhsQualityMetrics";
    /** Constant. */
    public static final boolean DOE_LHS_QUALISTY_METRICS_DEF = false;
    /** Constant. */
    public static final String DOE_LHS_VARIANCE_BASED_DECOMP = "lhsVarianceBasedDecomp";
    /** Constant. */
    public static final boolean DOE_LHS_VARIANCE_BASED_DECOMP_DEF = false;
    
    
    /**
     * Hide the constructor.
     */
    private DakotaMethodConstants() {}
}
