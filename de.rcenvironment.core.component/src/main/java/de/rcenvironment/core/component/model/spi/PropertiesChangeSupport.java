/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.spi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;


/**
 * Base class elements that implements property change support.
 *
 * @author Heinrich Wendel
 * @author Doreen Seider
 * 
 * Note: Model changes (WorkflowDescription, etc.) are announced but the underlying system evolved over time and seems to get a bit
 * eroded. It starts with the identifiers of the events announced (no scheme behind and constants spreaded) and includes also
 * missing overview which events are announced when. I can imagine that there might by too many events overall. --seid_do
 */
public abstract class PropertiesChangeSupport {

    /** Delegate used to implement property-change-support. */
    private transient PropertyChangeSupport pcsDelegate = new PropertyChangeSupport(this);
    
    /**
     * Adds a new PropertyChangeListener.
     * @param listener The PropertyChangeListener.
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        pcsDelegate.addPropertyChangeListener(listener);
    }

    /**
     * Report a property change to registered listeners.
     * @param property the programmatic name of the property that changed
     */
    public void firePropertyChange(String property) {
        if (pcsDelegate.hasListeners(property)) {
            pcsDelegate.firePropertyChange(property, null, null);
        }
    }

    /**
     * Report a property change to registered listeners.
     * @param property the programmatic name of the property that changed
     * @param newValue the new value.
     */
    public void firePropertyChange(String property, Object newValue) {
        if (pcsDelegate.hasListeners(property)) {
            pcsDelegate.firePropertyChange(property, null, newValue);
        }
    }

    /**
     * Report a property change to registered listeners.
     * @param property the programmatic name of the property that changed
     * @param oldValue the old value
     * @param newValue the new value
     */
    public void firePropertyChange(String property, Object oldValue, Object newValue) {
        if (pcsDelegate.hasListeners(property)) {
            final PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
            /*
             * Custom implementation required, as some PropertyChangeEvents might be ommited by
             * PropertyChangeSupport if oldValue and newValue are equal to each other (compared via
             * the equal-function), which might be an undesired behaviour.
             */
            // code adopted from JavaDoc of PropertyChangeSupport
            PropertyChangeListener[] listeners = pcsDelegate.getPropertyChangeListeners();
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] instanceof PropertyChangeListenerProxy) {
                    PropertyChangeListenerProxy proxy =
                        (PropertyChangeListenerProxy) listeners[i];
                    if (proxy.getPropertyName().equals(property)) {
                        listeners[i].propertyChange(event);
                    }
                } else {
                    listeners[i].propertyChange(event);
                }
            }
        }
    }
    
    /**
     * Remove a PropertyChangeListener from this component.
     * @param listener a PropertyChangeListener instance
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        pcsDelegate.removePropertyChangeListener(listener);
    }
    
    protected PropertyChangeListener[] getPropertyChangeListeners() {
        return pcsDelegate.getPropertyChangeListeners();
    }
}
