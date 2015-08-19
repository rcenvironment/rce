/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Registry for {@link WorkflowNodeValidator}s.
 * 
 * @author Christian Weiss
 */
public class WorkflowNodeValidatorsRegistryImpl implements WorkflowNodeValidatorsRegistry {

    /** The ID of the extension point. */
    public static final String EXTENSION_POINT_ID = "de.rcenvironment.core.gui.workflow.nodeValidators";

    private static final Log LOGGER = LogFactory.getLog(WorkflowNodeValidatorsRegistry.class);

    private ExtensionRegistryListener extensionRegistryListener;

    private final Map<ComponentFilter, InstanceFactory<? extends WorkflowNodeValidator>> validatorMappings =
        new ConcurrentHashMap<ComponentFilter, InstanceFactory<? extends WorkflowNodeValidator>>();

    @Override
    public void initialize() {
        final IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        // listen to registry change events to stay up to date
        extensionRegistryListener = new ExtensionRegistryListener();
        extensionRegistry.addRegistryChangeListener(extensionRegistryListener);
        // parste the extension elements
        final IConfigurationElement[] config = extensionRegistry
            .getConfigurationElementsFor(EXTENSION_POINT_ID);
        for (final IConfigurationElement element : config) {
            try {
                final ComponentFilter filter;
                final Object filterObject = element.createExecutableExtension("filter");
                if (ComponentFilter.class.isAssignableFrom(filterObject.getClass())) {
                    filter = (ComponentFilter) filterObject;
                    // final String validatorClassName = element.getAttribute("class");
                    // FIXME deprecated!
                    // final ClassLoader pluginClassLoader =
                    // element.getDeclaringExtension().getDeclaringPluginDescriptor().getPluginClassLoader();
                    // final Class<? extends WorkflowNodeValidator> validatorClass =
                    // loadClass(validatorClassName, pluginClassLoader);
                    final InstanceFactory<? extends WorkflowNodeValidator> instanceFactory = new InstanceFactory<WorkflowNodeValidator>() {

                        @Override
                        public WorkflowNodeValidator newInstance() {
                            try {
                                return (WorkflowNodeValidator) element.createExecutableExtension("class");
                            } catch (CoreException e) {
                                final String errorMessage = "Failed to load class for WorkflowNodeValidator extension:";
                                LOGGER.error(errorMessage, e);
                                throw new RuntimeException(errorMessage, e);
                            }
                        }

                    };
                    // validatorMappings.put(filter, validatorClass);
                    validatorMappings.put(filter, instanceFactory);
                }
            } catch (final CoreException e) {
                final String errorMessage = "Failed to instantiate filter for WorkflowNodeValidator extension:";
                LOGGER.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
                // } catch (final ClassNotFoundException e) {
                // LOGGER.error("Failed to load class for WorkflowNodeValidator extension.", e);
            }
        }
    }

    // /**
    // * Tries to load the specified class.
    // *
    // * @param validatorClassName
    // * @return
    // * @throws ClassNotFoundException
    // */
    // @SuppressWarnings("unchecked")
    // private Class<? extends WorkflowNodeValidator> loadClass(final String validatorClassName,
    // final ClassLoader classLoader)
    // throws ClassNotFoundException {
    // final Class<?> clazz = classLoader.loadClass(validatorClassName);
    // if (!WorkflowNodeValidator.class.isAssignableFrom(clazz)) {
    // throw new
    // ClassNotFoundException(String.format("%s is not a valid subtype of WorkflowNodeValidator.",
    // validatorClassName));
    // }
    // return (Class<? extends WorkflowNodeValidator>) clazz;
    // }

    @Override
    public synchronized List<WorkflowNodeValidator> getValidatorsForWorkflowNode(final WorkflowNode workflowNode, boolean onWorkflowStart) {
        List<WorkflowNodeValidator> result = new LinkedList<WorkflowNodeValidator>();
        Iterator<ComponentFilter> it = validatorMappings.keySet().iterator();
        while (it.hasNext()) {
            ComponentFilter filter = it.next();
            boolean filterResult = filter.select(workflowNode);
            if (filterResult) {
                // final Class<? extends WorkflowNodeValidator> clazz =
                // validatorMappings.get(filter);
                // try {
                // final WorkflowNodeValidator validator = clazz.newInstance();

                final WorkflowNodeValidator validator = validatorMappings.get(filter).newInstance();
                validator.setWorkflowNode(workflowNode, onWorkflowStart);
                result.add(validator);
                // } catch (final IllegalAccessException e) {
                // LOGGER.error("Failed to instantiate WorkflowNodeValidator instance.", e);
                // } catch (final InstantiationException e) {
                // LOGGER.error("Failed to instantiate WorkflowNodeValidator instance.", e);
                // }
            }
        }
        return result;
    }

    /**
     * An {@link IRegistryChangeListener} to listen to changes concerning the extension point.
     * 
     * @author Christian Weiss
     */
    private class ExtensionRegistryListener implements IRegistryChangeListener {

        @Override
        public void registryChanged(final IRegistryChangeEvent event) {
            System.err.println(event);
        }

    }

    /**
     * Factory interface to encapsulate the creation of new instances from
     * {@link IConfigurationElement}s.
     * 
     * @author Christian Weiss
     */
    private interface InstanceFactory<T> {

        T newInstance();

    }

}
