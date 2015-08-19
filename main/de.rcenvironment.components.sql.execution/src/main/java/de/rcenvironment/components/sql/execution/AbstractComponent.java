/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.execution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.channel.legacy.VariantArray;

/**
 * Abstract implementation of {@link Component}.
 * 
 * @author Christian Weiss
 */
public abstract class AbstractComponent extends DefaultComponent {

    /**
     * The key for the context 'static', which stores values that are shared between the iterations
     * of a component. Shared values must be thread-safe, if a component is enabled for concurrent
     * runs!
     */
    public static final String CONTEXT_STATIC = "static";

    /** The key for the context 'config'. */
    public static final String CONTEXT_CONFIG = "config";

    /** The key for the context 'input'. */
    public static final String CONTEXT_INPUT = "input";

    /** The key for the context 'output'. */
    public static final String CONTEXT_OUTPUT = "output";

    /** The key for the context 'run'. */
    public static final String CONTEXT_RUN = "run";

    /** The key for the context 'env'. */
    public static final String CONTEXT_ENV = "env";

    /** The key for the context 'system'. */
    public static final String CONTEXT_SYSTEM = "system";

    protected static final boolean PARALLEL_DEFAULT = false;

    protected static final String VARIABLE_PATTERN_STRING = "^.*(\\$\\{((?:[a-zA-Z0-9]+\\:)?[-_a-zA-Z0-9]*(?:\\[[0-9]+\\])*)\\}).*$";

