/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.evaluationmemory.execution.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;


/**
 * A file used by the evaluation memory component to store inputs and associated outputs. Such a file contains five types of entries:
 * 
 * - The version number of the file,
 * 
 * - the type of the component using this file,
 * 
 * - a specification of the inputs to the component, i.e., the design variables,
 * 
 * - a specification of the outputs of the component, i.e., the results of the loop evaluation, and
 * 
 * - records of design variables and their corresponding results.
 * 
 * In order to alleviate access to these different kinds of fields, we extend the Properties-class and provide some convenience methods.
 * 
 * @author Alexander Weinert
 */
class EvaluationMemoryProperties extends Properties {

    private static final String TYPE = "type";

    private static final String VERSION = "version";

    private static final String OUTPUTS = "outputs";

    private static final String INPUTS = "inputs";
    
    String getInputSpecificationKey() {
        return INPUTS;
    }

    String getOutputSpecificationKey() {
        return OUTPUTS;
    }

    void setVersion(final String version) {
        setProperty(VERSION, version);
    }

    void setType(final String type) {
        setProperty(TYPE, type);
    }

    void setInputSpecification(final String inputSpecification) {
        setProperty(INPUTS, inputSpecification);
    }

    void setOutputSpecification(final String outputSpecification) {
        setProperty(OUTPUTS, outputSpecification);
    }

    String getVersion() {
        return getProperty(VERSION);
    }

    String getType() {
        return getProperty(TYPE);
    }

    String getInputSpecification() {
        return getProperty(INPUTS);
    }

    String getOutputSpecification() {
        return getProperty(OUTPUTS);
    }

    Iterable<String> getRecordKeys() {
        final Collection<String> returnValue = new HashSet<>();

        for (Object key : this.keySet()) {
            final String keyString = (String) key;
            // The evaluation memory stores five types of values: The version number, the type of the component, the input and output
            // definition, and the stored values. Hence, it suffices to explicitly skip the former four types of values in order to obtain
            // only the stored values.
            final boolean isVersion = keyString.equals(VERSION);
            final boolean isType = keyString.equals(TYPE);
            final boolean isInputDefinition = keyString.equals(INPUTS);
            final boolean isOutputDefinition = keyString.equals(OUTPUTS);
            final boolean isRecord = (!isVersion && !isType && !isInputDefinition && !isOutputDefinition);
            if (isRecord) {
                returnValue.add(keyString);
            }
        }

        return returnValue;
    }

}
