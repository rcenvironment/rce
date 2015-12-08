/*
 * Copyright (C) 2006-2013 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.optimizer.generic.execution;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.execution.OptimizerAlgorithmExecutor;
import de.rcenvironment.components.optimizer.execution.algorithms.registry.OptimizerAlgorithmExecutorFactory;
import de.rcenvironment.components.optimizer.generic.execution.internal.GenericAlgorithmExecutor;
import de.rcenvironment.core.component.api.ComponentException;

/**
 * @Inherited
 * @author Sascha Zur
 */
public class GenericAlgorithmFactory implements OptimizerAlgorithmExecutorFactory {

    @Override
    public String getOptimizerAlgorithmPackageIdentifier() {
        return "generic";
    }

    @Override
    public OptimizerAlgorithmExecutor createOptimizerAlgorithmExecutorInstance(String algorithm,
        Map<String, MethodDescription> methodConfiguration, Map<String, de.rcenvironment.core.datamodel.api.TypedDatum> outputValues,
        Collection<String> input, de.rcenvironment.core.component.execution.api.ComponentContext ci, Map<String, Double> upperMap,
        Map<String, Double> lowerMap) throws ComponentException {
        return new GenericAlgorithmExecutor(algorithm, methodConfiguration, outputValues, input, ci, upperMap, lowerMap);
    }

}