    protected static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE_PATTERN_STRING, Pattern.DOTALL);

    protected final Log logger = LogFactory.getLog(getClass());

    protected final Context defaultContext = new Context();
    
    protected ComponentContext componentContext;

    private final ThreadLocal<Context> contextContainer = new ThreadLocal<Context>();

    private final boolean parallel;

    private final Semaphore parallelizationSemaphore = new Semaphore(1);

    private boolean prepared = false;

    private final CyclicInputConsumptionStrategy inputConsumptionStrategy;
    
    public AbstractComponent(final CyclicInputConsumptionStrategy inputConsumptionStrategy) {
        this(inputConsumptionStrategy, PARALLEL_DEFAULT);
    }

    public AbstractComponent(final CyclicInputConsumptionStrategy inputConsumptionStrategy, final boolean parallel) {
        this.inputConsumptionStrategy = inputConsumptionStrategy;
        this.parallel = parallel;
    }

    private void setContext(final Context context) {
        contextContainer.set(context);
    }

    protected boolean hasContext() {
        final Context context = contextContainer.get();
        return context != null;
    }

    /**
     * The {@link Context} is linked to the current execution of the component. This relationship is
     * realized via a {@link ThreadLocal}.
     * 
     * <p><b>Be aware</b>, that changing the thread during the execution of a component requires a transfer of the
     * context or the context will not be accessible in the other thread.</p>
     * 
     * @return the current {@link Context}
     * @throws IllegalStateException in case no current execution is running in the current thread
     */
    protected Context getContext() throws IllegalStateException {
        final Context context = contextContainer.get();
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    private void assertState() {
        if (!prepared) {
            throw new IllegalStateException();
        }
    }

    protected ComponentContext getComponentContext() {
        return componentContext;
    }

    protected String replace(final String string) {
        assertState();
        String result = string;
        do {
            final Matcher matcher = VARIABLE_PATTERN.matcher(result);
            if (matcher.matches()) {
                final String variablePlaceholder = matcher.group(1).replaceAll("([\\$\\{\\}\\[\\]])", "\\\\$0");
                final String variableName = matcher.group(2);
                final Object variableValue = getVariableValue(variableName);
                final String replacementValue;
                if (variableValue != null) {
                    replacementValue = variableValue.toString().replaceAll("\\\\", "\\\\\\\\");
                } else {
                    replacementValue = "";
                }
                result = result.replaceAll(variablePlaceholder, replacementValue);
            } else {
                break;
            }
        } while (true);
        return result;
    }

    protected Object getVariableValue(final String variableIdentifier) {
        assertState();
        // extract SCOPE
        final String scope;
        final String variableCoordinate;
        if (variableIdentifier.contains(":")) {
            final int periodIndex = variableIdentifier.indexOf(":");
            scope = variableIdentifier.substring(0, periodIndex);
            variableCoordinate = variableIdentifier.substring(periodIndex + 1);
        } else {
            scope = null;
            variableCoordinate = variableIdentifier;
        }
        // extract INDICES
        final boolean isIndexedVariable = variableCoordinate.contains("[");
        final String variableKey;
        final int[] indexs;
        if (!isIndexedVariable) {
            variableKey = variableCoordinate;
            indexs = new int[0];
        } else {
            final int indexsStartPosition = variableCoordinate.indexOf('[');
            variableKey = variableCoordinate.substring(0, indexsStartPosition);
            final String[] indexStrings =
                variableCoordinate.substring(indexsStartPosition + 1, variableCoordinate.length() - 1).split("\\]\\[");
            indexs = new int[indexStrings.length];
            for (int indexIndex = 0; indexIndex < indexStrings.length; ++indexIndex) {
                indexs[indexIndex] = Integer.parseInt(indexStrings[indexIndex]);
            }
        }
        // prepare result
        final Object interimResult;
        if (scope != null) {
            interimResult = getScopedVariableValue(scope, variableKey);
        } else {
            interimResult = getScopelessVariableValue(variableKey);
        }
        final Object result;
        if (isIndexedVariable) {
            result = extractIndexedValue(interimResult, indexs);
        } else {
            result = interimResult;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getVariableValue(final String variableName, final Class<T> clazz) {
        final Object value = getVariableValue(variableName);
        final T result;
        if (value == null || clazz.isAssignableFrom(value.getClass())) {
            result = (T) value;
        } else {
            throw new RuntimeException("Type of variable content does not match desired type.");
        }
        return result;
    }

    /**
     * Returns a list of available {@link Scope} names to be used for looking up variables.
     * 
     * @return a list of {@link Scope} names
     */
    protected List<String> getScopes() {
        final List<String> values =
            Arrays.asList(CONTEXT_STATIC, CONTEXT_CONFIG, CONTEXT_INPUT, CONTEXT_RUN, CONTEXT_ENV, CONTEXT_SYSTEM, CONTEXT_OUTPUT);
        final List<String> result = new LinkedList<String>(values);
        return result;
    }

    /**
     * Look up the value of a variable with the specified variable name, as no {@link Scope} is
     * specified, use all {@link Scope}s returned by {@link #getScopes()}.
     * 
     * @param variableName the name of the variable
     * @return the value of the variable
     */
    protected Object getScopelessVariableValue(final String variableName) {
        Object result = null;
        final List<String> scopeKeys = getScopes();
        for (final String scopeKey : scopeKeys) {
            result = getScopedVariableValue(scopeKey, variableName);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    protected Object getScopedVariableValue(final String scope, final String variableName) {
        if (scope == null) {
            throw new IllegalArgumentException();
        }
        assertState();
        final Object result;
        final List<String> scopeKeys = getScopes();
        if (scope == null || scope.isEmpty()) {
            Object result2 = null;
            for (final String scopeKey : scopeKeys) {
                result2 = getScopedVariableValue(scopeKey, variableName);
                if (result2 != null) {
                    break;
                }
            }
            result = result2;
        } else {
            final Context context;
            if (hasContext()) {
                context = getContext();
            } else {
                context = defaultContext;
            }
            Object result2 = null;
            boolean found = false;
            for (final String scopeKey : scopeKeys) {
                if (scope.equals(scopeKey)) {
                    found = true;
                    result2 = context.get(scopeKey, variableName);
                    break;
                }
            }
            if (found) {
                result = result2;
            } else {
                throw new IllegalArgumentException(String.format("Unknown scope '%s'", scope));
            }
        }
        return result;
    }

    protected Object extractIndexedValue(final Object object, final int... indexs) {
        assertState();
        final Serializable result;
        if (object == null) {
            result = null;
        } else if (object instanceof VariantArray) {
            final VariantArray variantArray = (VariantArray) object;
            result = variantArray.getValue(indexs).getValue();
        } else {
            throw new RuntimeException(String.format("Unknown data type for indexed access: '%s'", object.getClass()));
        }
        return result;
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }
    
    @Override
    public void start() throws ComponentException {
        this.inputConsumptionStrategy.setInputDefinitions(componentContext.getInputs());
        // make sure, the Scopes exist
        for (final String scope : getScopes()) {
            defaultContext.set(scope, null, null);
        }
        /*
         * Mark this component instance as prepared successfully. The rest of the preparation is
         * wrapped in a try-catch-block, so unsuccessfull preparation resets this attribute to
         * false.
         */
        this.prepared = true;
        try {
            /*
             * Set some default values in the static context.
             */
            final String dash = "-";
            defaultContext.set(CONTEXT_STATIC, "componentContext", componentContext);
            defaultContext.set(CONTEXT_STATIC, "componentExeId", componentContext.getExecutionIdentifier());
            defaultContext.set(CONTEXT_STATIC, "componentExeIdSafe", componentContext.getExecutionIdentifier().replace(dash, ""));
            defaultContext.set(CONTEXT_STATIC, "componentInstanceName", componentContext.getComponentName());
            defaultContext.set(CONTEXT_STATIC, "workflowExeId", componentContext.getWorkflowExecutionIdentifier());
            defaultContext.set(CONTEXT_STATIC, "workflowExeIdSafe", componentContext.getWorkflowExecutionIdentifier().replace(dash, ""));
            defaultContext.set(CONTEXT_STATIC, "workflowInstanceName", componentContext.getWorkflowInstanceName());
            /*
             * Set the Outputs to the output context and prevent modifications through locking it for
             * write-access.
             */
            for (final String outputName : componentContext.getOutputs()) {
                defaultContext.set(CONTEXT_OUTPUT, outputName, outputName);
            }
            defaultContext.setLocked(CONTEXT_OUTPUT, true); // LOCK!
            /*
             * Set the configuration property values to the config context and prevent modifications
             * through locking it for write-access.
             */
            for (final String key : componentContext.getConfigurationKeys()) {
                defaultContext.set(CONTEXT_CONFIG, key, componentContext.getConfigurationValue(key));
            }
            defaultContext.setLocked(CONTEXT_CONFIG, true); // LOCK!
        } catch (final RuntimeException e) {
            this.prepared = false;
            throw e;
        }
        
        startInComponent();

        try {
            startRun();
            final SafeExecutor executor = new SafeExecutor() {
                @Override
                protected Boolean execute() throws ComponentException {
                    return runInitialInComponent(componentContext.getInputs().size() > 0);
                }
            };
            executor.executeSafely();
        } finally {
            endRun();
        }
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return isSqlInitQueryEnabled();
    }
    
    protected abstract boolean isSqlInitQueryEnabled();
    
    protected abstract void startInComponent() throws ComponentException;
    
    protected abstract boolean runInitialInComponent(boolean inputsConnected) throws ComponentException;
    
    @Override
    public void processInputs() throws ComponentException {
        try {
            startRun();
            for (String inputName : componentContext.getInputsWithDatum()) {
                inputConsumptionStrategy.addInput(new Input(inputName, componentContext.getInputDataType(inputName),
                    componentContext.readInput(inputName), componentContext.getWorkflowInstanceName(),
                    componentContext.getExecutionIdentifier(), 0));
            }
            final Map<String, List<Input>> inputsValues = inputConsumptionStrategy.popInputs();
            registerInputContextValues(inputsValues);
            final SafeExecutor executor = new SafeExecutor() {
                @Override
                protected Boolean execute() throws ComponentException {
                    return runStepInComponent(inputsValues);
                }
            };
            
            executor.executeSafely();
        } finally {
            endRun();
        }
    }

    private void startRun() {
        try {
            if (!parallel) {
                parallelizationSemaphore.acquire();
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        final Context context = defaultContext.clone();
        context.set(CONTEXT_RUN, "startTime", System.currentTimeMillis());
        setContext(context);
    }

    private void endRun() {
        setContext(null);
        parallelizationSemaphore.release();
    }

    @SuppressWarnings("unchecked")
    private void registerInputContextValues(final Map<String, List<Input>> inputValues) {
        final Context context = getContext();
        for (final String inputName : inputValues.keySet()) {
            final List<Input> inputs = inputValues.get(inputName);
            final Object value;
            if (inputs.size() > 1) {
                value = new ArrayList<Serializable>(inputs.size());
                for (int index = 0; index < inputs.size(); ++index) {
                    ((List<TypedDatum>) value).add(inputs.get(index).getValue());
                }
            } else {
                value = inputs.get(0).getValue();
            }
            context.set(CONTEXT_INPUT, inputName, value);
        }
    }

    protected abstract boolean runStepInComponent(Map<String, List<Input>> inputValues) throws ComponentException;

    protected Set<String> getInputs(final DataType type) {
        assertState();
        final Set<String> result = new HashSet<>();
        for (final String inputName : componentContext.getInputs()) {
            if (type == null || type == componentContext.getInputDataType(inputName)) {
                result.add(inputName);
            }
        }
        return result;
    }

    protected Set<String> getOutputs() {
        return Collections.unmodifiableSet(componentContext.getOutputs());
    }

    protected Set<String> getOutputs(final DataType type) {
        assertState();
        final Set<String> result = new HashSet<>();
        for (final String outputName : componentContext.getOutputs()) {
            if (type == null || type == componentContext.getOutputDataType(outputName)) {
                result.add(outputName);
            }
        }
        return result;
    }

    protected boolean hasProperty(final String key) {
        return componentContext.getConfigurationKeys().contains(key); 
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(": ");
        builder.append(componentContext.getInstanceName());
        final String result = builder.toString();
        return result;
    }
    
    protected Set<String> getEndpointNames(Set<EndpointDescription> descs) {
        Set<String> names = new HashSet<String>();
        for (EndpointDescription desc : descs) {
            names.add(desc.getName());
        }
        return names;
    }

    /**
     * A named context for variables.
     *
     * @author Christian Weiss
     */
    protected class Scope extends HashMap<String, Object> {

        private static final long serialVersionUID = 8140134377270842306L;

        private final String name;

        private boolean locked;

        protected Scope(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(final boolean locked) {
            this.locked = locked;
        }

        @Override
        public Object put(final String key, final Object value) {
            if (locked) {
                throw new RuntimeException(String.format("Scope '%s' is locked for write-access.", name));
            }
            return super.put(key, value);
        }

        public <T> T get(final String key, final Class<T> type) {
            final Object resultObject = get(key);
            final T result;
            if (resultObject == null) {
                result = null;
            } else if (type.isAssignableFrom(resultObject.getClass())) {
                result = type.cast(resultObject);
            } else {
                throw new RuntimeException(String.format("Type '%s' can not be cast to '%s'.",
                        resultObject.getClass().getCanonicalName(), type.getCanonicalName()));
            }
            return result;
        }

    }

    /**
     * A container for multiple {@link Scope}s.
     *
     * @author Christian Weiss
     */
    protected class Context implements Cloneable {

        private final Map<String, Scope> scopes = new HashMap<String, Scope>();

        public void set(final String scope, final String key, final Object value) {
            if (!scopes.containsKey(scope)) {
                scopes.put(scope, new Scope(scope));
            }
            scopes.get(scope).put(key, value);
        }

        public Object get(final String scope, final String key) {
            assertScopeExists(scope);
            final Object result;
            result = scopes.get(scope).get(key);
            return result;
        }

        public <T> T get(final String scope, final String key, final Class<T> type) {
            assertScopeExists(scope);
            final T result;
            result = scopes.get(scope).get(key, type);
            return result;
        }

        public Set<String> scopeKeys() {
            return Collections.unmodifiableSet(scopes.keySet());
        }

        public boolean hasScope(final String key) {
            return scopes.containsKey(key);
        }

        public Map<String, Object> getScope(final String scope) {
            assertScopeExists(scope);
            return Collections.unmodifiableMap(scopes.get(scope));
        }

        public boolean isLocked(final String scope) {
            assertScopeExists(scope);
            return scopes.get(scope).isLocked();
        }

        public void setLocked(final String scope, final boolean locked) {
            assertScopeExists(scope);
            scopes.get(scope).setLocked(locked);
        }

        private void assertScopeExists(final String scope) {
            if (!scopes.containsKey(scope)) {
                throw new NoSuchElementException(String.format("Scope '%s' is unknown.", scope));
            }
        }

        @Override
        protected Context clone() {
            final Context clone = new Context();
            for (final String scope : scopes.keySet()) {
                for (final String key : getScope(scope).keySet()) {
                    clone.set(scope, key, get(scope, key));
                }
            }
            return clone;
        }

    }

    /**
     * An execution wrapper wrapping the execution of script and wrapping all occuring exceptions in
     * a {@link ComponentException}.
     * 
     * @author Christian Weiss
     */
    private abstract class SafeExecutor {

        protected final Boolean executeSafely() throws ComponentException {
            try {
                return executeHandlingExceptions();
            } catch (final ComponentException e) {
                handleException(e);
                throw e;
            }
        }

        /**
         * Calls {@link #execute()} and unwraps {@link ComponentExceptions} wrapped in
         * {@link RuntimeException} and wraps {@link RuntimeExceptions} in
         * {@link ComponentRunExceptions}.
         * 
         * @return the result of {@link #execute()}
         * @throws ComponentException if {@link #execute()} threw any exception
         */
        private Boolean executeHandlingExceptions() throws ComponentException {
            try {
                return execute();
            } catch (final RuntimeException e) {
                final ComponentException exception;
                final Throwable cause = e.getCause();
                if (cause != null && cause instanceof ComponentException) {
                    // unwrap wrapped ComponentExceptions
                    exception = (ComponentException) cause;
                } else {
                    // wrap RuntimeExceptions in a ComponentRunException
                    exception = new ComponentException(e);
                }
                throw exception;
            }
        }

        protected abstract Boolean execute() throws ComponentException;

        protected void handleException(final ComponentException exception) {
            logger.error(exception);
        }
        
    }

}
