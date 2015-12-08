/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopBehaviorInCaseOfFailure;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * The {@link AbstractLoopComponent} is an abstract class that must be used for components that
 * should be able to control a loop.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public abstract class AbstractLoopComponent extends DefaultComponent {

    private static final String RECEIVED_NAV_VALUES = "Received value(s) of type 'Not a value' (i.e., a component in the loop failed) ";

    protected ComponentContext componentContext;
    
    protected ComponentLog componentLog;

    protected TypedDatumFactory typedDatumFactory;
    
    protected LoopBehaviorInCaseOfFailure loopBehaviorInCaseOfFailure;
    
    protected boolean loopFailureRequested = false;
    
    protected boolean onlyFailLoopInCaseOfFailure;

    private boolean anyRunFailed = false;

    private int maximumReruns;
    
    private int rerunCount = 0;

    private Map<String, TypedDatum> outputValuesSentInLastLoop = new HashMap<>();

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        this.componentLog = componentContext.getLog();
    }
    
    @Override
    public void start() throws ComponentException {
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        loopBehaviorInCaseOfFailure = LoopBehaviorInCaseOfFailure.fromString(
            componentContext.getConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE));
        onlyFailLoopInCaseOfFailure = Boolean.valueOf(componentContext.getConfigurationValue(LoopComponentConstants.CONFIG_KEY_FAIL_LOOP));
        
        switch (loopBehaviorInCaseOfFailure) {
        case RerunAndFail:
            maximumReruns = getRerunCount(LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_FAIL);
            break;
        case RerunAndDiscard:
            maximumReruns = getRerunCount(LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_DISCARD);
            break;
        default:
            // nothing to do
            break;
        }
        
        startComponentSpecific();
        if (treatStartAsComponentRun()) {
            sendLoopDoneValuesIfDone();
        }
    }
    
    private int getRerunCount(String configKey) throws ComponentException {
        String rerunConfigValue = componentContext.getConfigurationValue(configKey);
        try {
            return Integer.valueOf(rerunConfigValue);
        } catch (NumberFormatException e) {
            if (rerunConfigValue == null || rerunConfigValue.isEmpty()) {
                throw new ComponentException("No number of reruns given");
            } else {
                throw new ComponentException(StringUtils.format("Given number of reruns is invalid: %s", rerunConfigValue));
            }
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        if (hasNaVInputValues(componentContext.getInputsWithDatum())) {
            anyRunFailed = true;
            if (handleFaultTolerance()) {
                return;
            }
        }
        outputValuesSentInLastLoop.clear();
        rerunCount = 0;
        processInputsComponentSpecific();
        if (!isReset()) {
            sendLoopDoneValuesIfDone();
            if (isFinallyDone()) {
                componentContext.closeAllOutputs();
            } else {
                forwardValues();
            }
        }
    }
    
    protected void writeOutput(String outputName, TypedDatum value) {
        LoopEndpointType loopEndpointType = LoopEndpointType.fromString(
            componentContext.getOutputMetaDataValue(outputName, LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE));
        if (loopEndpointType.equals(LoopEndpointType.SelfLoopEndpoint)) {
            outputValuesSentInLastLoop.put(outputName, value);            
        }
        componentContext.writeOutput(outputName, value);
    }

    
    protected boolean hasNaVInputValues(Set<String> inputsWithDatum) {
        for (String inputName : inputsWithDatum) {
            LoopEndpointType loopEndpointType = LoopEndpointType.fromString(
                componentContext.getInputMetaDataValue(inputName, LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE));
            if (loopEndpointType.equals(LoopEndpointType.SelfLoopEndpoint)) {
                TypedDatum typedDatum = componentContext.readInput(inputName);
                if (typedDatum.getDataType().equals(DataType.NotAValue)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    private boolean handleFaultTolerance() throws ComponentException {

        boolean handled = false;
        
        switch (loopBehaviorInCaseOfFailure) {
        case Fail:
            handled = handleFailure(new ComponentException(RECEIVED_NAV_VALUES
                + "-> fail workflow (as defined by behavior declaration in configuration tab 'Fault Tolerance')"));
            break;
        case Discard:
            String inputName1 = guardAgainstNaVValueAtForwardingInputs(componentContext.getInputsWithDatum());
            if (inputName1 != null) {
                handled = handleFailure(new ComponentException(StringUtils.format("Received value of type 'Not a value' at input"
                    + " '%s' that is not allowed to be forwarded; most likely reason: failure in a fault-tolerant loop so that"
                    + " no reasonable value to forward was provided", inputName1)));
            } else {
                componentLog.componentInfo(RECEIVED_NAV_VALUES
                    + "-> discard evaluation loop (as defined by behavior declaration in configuration tab 'Fault Tolerance')");
                handled = false;
            }
            break;
        case RerunAndFail:
            if (rerunCount >= maximumReruns) {
                handled = handleFailure(new ComponentException(StringUtils.format(RECEIVED_NAV_VALUES
                    + " and maximum number of reruns execeeded (%d) "
                    + "-> fail workflow (as defined by behavior declaration in configuration tab 'Fault Tolerance')", maximumReruns)));
            } else {
                rerunLoop();
                handled = true;
            }
            break;
        case RerunAndDiscard:
            if (rerunCount >= maximumReruns) {
                String inputName2 = guardAgainstNaVValueAtForwardingInputs(componentContext.getInputsWithDatum());
                if (inputName2 != null) {
                    handled = handleFailure(new ComponentException(StringUtils.format("Received value of type 'Not a value' at input '%s'"
                        + " that is not allowed to be forwarded; most likely reason: failure in a fault-tolerant loop so that no reasonable"
                        + " value to forward was provided", inputName2)));
                }
            } else {
                rerunLoop();
                handled = true;
            }
            break;
        default:
            // should not happen
            break;
        }
        return handled;
    }
    
    private boolean handleFailure(ComponentException e) throws ComponentException {
        if (onlyFailLoopInCaseOfFailure) {
            loopFailureRequested = true;
            return false;
        } else {
            throw e;
        }
    }
    
    private void rerunLoop() {
        rerunCount++;
        componentLog.componentInfo(StringUtils.format(RECEIVED_NAV_VALUES
            + "-> re-run evaluation loop (rerun count: %d) "
            + "(as defined by behavior declaration in configuration tab 'Fault Tolerance')", rerunCount));
        for (String outputName : outputValuesSentInLastLoop.keySet()) {
            componentContext.writeOutput(outputName, outputValuesSentInLastLoop.get(outputName));
        }
    }
    
    private String guardAgainstNaVValueAtForwardingInputs(Set<String> inputsWithDatum) throws ComponentException {
        for (String inputName : inputsWithDatum) {
            if (componentContext.getDynamicInputIdentifier(inputName).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                TypedDatum typedDatum = componentContext.readInput(inputName);
                if (typedDatum.getDataType().equals(DataType.NotAValue)) {
                    return inputName; 
                }
            }
        }
        return null;
    }

    @Override
    public void reset() throws ComponentException {
        if (loopFailureRequested) {
            resetComponentSpecific();
            loopFailureRequested = false;
            throw new ComponentException(RECEIVED_NAV_VALUES
                + "-> fail loop (as defined by behavior declaration in configuration tab 'Fault Tolerance')");
        } else {
            finishLoopComponentSpecific(false);
            sendLoopDoneValue(true);
            resetComponentSpecific();
        }
    }
    
    protected void addOutputValueSentInSelfLoop(String outputName, TypedDatum value) {
        outputValuesSentInLastLoop.put(outputName, value);
    }

    /**
     * Check if the component has forwarding start values to wait for.
     * 
     * @return true, if there are any.
     */
    protected boolean hasForwardingStartInputs() {
        Set<String> inputs = componentContext.getInputs();
        for (String input : inputs) {
            if (componentContext.getDynamicInputIdentifier(input).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                if (input.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void forwardValues() {
        Set<String> inputs = componentContext.getInputsWithDatum();
        for (String input : inputs) {
            if (componentContext.getDynamicInputIdentifier(input).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                String output;
                if (input.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                    output = input.substring(0, input.indexOf(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX));
                } else {
                    output = input;
                }
                writeOutput(output, componentContext.readInput(input));
            }
        }
    }

    private void sendLoopDoneValuesIfDone() throws ComponentException {
        if (isDone() || isFinallyDone()) {
            if (anyRunFailed && Boolean.valueOf(componentContext.getConfigurationValue(LoopComponentConstants.CONFIG_KEY_FINALLY_FAIL))) {
                throw new ComponentException("Evaluation loop terminated and at least one evaluation loop run failed "
                    + "- fail (as defined by behavior declaration in configuration tab 'Fault Tolerance')");
            }
        }
        if (isDone()) {
            sendLoopDoneValue(true);            
        }
        if (isFinallyDone()) {
            componentContext.writeOutput(LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE,
                typedDatumFactory.createBoolean(true));
        }
    }

    private void sendLoopDoneValue(boolean done) {
        componentContext.writeOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE,
            typedDatumFactory.createBoolean(done));
    }

    protected boolean isReset() {
        return false;
    }
    
    protected abstract void finishLoopComponentSpecific(boolean outerLoopFinished) throws ComponentException;

    /**
     * {@link DefaultComponent#reset()} of components implementing this abstract class.
     */
    protected void resetComponentSpecific() throws ComponentException {}

    /**
     * {@link DefaultComponent#processInputs()} of components implementing this abstract class.
     */
    protected abstract void processInputsComponentSpecific() throws ComponentException;

    /**
     * {@link DefaultComponent#start()} of components implementing this abstract class.
     */
    protected abstract void startComponentSpecific() throws ComponentException;

    /**
     * return <code>true</code> if the loop is terminated and thus, the loop component done,
     * otherwise <code>false</code>.
     */
    protected abstract boolean isFinallyDone();

    /**
     * return <code>true</code> if the loop is terminated and thus, the loop component done,
     * otherwise <code>false</code>.
     */
    protected abstract boolean isDone();
    
}
