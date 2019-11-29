/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.objectbindings.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionContributor;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsConsumer;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;
import de.rcenvironment.toolkit.utils.common.AutoCreationMap;
import de.rcenvironment.toolkit.utils.internal.StringUtils;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * For simplicity, all methods of this class are currently synchronized. This should not add significant blocking, as object bindings are
 * not expected to change rapidly, and consumer callback methods should terminate quickly. If this still turns out to be a problem in some
 * application, locking could be reworked to a per-binding-class approach.
 * 
 * @author Robert Mischke
 * 
 */
public class ObjectBindingsServiceImpl implements ObjectBindingsService {

    private final Map<Class<?>, ObjectBindingsConsumer<?>> consumers = new HashMap<>();

    private final AutoCreationMap<Class<?>, List<Object>> bindingLists =
        new AutoCreationMap<Class<?>, List<Object>>() {

            @Override
            protected List<Object> createNewEntry(Class<?> key) {
                return new LinkedList<>();
            }
        };

    private final Log log = LogFactory.getLog(getClass());

    public ObjectBindingsServiceImpl(StatusCollectionRegistry statusCollectionRegistry) {
        statusCollectionRegistry.addContributor(new StatusCollectionContributor() {

            @Override
            public void printUnfinishedOperationsInformation(TextLinesReceiver receiver) {}

            @Override
            public void printDefaultStateInformation(TextLinesReceiver receiver) {
                printStatus(receiver);
            }

            @Override
            public String getUnfinishedOperationsDescription() {
                return null;
            }

            @Override
            public String getStandardDescription() {
                return "Object Bindings";
            }
        });
    }

    @Override
    public synchronized <T> void addBinding(Class<T> bindingClass, T instance, Object owner) {
        ObjectBindingsConsumer<T> existingConsumer = getExistingConsumer(bindingClass);
        // TODO check for duplicate binding
        bindingLists.get(bindingClass).add(instance);
        log.debug(StringUtils.format("Added binding for %s: %s", bindingClass.getName(), instance.toString()));
        if (existingConsumer != null) {
            log.debug(StringUtils.format("Reporting new binding for %s to existing consumer %s", bindingClass.getName(),
                existingConsumer.toString()));
            existingConsumer.addInstance(instance);
        }
    }

    @Override
    public synchronized <T> void removeBinding(Class<T> bindingClass, T instance) {
        List<Object> bindingList = bindingLists.get(bindingClass);
        // note: List#remove() will *not* work properly instead; see method comment
        if (!removeByIdentity(instance, bindingList)) {
            throw new IllegalStateException("This object instance was not registered/bound before: " + instance);
        }
        log.debug(StringUtils.format("Removed binding for %s: %s", bindingClass.getName(), instance.toString()));
        ObjectBindingsConsumer<T> existingConsumer = getExistingConsumer(bindingClass);
        if (existingConsumer != null) {
            log.debug(StringUtils.format("Reporting removal of binding for %s to existing consumer %s", bindingClass.getName(),
                existingConsumer.toString()));
            existingConsumer.removeInstance(instance);
        }
    }

    @Override
    public synchronized void removeAllBindingsOfOwner(Object owner) {
        log.debug("Note: batch removal of bindings not implemented yet"); // TODO (p2) implement
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> void setConsumer(Class<T> bindingClass, ObjectBindingsConsumer<T> newConsumer) {

        List<T> boundInstances = (List<T>) bindingLists.get(bindingClass);

        // unregister instances from old consumer, unless null
        ObjectBindingsConsumer<T> previousConsumer = getExistingConsumer(bindingClass);
        if (previousConsumer != null) {
            for (Object instance : boundInstances) {
                previousConsumer.removeInstance((T) instance);
            }
        }

        // unregister instances with new consumer, unless null
        if (newConsumer != null) {
            for (Object instance : boundInstances) {
                newConsumer.addInstance((T) instance);
            }
        }

        log.debug(StringUtils.format("Setting the consumer of %s instances to %s",
            bindingClass.getName(), newConsumer)); // note: consumer may be null
        consumers.put(bindingClass, newConsumer);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectBindingsConsumer<T> getExistingConsumer(Class<T> bindingClass) {
        return (ObjectBindingsConsumer<T>) consumers.get(bindingClass);
    }

    private <T> boolean removeByIdentity(T instance, List<Object> bindingList) {
        // this looping approach is required instead of List#remove() to ensure object *identity* is used;
        // otherwise, equal (but not identical) bindings will get mixed up - misc_ro
        Iterator<Object> iterator = bindingList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == instance) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private synchronized void printStatus(TextLinesReceiver receiver) {
        for (Entry<Class<?>, List<Object>> e : bindingLists.getImmutableShallowCopy().entrySet()) {
            final Class<?> clazz = e.getKey();
            final String consumedBy;
            final ObjectBindingsConsumer<?> consumer = consumers.get(clazz);
            if (consumer != null) {
                consumedBy = consumer.getClass().getName();
            } else {
                consumedBy = "<none>";
            }
            receiver.addLine(StringUtils.format("%s  (consumed by %s)", clazz.getName(), consumedBy));
            for (Object instance : e.getValue()) {
                receiver.addLine(StringUtils.format("  - %s", instance.getClass().getName()));
            }
        }
    }
}
