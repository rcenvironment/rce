/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.execution.algorithms.registry;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.execution.OptimizerAlgorithmExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Registry for receiving methods from different optimization packages.
 * 
 * @author Sascha Zur
 */
public interface OptimizerAlgorithmExecutorFactoryRegistry {

    /**
     * Adds the given {@link OptimizerAlgorithmExecutorFactory} to the list of all provider registered.
     * 
     * @param factory new factory
     */
    void addOptimizerAlgorithmExecutorFactory(OptimizerAlgorithmExecutorFactory factory);

    /**
     * Removes the given {@link OptimizerAlgorithmExecutorFactory} from the list of all factories registered.
     * 
     * @param algFactory to remove
     */
    void removeOptimizerAlgorithmExecutorFactory(OptimizerAlgorithmExecutorFactory algFactory);

    /**
     * Returns an instance of the given providerPacke, if available.
     * 
     * @param algorithmPackage the package to look for (e.g. dakota ...)
     * @param methodConfiguration :
     * @param outputValues :
     * @param input :
     * @param compContext :
     * @param boundMaps : maps "lower" and "upper" for the upper and lower bounds start values
     * @param stepValues for the algorithm
     * @return instance of the algorithm class
     * @throws ComponentException on unexpected errors when creating instance of {@link OptimizerAlgorithmExecutor}
     */
    OptimizerAlgorithmExecutor createAlgorithmProviderInstance(String algorithmPackage,
        Map<String, MethodDescription> methodConfiguration, Map<String, TypedDatum> outputValues,
        Collection<String> input, ComponentContext compContext, Map<String, Map<String, Double>> boundMaps, Map<String, Double> stepValues)
        throws ComponentException;
}
