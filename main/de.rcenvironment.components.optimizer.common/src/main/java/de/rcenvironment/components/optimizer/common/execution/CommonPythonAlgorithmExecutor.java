/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common.execution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.VectorTD;

/**
 * A part implementation for the algorithm executor which write config files in python.
 * 
 * @author Sascha Zur
 */
public abstract class CommonPythonAlgorithmExecutor extends OptimizerAlgorithmExecutor {

    protected static final Log LOGGER = LogFactory.getLog(OptimizerAlgorithmExecutor.class);

    protected static final String CLOSE_BRACKET_AND_NL = ")\n";

    protected static final String REGEX_DOT = "\\.";

    protected final String apostroph = "'";

    protected final String comma = ",";

    protected Map<String, TypedDatum> outputValues;

    protected Collection<String> input;

    protected String algorithm;

    protected Map<String, MethodDescription> methodConfiguration;

    protected Map<String, Double> lowerMap;

    protected Map<String, Double> upperMap;

    private boolean gradRequest;

    public CommonPythonAlgorithmExecutor(String algorithm, Map<String, MethodDescription> methodConfiguration,
        Map<String, TypedDatum> outputValues,
        Collection<String> input, ComponentContext compContext,
        Map<String, Double> upperMap, Map<String, Double> lowerMap, String inputFilename) {
        super(compContext, compContext.getInstanceName(), inputFilename);
        this.algorithm = algorithm;
        this.methodConfiguration = methodConfiguration;
        this.outputValues = outputValues;
        this.input = input;
        this.lowerMap = lowerMap;
        this.upperMap = upperMap;
        typedDatumFactory = compContext.getService(TypedDatumService.class).getFactory();
    }

    public CommonPythonAlgorithmExecutor() {

    }

