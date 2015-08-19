/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.execution.algorithms.registry;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.execution.OptimizerAlgorithmExecutor;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Registry for receiving methods from different optimization packages.
 * 
 * @author Sascha Zur
 */
public interface OptimizerAlgorithmExecutorFactoryRegistry {

    /**
     * Adds the given {@link OptimizerAlgorithmExecutorFactory} to the list of all provider
     * registered.
     * 
     * @param factory new factory
     */
    void addOptimizerAlgorithmExecutorFactory(OptimizerAlgorithmExecutorFactory factory);

    /**
     * Removes the given {@link OptimizerAlgorithmExecutorFactory} from the list of all factories
     * registered.
     * 
     * @param algFactory to remove
     */
    void removeOptimizerAlgorithmExecutorFactory(OptimizerAlgorithmExecutorFactory algFactory);

    /**
     * Returns an instance of the given providerPacke, if available.
     * 
     * @param algorithmPackage the package to look for (e.g. dakota ...)
     * @param algorithm the actual method used
     * @param methodConfiguration :
     * @param outputValues :
     * @param input :
     * @param compContext :
     * @param boundMaps : maps "lower" and "upper" for the upper and lower bounds start values
     * @return instance of the algorithm class
     */
    OptimizerAlgorithmExecutor createAlgorithmProviderInstance(String algorithmPackage, String algorithm,
        Map<String, MethodDescription> methodConfiguration, Map<String, TypedDatum> outputValues,
        Collection<String> input, ComponentContext compContext, Map<String, Map<String, Double>> boundMaps);
}
