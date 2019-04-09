/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.spi.module.ClassFilter;
import de.rcenvironment.toolkit.core.spi.module.DefaultClassFilter;
import de.rcenvironment.toolkit.core.spi.module.ObjectGraph;

/**
 * Abstract base class for {@link ObjectGraph} implementations.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractObjectGraph implements ObjectGraph {

    // mutable; applied to all subsequent class registrations
    private ClassFilter publicInterfaceFilter = new DefaultClassFilter();

    private final Collection<Class<?>> interfacesToPublish = new ArrayList<>();

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public final void registerObject(Object object) {
        registerObject(object, getRelevantInterfaces(object.getClass()));
    }

    @Override
    public final void registerServiceClass(Class<?> implementationClass) {
        Collection<Class<?>> privateCandidates = getRelevantInterfaces(implementationClass);
        Collection<Class<?>> publicInterfaces = selectPublicInterfaces(privateCandidates);
        registerServiceClass(implementationClass, publicInterfaces, privateCandidates);
        interfacesToPublish.addAll(publicInterfaces);
    }

    @Override
    public void setPublicInterfaceFilter(de.rcenvironment.toolkit.core.spi.module.ClassFilter filter) {
        this.publicInterfaceFilter = filter;
    };

    @Override
    public final ImmutableServiceRegistry instantiate() throws ToolkitException {
        return instantiateServices(interfacesToPublish);
    }

    protected abstract void registerObject(Object object, Collection<Class<?>> privateInterfaces);

    protected abstract void registerServiceClass(Object object, Collection<Class<?>> publicInterfaces,
        Collection<Class<?>> privateInterfaces);

    protected abstract ImmutableServiceRegistry instantiateServices(Collection<Class<?>> serviceInterfaces) throws ToolkitException;

    /**
     * Hides the concrete log API from implementing classes; simple DEBUG messages should be sufficient.
     */
    protected final void logDebug(String text) {
        log.debug(text);
    }

    private Collection<Class<?>> getRelevantInterfaces(Class<? extends Object> clazz) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> i : clazz.getInterfaces()) {
            if (!i.getName().startsWith("java")) { // exclude Serializable etc.
                result.add(i);
            }
        }
        return result;
    }

    private Collection<Class<?>> selectPublicInterfaces(Collection<Class<?>> privateCandidates) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> i : privateCandidates) {
            if (publicInterfaceFilter.accept(i)) {
                result.add(i);
            }
        }

        // logDebug(String.format("Selected interfaces %s to publish (out of %s) using filter %s", result, privateCandidates,
        // publicInterfaceFilter.getClass()));
        return result;
    }
}