    protected void writeConfigurationFile(File parafile) {
        try {
            BufferedWriter fw = new BufferedWriter(new FileWriter(parafile));
            List<String> orderedInputValueKeys = new ArrayList<String>(input.size());
            for (String e : input) {
                orderedInputValueKeys.add(e);
            }
            Collections.sort(orderedInputValueKeys);
            List<String> orderedOutputValueKeys = new ArrayList<String>();
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
            fw.append("def setting(DBase,Optim):\n"
                + "\tDBase.nDesVar = " + orderedOutputValueKeys.size() + "\n");

            for (String key : orderedOutputValueKeys) {
                fw.append("\tDBase.DesVar['name'].append('" + key + "')\n");
                if (key.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)
                    && compContext.getOutputDataType((key.substring(0, key.
                        indexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)))) == DataType.Vector
                       && ! outputValues.containsKey(key)) {
                    fw.append("\tDBase.DesVar['init'].append("
                        + ((VectorTD) outputValues.get(key.substring(0, key.
                                indexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)))).getFloatTDOfElement(Integer
                                    .parseInt(key.substring(key
                                        .lastIndexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL) + 1)))
                        + CLOSE_BRACKET_AND_NL);
                } else {
                    fw.append("\tDBase.DesVar['init'].append(" + outputValues.get(key) + CLOSE_BRACKET_AND_NL);
                }
                fw.append("\tDBase.DesVar['mini'].append(" + lowerMap.get(key) + CLOSE_BRACKET_AND_NL
                    + "\tDBase.DesVar['maxi'].append(" + upperMap.get(key) + CLOSE_BRACKET_AND_NL);
                if (key.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)) {
                    fw.append("\tDBase.DesVar['step'].append(" + compContext.getOutputMetaDataValue(
                        key.substring(0, key.indexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)), "step")
                        + CLOSE_BRACKET_AND_NL + "\tDBase.DesVar['base'].append(" + compContext.getOutputMetaDataValue(
                            key.substring(0, key.indexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)), "base")
                        + CLOSE_BRACKET_AND_NL);
                } else {
                    fw.append("\tDBase.DesVar['step'].append(" + compContext.getOutputMetaDataValue(key, "step")
                        + CLOSE_BRACKET_AND_NL
                        + "\tDBase.DesVar['base'].append(" + compContext.getOutputMetaDataValue(key, "base") + CLOSE_BRACKET_AND_NL);
                }

            }

            fw.append("\tDBase.Goal['name'] = [ ");
            int i = 1;
            int countInput = countInput(input);
            for (String key : orderedInputValueKeys) {
                if (compContext.getDynamicInputIdentifier(key).equals(OptimizerComponentConstants.ID_OBJECTIVE)
                    && !key.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                    fw.append(apostroph + key + apostroph);
                    if (i++ < countInput) {
                        fw.append(comma);
                    }
                }
            }
            fw.append("] \n"
                + "\n"
                + "\tDBase.Cons['name'] = [");
            i = 1;
            int countConstraints = countConstraint(input);
            for (String key : orderedInputValueKeys) {
                if (compContext.getDynamicInputIdentifier(key).equals(OptimizerComponentConstants.ID_CONSTRAINT)
                    && !key.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                    if (compContext.getInputDataType(key) == DataType.Vector) {
                        for (int j = 0; j < Integer.parseInt(compContext.getInputMetaDataValue(key,
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE)); j++) {
                            fw.append(apostroph + key + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + j + apostroph);
                            if (j < Integer.parseInt(compContext.getInputMetaDataValue(key,
                                OptimizerComponentConstants.METADATA_VECTOR_SIZE)) - 1) {
                                fw.append(comma);
                            }
                        }
                    } else {
                        fw.append(apostroph + key + apostroph);
                    }

                    if (i++ < countConstraints) {
                        fw.append(comma);
                    }
                }
            }
            fw.append("] \n");
            fw.append("\tDBase.Cons['mini'] = [");
            appendBoundList(lowerMap, fw, orderedInputValueKeys, countConstraints);

            fw.append("]\n");
            fw.append("\tDBase.Cons['maxi'] = [");
            appendBoundList(upperMap, fw, orderedInputValueKeys, countConstraints);

            fw.append("]\n");

            String[] algos = algorithm.split(comma);
            for (String algo : algos) {
                String pyranhaClass = methodConfiguration.get(algo).getMethodCode().split(REGEX_DOT)[0];
                Map<String, Map<String, String>> settings = methodConfiguration.get(algo).getSpecificSettings();
                for (String attributeKey : settings.keySet()) {
                    fw.append("\tOptim." + pyranhaClass + "['" + attributeKey + "'] = ");
                    String value = settings.get(attributeKey).get(OptimizerComponentConstants.VALUE_KEY);
                    if (value == null || value.equals("")) {
                        value = settings.get(attributeKey).get(OptimizerComponentConstants.DEFAULT_VALUE_KEY);
                    }
                    if (settings.get(attributeKey).get(OptimizerComponentConstants.DATA_TYPE_KEY).equalsIgnoreCase("String")) {
                        fw.append(apostroph + value + "'\n");
                    } else {
                        fw.append(value + "\n");
                    }
                }
            }
            addAlgorithmsToFile(fw, algos);
            
            fw.flush();
            fw.close();
        } catch (IOException e) {
            LOGGER.error("Could not create configuration file", e);
        }

    }

    protected abstract void addAlgorithmsToFile(BufferedWriter fw, String[] algos) throws IOException;

    private void appendBoundList(Map<String, Double> list, BufferedWriter fw, List<String> orderedInputValueKeys, int countConstraints)
        throws IOException {
        int i;
        i = 1;
        for (String key : orderedInputValueKeys) {
            if (compContext.getDynamicInputIdentifier(key).equals(OptimizerComponentConstants.ID_CONSTRAINT)
                && !key.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                if (compContext.getInputDataType(key) == DataType.Vector) {
                    for (int j = 0; j < Integer.parseInt(compContext.getInputMetaDataValue(key,
                        OptimizerComponentConstants.METADATA_VECTOR_SIZE)); j++) {
                        fw.append("" + list.get(key + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + j));
                        if (j < Integer.parseInt(compContext.getInputMetaDataValue(key,
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE)) - 1) {
                            fw.append(comma);
                        }
                    }
                } else {
                    fw.append("" + list.get(key));
                }
                if (i++ < countConstraints) {
                    fw.append(comma);
                }
            }
        }
    }

    @Override
    public void readOutputFileFromExternalProgram(Map<String, TypedDatum> outputValueMap) throws IOException {
        File outputFile = new File(messageFromClient.getCurrentWorkingDir()).listFiles()[0];
        BufferedReader fr = new BufferedReader(new FileReader(outputFile));
        String firstLine = fr.readLine();
        int varCount = 0;
        try {
            varCount = Integer.parseInt(firstLine);
        } catch (NumberFormatException e) {
            e.getCause();
        }
        Map<String, Double> newOutput = new HashMap<String, Double>();
        for (int i = 0; i < varCount; i++) {
            String x = fr.readLine();
            newOutput.put("" + i, Double.parseDouble(x));
        }
        gradRequest = Boolean.parseBoolean(fr.readLine());
        fr.close();
        List<String> orderedOutputValueMapKeys = new ArrayList<String>(outputValueMap.keySet());
        Collections.sort(orderedOutputValueMapKeys);
        int offset = 0;
        for (int i = 0; i < orderedOutputValueMapKeys.size(); i += offset) {
            String key = orderedOutputValueMapKeys.get(i);
            offset = 1;
            if (key.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)) {
                key = key.substring(0, key.lastIndexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL));
            }
            if (compContext.getOutputDataType(key) == DataType.Vector) {
                VectorTD resultVec = typedDatumFactory.createVector(Integer.parseInt(compContext.getOutputMetaDataValue(
                    key, OptimizerComponentConstants.METADATA_VECTOR_SIZE)));
                for (int j = 0; j < resultVec.getRowDimension(); j++) {
                    resultVec.setFloatTDForElement(typedDatumFactory.createFloat(newOutput.get("" + j)), j);
                }
                outputValueMap.put(key, resultVec);
                offset++;
            } else {
                outputValueMap.put(key, typedDatumFactory.createFloat(newOutput.get("" + i)));
            }
        }
    }

    @Override
    public boolean getDerivativedNeeded() {
        return gradRequest;
    }

    @Override
    protected void writeInputFileforExternalProgram(Map<String, Double> functionVariables,
        Map<String, Double> functionVariablesGradients,
        Map<String, Double> constraintVariables,
        String outputFileName)
        throws IOException {
        File rceInputFile = new File(messageFromClient.getCurrentWorkingDir() + File.separator + outputFileName);
        if (!rceInputFile.exists()) {
            rceInputFile.createNewFile();
        }
        String objectiveFunctions = "";
        FileWriter fw2 = new FileWriter(rceInputFile);

        List<String> orderedFunctionVariableKeys = new ArrayList<String>(functionVariables.keySet());
        Collections.sort(orderedFunctionVariableKeys);
        List<String> orderedConstraintVariableKeys = new ArrayList<String>(constraintVariables.keySet());
        Collections.sort(orderedConstraintVariableKeys);
        List<String> orderedFunctionVariablesGradientKeys = new ArrayList<String>(functionVariablesGradients.keySet());
        Collections.sort(orderedFunctionVariablesGradientKeys);

        for (String key : orderedFunctionVariableKeys) {
            String name = key;
            if (name.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)) {
                name = name.substring(0, name.lastIndexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL));
            }
            if (compContext.getInputMetaDataValue(name, OptimizerComponentConstants.META_GOAL).equals("Maximize")) { // Maximize
                // Optimizer only minimizes functions so for maximizing you need to minimize -f(x)
                objectiveFunctions += ("-");
            }
            objectiveFunctions += (functionVariables.get(key) + comma);

        }
        fw2.append(objectiveFunctions.substring(0, objectiveFunctions.length() - 1) + IOUtils.LINE_SEPARATOR);
        String constraintFunctions = "";

        for (String key : orderedConstraintVariableKeys) {
            constraintFunctions += "" + constraintVariables.get(key) + comma;
        }
        if (constraintFunctions.length() > 0) {
            fw2.append(constraintFunctions.substring(0, constraintFunctions.length() - 1) + IOUtils.LINE_SEPARATOR);
        } else {
            fw2.append(IOUtils.LINE_SEPARATOR);
        }
        for (String key : orderedFunctionVariableKeys) {
            fw2.append("[");
            for (String variable : orderedFunctionVariablesGradientKeys) {
                if (variable.contains(
                    OptimizerComponentConstants.GRADIENT_DELTA + key + "." + OptimizerComponentConstants.GRADIENT_DELTA)) {
                    fw2.append(" " + functionVariablesGradients.get(variable));
                }
            }
            fw2.append(" ]:" + key + IOUtils.LINE_SEPARATOR);
        }
        fw2.append(IOUtils.LINE_SEPARATOR);
        for (String key : orderedConstraintVariableKeys) {
            fw2.append("[");
            for (String variable : orderedFunctionVariablesGradientKeys) {
                if (variable.contains(
                    OptimizerComponentConstants.GRADIENT_DELTA + key + "." + OptimizerComponentConstants.GRADIENT_DELTA)) {
                    fw2.append(" " + functionVariablesGradients.get(variable));
                }
            }
            fw2.append(" ]:" + key + IOUtils.LINE_SEPARATOR);
        }
        fw2.flush();
        fw2.close();
    }
}
