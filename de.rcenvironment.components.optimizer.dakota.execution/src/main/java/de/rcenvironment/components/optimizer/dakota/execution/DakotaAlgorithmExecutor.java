/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.dakota.execution;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.execution.OptimizerAlgorithmExecutor;
import de.rcenvironment.components.optimizer.dakota.execution.internal.DakotaAlgorithm;
import de.rcenvironment.components.optimizer.execution.algorithms.registry.OptimizerAlgorithmExecutorFactory;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Dakota implementation of {@link OptimizerAlgorithmExecutorFactory}.
 * 
 * @author Sascha Zur
 */
public class DakotaAlgorithmExecutor implements OptimizerAlgorithmExecutorFactory {

    @Override
    public String getOptimizerAlgorithmPackageIdentifier() {
        return "dakota";
    }

    @Override
    public OptimizerAlgorithmExecutor createOptimizerAlgorithmExecutorInstance(
        Map<String, MethodDescription> methodConfiguration, Map<String, TypedDatum> outputValues,
        Collection<String> input, ComponentContext compContext,
        Map<String, Double> upperMap, Map<String, Double> lowerMap, Map<String, Double> stepValues) throws ComponentException {
        return new DakotaAlgorithm(methodConfiguration, outputValues, input, compContext, upperMap, lowerMap, stepValues);
    }

}
