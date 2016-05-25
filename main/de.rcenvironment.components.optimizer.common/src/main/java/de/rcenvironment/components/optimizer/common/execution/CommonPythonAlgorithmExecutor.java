/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common.execution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * A part implementation for the algorithm executor which write config files in python.
 * 
 * @author Sascha Zur
 */
public abstract class CommonPythonAlgorithmExecutor extends OptimizerAlgorithmExecutor {

    protected static final Log LOGGER = LogFactory.getLog(OptimizerAlgorithmExecutor.class);

    protected static final String CLOSE_BRACKET_AND_NL = ")\n";

    protected static final String REGEX_DOT = "\\.";

    private static final String DOT = ".";

    private static final int NEGATE_VALUE = -1;

    private static final String META_BASE = "base";

    private static final String VALUE = "value";

    protected final String apostroph = "'";

    protected final String comma = ",";

    protected Map<String, TypedDatum> outputValues;

    protected Collection<String> input;

    protected String algorithm;

    protected Map<String, MethodDescription> methodConfiguration;

    protected Map<String, Double> lowerMap;

    protected Map<String, Double> upperMap;

    private boolean gradRequest;

    private List<String> orderedOutputValueKeys;

    private Map<String, Double> stepValues;

    public CommonPythonAlgorithmExecutor(Map<String, MethodDescription> methodConfiguration,
        Map<String, TypedDatum> outputValues,
        Collection<String> input, ComponentContext compContext,
        Map<String, Double> upperMap, Map<String, Double> lowerMap, Map<String, Double> stepValues, String inputFilename)
        throws ComponentException {
        super(compContext, compContext.getInstanceName(), inputFilename);
        this.algorithm = compContext.getConfigurationValue(OptimizerComponentConstants.ALGORITHMS);
        this.methodConfiguration = methodConfiguration;
        this.outputValues = outputValues;
        this.input = input;
        this.lowerMap = lowerMap;
        this.upperMap = upperMap;
        this.stepValues = stepValues;
        typedDatumFactory = compContext.getService(TypedDatumService.class).getFactory();

    }

    public CommonPythonAlgorithmExecutor() {

    }

