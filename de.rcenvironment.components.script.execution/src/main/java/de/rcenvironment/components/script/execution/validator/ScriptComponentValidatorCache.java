/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.script.execution.validator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.rcenvironment.components.script.execution.validator.ScriptComponentValidator.PythonValidationResult;

/**
 * This class is a cache for script component validator results, cf. {@link ScriptComponentValidator}. The underlying assumption for this
 * cache is, that for the entire runtime of this RCE instance, Python installations are not changing.
 * 
 * This class and all of its public methods are thread-safe.
 * 
 * @author Thorsten Sommer
 */
public final class ScriptComponentValidatorCache {

    private static final ScriptComponentValidatorCache INSTANCE = new ScriptComponentValidatorCache();

    private final ConcurrentMap<String, PythonValidationResult> cache = new ConcurrentHashMap<String, PythonValidationResult>();

    /**
     * 
     * The default constructor is private, because this cache is implemented as a singleton.
     *
     */
    private ScriptComponentValidatorCache() {}

    /**
     * 
     * Due to the fact that this cache is a singleton, this methods returns the instance.
     * 
     * @return Yields the singleton instance of this cache.
     */
    public static ScriptComponentValidatorCache getCache() {
        return INSTANCE;
    }

    /**
     * 
     * Checks wherever this path was cached i.e. validated.
     * 
     * @param pythonPath The path to test
     * @return True if the given Python's path was validated and cached. False otherwise.
     */
    public boolean isPathValidated(final String pythonPath) {
        if (pythonPath == null) {
            return false;
        }

        return this.cache.containsKey(pythonPath);
    }

    /**
     * 
     * This method tries to find the desired path in the cache and returns the cached result. If this path was not yet validated, the
     * {@link PythonValidationResult.DEFAULT_NONE_PLACEHOLDER} gets returned.
     * 
     * @param pythonPath The python's path
     * @return The cached validation result or {@link PythonValidationResult.DEFAULT_NONE_PLACEHOLDER}
     */
    public PythonValidationResult getValidationResult(final String pythonPath) {
        return this.cache.getOrDefault(pythonPath, PythonValidationResult.DEFAULT_NONE_PLACEHOLDER);
    }
    
    /**
     * 
     * This method adds a {@link PythonValidationResult} to the cache.
     * 
     * @param result The validator result which should be cached
     */
    public void addOrUpdateValidationResult(final PythonValidationResult result) {
        if (result.isPlaceholder()) {
            return;
        }

        final String pythonPath = result.getPythonPath();
        if (this.cache.containsKey(pythonPath)) {
            this.cache.replace(pythonPath, result);
            return;
        }

        this.cache.putIfAbsent(pythonPath, result);
    }
}
