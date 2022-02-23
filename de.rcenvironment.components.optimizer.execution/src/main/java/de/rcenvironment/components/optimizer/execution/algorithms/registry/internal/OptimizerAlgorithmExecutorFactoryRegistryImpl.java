/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.execution.algorithms.registry.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.execution.OptimizerAlgorithmExecutor;
import de.rcenvironment.components.optimizer.execution.algorithms.registry.OptimizerAlgorithmExecutorFactory;
import de.rcenvironment.components.optimizer.execution.algorithms.registry.OptimizerAlgorithmExecutorFactoryRegistry;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Implementation of {@link OptimizerAlgorithmExecutorFactoryRegistry}.
 * 
 * @author Sascha Zur
 */
public class OptimizerAlgorithmExecutorFactoryRegistryImpl implements OptimizerAlgorithmExecutorFactoryRegistry {

    private final List<OptimizerAlgorithmExecutorFactory> factories =
        Collections.synchronizedList(new LinkedList<OptimizerAlgorithmExecutorFactory>());

    @Override
    public void addOptimizerAlgorithmExecutorFactory(OptimizerAlgorithmExecutorFactory incProvider) {
        if (incProvider != null && !factories.contains(incProvider)) {
            factories.add(incProvider);
        }
    }

    @Override
    public void removeOptimizerAlgorithmExecutorFactory(OptimizerAlgorithmExecutorFactory algFactory) {
        if (algFactory != null && factories.contains(algFactory)) {
            factories.remove(algFactory);
        }
    }

    @Override
    public OptimizerAlgorithmExecutor createAlgorithmProviderInstance(String algorithmPackage,
        Map<String, MethodDescription> methodConfiguration, Map<String, TypedDatum> outputValues, Collection<String> input,
        ComponentContext compContext, Map<String, Map<String, Double>> boundMaps, Map<String, Double> stepValues)
        throws ComponentException {

        OptimizerAlgorithmExecutor result = null;
        for (OptimizerAlgorithmExecutorFactory currentProvider : factories) {
            if (currentProvider.getOptimizerAlgorithmPackageIdentifier().equalsIgnoreCase(algorithmPackage)) {
                result = currentProvider.createOptimizerAlgorithmExecutorInstance(methodConfiguration, outputValues, input, compContext,
                    boundMaps.get("upper"), boundMaps.get("lower"), stepValues);
            }
        }
        return result;
    }
}