    protected void writeConfigurationFile(File configFile) throws ComponentException {
        Map<String, Object> configuration = new HashMap<>();

        addOutputsToConfig(configuration);

        addInputsToConfig(configuration);

        String[] algos = algorithm.split(comma);

        for (String algo : algos) {
            Map<String, Map<String, String>> allSettings = methodConfiguration.get(algo).getSpecificSettings();
            Map<String, Object> settings = new HashMap<>();
            for (String key : allSettings.keySet()) {
                Map<String, Object> set = new HashMap<>();
                String dataType = allSettings.get(key).get("dataType");
                String value = allSettings.get(key).get("Value");
                if (value == null || value.isEmpty()) {
                    value = allSettings.get(key).get("DefaultValue");
                }
                switch (dataType.toLowerCase()) {
                case "real":
                    set.put(VALUE, Double.parseDouble(value));

                    break;
                case "int":
                    set.put(VALUE, Integer.parseInt(value));
                    break;
                case "bool":
                    set.put(VALUE, Boolean.parseBoolean(value));
                    break;
                case "None":
                default:
                    set.put(VALUE, value);
                }
                settings.put(key, set);
            }
            configuration.put("algorithmSettings", settings);
            configuration.put("algorithm", algo.substring(0, algo.lastIndexOf("[") - 1));
        }

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, configuration);
        } catch (IOException e) {
            throw new ComponentException("Failed to write configuration file", e);
        }

    }

    private void addInputsToConfig(Map<String, Object> configuration) {
        List<String> orderedInputValueKeys = new ArrayList<String>(input.size());
        for (String e : input) {
            orderedInputValueKeys.add(e);
        }
        Collections.sort(orderedInputValueKeys);

        List<String> orderedObjectives = new LinkedList<>();
        List<String> orderedConstraints = new LinkedList<>();
        List<String> orderedGradients = new LinkedList<>();
        Map<String, Double> minValues = new HashMap<>();
        Map<String, Double> maxValues = new HashMap<>();
        Map<String, Double> objectiveWeights = new HashMap<>();

        for (String key : orderedInputValueKeys) {
            if (compContext.getDynamicInputIdentifier(key).equals(OptimizerComponentConstants.ID_OBJECTIVE)
                && !key.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                orderedObjectives.add(key);
                String weight = compContext.getInputMetaDataValue(key, OptimizerComponentConstants.META_WEIGHT);
                objectiveWeights.put(key, Double.parseDouble(weight));
            }
            if (compContext.getDynamicInputIdentifier(key).equals(OptimizerComponentConstants.ID_CONSTRAINT)
                && !key.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                orderedConstraints.add(key);
                minValues.put(key, lowerMap.get(key));
                maxValues.put(key, upperMap.get(key));
            }
            if (key.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                orderedGradients.add(key);
            }
        }

        configuration.put("objectives", orderedObjectives);
        configuration.put("constraints", orderedConstraints);
        configuration.put("gradients", orderedGradients);
        configuration.put("objectivesWeights", objectiveWeights);
        configuration.put("minValuesConstraints", minValues);
        configuration.put("maxValuesConstraints", maxValues);

    }

    private void addOutputsToConfig(Map<String, Object> configuration) {
        orderedOutputValueKeys = new ArrayList<String>();
        for (String output : outputValues.keySet()) {
            if (compContext.getOutputDataType(output) == DataType.Vector) {
                for (int i = 0; i < Integer.valueOf(compContext.getOutputMetaDataValue(output,
                    OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                    orderedOutputValueKeys.add(output + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i);
                }
            } else {
                orderedOutputValueKeys.add(output);
            }
        }
        Collections.sort(orderedOutputValueKeys);
        Map<String, Double> initValues = new HashMap<>();
        Map<String, Double> baseValues = new HashMap<>();
        // Map<String, Double> stepValues = new HashMap<>();
        Map<String, Boolean> discreteValues = new HashMap<>();
        Map<String, Double> minValues = new HashMap<>();
        Map<String, Double> maxValues = new HashMap<>();

        for (String key : orderedOutputValueKeys) {

            if (key.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)
                && compContext.getOutputDataType(getVectorName(key)) == DataType.Vector) {
                if (outputValues.containsKey(key)) {
                    initValues.put(key, ((FloatTD) outputValues.get(key)).getFloatValue());
                }
                discreteValues.put(key, Boolean.parseBoolean(
                    compContext.getOutputMetaDataValue(getVectorName(key), OptimizerComponentConstants.META_IS_DISCRETE)));
            } else {
                initValues.put(key, ((FloatTD) outputValues.get(key)).getFloatValue());
            }
            if (!key.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                if (key.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)) {
                    if (compContext.getOutputMetaDataValue(getVectorName(key), META_BASE) != null) {
                        baseValues.put(key, Double.valueOf(compContext.getOutputMetaDataValue(
                            getVectorName(key), META_BASE)));
                    }
                } else {
                    if (compContext.getOutputMetaDataValue(key, META_BASE) != null) {
                        baseValues.put(key, Double.valueOf(compContext.getOutputMetaDataValue(key, META_BASE)));
                    }
                }
            }
            minValues.put(key, lowerMap.get(key));
            maxValues.put(key, upperMap.get(key));
        }

        configuration.put("designVariableCount", orderedOutputValueKeys.size());
        configuration.put("designVariables", orderedOutputValueKeys);
        configuration.put("initValues", initValues);
        configuration.put("minValuesVariables", minValues);
        configuration.put("maxValuesVariables", maxValues);
        configuration.put("baseValues", baseValues);
        configuration.put("stepValues", stepValues);
        configuration.put("discreteValues", discreteValues);

    }

    private String getVectorName(String key) {
        return key.substring(0, key.indexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readOutputFileFromExternalProgram(Map<String, TypedDatum> outputValueMap) throws IOException {
        if (messageFromClient != null) {
            String currentWorkingDir = messageFromClient.getCurrentWorkingDir();
            if (currentWorkingDir != null) {
                File[] cwdFiles = new File(currentWorkingDir).listFiles();
                if (cwdFiles != null) {
                    File outputFile = cwdFiles[0];
                    ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
                    Map<String, Object> result = mapper.readValue(outputFile, new HashMap<String, Object>().getClass());
                    List<Double> outputs = (List<Double>) result.get("designVar");
                    int offset = 0;
                    for (int i = 0; i < orderedOutputValueKeys.size(); i += offset) {
                        offset = readOutputs(outputValueMap, outputs, i);
                    }
                    gradRequest = (Boolean) result.get("gradRequest");
                }
            }
        }
    }

    private int readOutputs(Map<String, TypedDatum> outputValueMap, List<Double> outputs, int i) {
        int offset;
        String key = orderedOutputValueKeys.get(i);
        offset = 1;
        String realKey = "";
        if (key.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)) {
            realKey = key.substring(0, key.lastIndexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL));
        }

        if (!realKey.isEmpty()) {
            if (compContext.getOutputDataType(realKey) == DataType.Vector && !orderedOutputValueKeys.contains(realKey)) {
                VectorTD resultVec = typedDatumFactory.createVector(Integer.parseInt(compContext.getOutputMetaDataValue(
                    realKey, OptimizerComponentConstants.METADATA_VECTOR_SIZE)));
                for (int j = 0; j < resultVec.getRowDimension(); j++) {
                    resultVec.setFloatTDForElement(typedDatumFactory.createFloat(outputs.get(i + j)), j);
                }
                outputValueMap.put(realKey, resultVec);
                offset++;
            }
        } else {
            outputValueMap.put(key, typedDatumFactory.createFloat(outputs.get(i)));
        }
        return offset;
    }

    @Override
    public boolean getDerivativedNeeded() {
        return gradRequest;
    }

    @Override
    protected void writeInputFileforExternalProgram(Map<String, Double> functionVariables,
        Map<String, Double> functionVariablesGradients, Map<String, Double> constraintVariables, String outputFileName) throws IOException {
        File rceInputFile = new File(messageFromClient.getCurrentWorkingDir(), outputFileName);

        for (String key : functionVariables.keySet()) {
            String name = key;
            if (name.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)
                && !name.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                name = name.substring(0, name.lastIndexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL));
            }
            if (!name.contains(OptimizerComponentConstants.GRADIENT_DELTA)
                && compContext.getInputMetaDataValue(name, OptimizerComponentConstants.META_GOAL).equals("Maximize")) { // Maximize
                // Optimizer only minimizes functions so for maximizing you need to minimize -f(x)
                functionVariables.put(key, functionVariables.get(key) * NEGATE_VALUE);
            }
        }

        // Arrange gradients
        Map<String, List<Double>> gradientsPerObjectiveOrConstraint = new HashMap<>();

        getGradients(functionVariables, functionVariablesGradients, gradientsPerObjectiveOrConstraint, orderedOutputValueKeys);
        getGradients(constraintVariables, functionVariablesGradients, gradientsPerObjectiveOrConstraint, orderedOutputValueKeys);

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        Map<String, Object> all = new HashMap<>();

        all.put("objective", functionVariables);
        all.put("const", constraintVariables);

        all.put("grad", gradientsPerObjectiveOrConstraint);

        mapper.writerWithDefaultPrettyPrinter().writeValue(rceInputFile, all);
    }

    private void getGradients(Map<String, Double> functionVariables, Map<String, Double> functionVariablesGradients,
        Map<String, List<Double>> gradientsPerObjectiveOrConstraint, List<String> outputNames) {
        for (String obj : functionVariables.keySet()) {
            List<Double> values = new LinkedList<>();
            for (String output : outputNames) {
                if (functionVariablesGradients.containsKey(getGradientName(obj, output))) {
                    values.add(functionVariablesGradients.get(getGradientName(obj, output)));
                }
            }
            if (!values.isEmpty()) {
                gradientsPerObjectiveOrConstraint.put(obj, values);
            }
        }
    }

    private String getGradientName(String obj, String output) {
        return OptimizerComponentConstants.GRADIENT_DELTA + obj + DOT + OptimizerComponentConstants.GRADIENT_DELTA + output;
    }

}
