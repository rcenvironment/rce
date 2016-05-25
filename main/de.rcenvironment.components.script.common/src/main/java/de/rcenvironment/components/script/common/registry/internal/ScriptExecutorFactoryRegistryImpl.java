/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.common.registry.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactory;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactoryRegistry;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Implementation of {@link ScriptExecutorFactoryRegistry}.
 * 
 * @author Sascha Zur
 */
public class ScriptExecutorFactoryRegistryImpl implements ScriptExecutorFactoryRegistry {

    private static final Log LOGGER = LogFactory.getLog(ScriptExecutorFactoryRegistryImpl.class);

    // synchronized methods to avoid removing a factory while other read it. Since this won't happen
    // often, synchronizing the methods is enough
    private final List<ScriptExecutorFactory> executorFactories =
        Collections.synchronizedList(new LinkedList<ScriptExecutorFactory>());

    @Override
    public synchronized void addScriptExecutorFactory(ScriptExecutorFactory factory) {
        if (factory != null && !executorFactories.contains(factory)) {
            executorFactories.add(factory);
        } else {
            LOGGER.warn("Could not register ScriptExecutorFactory.");
        }
    }

    @Override
    public synchronized void removeScriptExecutorFactory(ScriptExecutorFactory factory) {
        if (factory != null && executorFactories.contains(factory)) {
            executorFactories.remove(factory);
        } else {
            LOGGER.warn("Could not register ScriptExecutorFactor.");
        }
    }

    @Override
    public synchronized List<ScriptLanguage> getCurrentRegisteredExecutorLanguages() {
        List<ScriptLanguage> result = new LinkedList<ScriptLanguage>();
        for (ScriptExecutorFactory factory : executorFactories) {
            result.add(factory.getSupportingScriptLanguage());
        }
        return result;
    }

    @Override
    public synchronized ScriptExecutor requestScriptExecutor(ScriptLanguage language) {
        ScriptExecutor result = null;
        for (ScriptExecutorFactory currentFactory : executorFactories) {
            if (currentFactory.getSupportingScriptLanguage().getName().equalsIgnoreCase(language.getName())) {
                result = currentFactory.createScriptExecutor();
            }
        }
        return result;
    }
}
