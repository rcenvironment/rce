/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
 * 
 * Must be implemented for all bundles that provide methods to the optimizer.
 * 
 * @author Sascha Zur
 */
public interface OptimizerAlgorithmExecutorFactory {

    /**
     * Method to identify the optimizer package providing optimization methods.
     * 
     * @return package identifier this factory produces executors from
     */
    String getOptimizerAlgorithmPackageIdentifier();

    /**
     * Returns an instance for executing the methods of the implementing bundle.
     * 
     * @param methodConfiguration :
     * @param outputValues :
     * @param input :
     * @param ci :
     * @param lowerMap map with lower bounds start values
     * @param upperMap map with upper bounds start values
     * @param stepValues for the algorithm
     * @return instance for executing methods
     * @throws ComponentException on unexpected errors when creating instance of {@link OptimizerAlgorithmExecutor}
     */
    OptimizerAlgorithmExecutor createOptimizerAlgorithmExecutorInstance(Map<String, MethodDescription> methodConfiguration,
        Map<String, TypedDatum> outputValues, Collection<String> input, ComponentContext ci, Map<String, Double> upperMap,
        Map<String, Double> lowerMap, Map<String, Double> stepValues)
        throws ComponentException;
}
