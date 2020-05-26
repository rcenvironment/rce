/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.view;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * Abstract base class providing a basic property change event infrastructure. This class can be
 * used to derive configuration container classes from.
 * 
 * @author Christian Weiss
 */
public abstract class AbstractConfiguration implements Serializable {

    /** The serialVersionUID. */
    private static final long serialVersionUID = 1998594795785763259L;

    /** The {@link PropertyChangeSupport} realizing the property change event infrastructure. */
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(
            this);

    /**
     * Validates the configuration.
     * 
     * @return the error message, in case the validation failed, null otherwise
     */
    public abstract String validate();

    /**
     * Add a PropertyChangeListener to the listener list. The listener is registered for all
     * properties. The same listener object may be added more than once, and will be called as many
     * times as it is added. If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     * 
     * @param listener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property. The listener will be invoked only when
     * a call on firePropertyChange names that specific property. The same listener object may be
     * added more than once. For each property, the listener will be invoked the number of times it
     * was added for that property. If <code>propertyName</code> or <code>listener</code> is null,
     * no exception is thrown and no action is taken.
     * 
     * @param propertyName The name of the property to listen on.
     * @param listener The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list. This removes a PropertyChangeListener
     * that was registered for all properties. If <code>listener</code> was added more than once to
     * the same event source, it will be notified one less time after being removed. If
     * <code>listener</code> is null, or was never added, no exception is thrown and no action is
     * taken.
     * 
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property. If <code>listener</code> was added
     * more than once to the same event source for the specified property, it will be notified one
     * less time after being removed. If <code>propertyName</code> is null, no exception is thrown
     * and no action is taken. If <code>listener</code> is null, or was never added for the
     * specified property, no exception is thrown and no action is taken.
     * 
     * @param propertyName The name of the property that was listened on.
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Fire property change.
     * 
     * @param propertyName the property name
     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void firePropertyChange(final String propertyName,
            Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Report a bound property update to any registered listeners. No event is fired if old and new
     * are equal and non-null.
     * 
     * <p>
     * This is merely a convenience wrapper around the more general firePropertyChange method that
     * takes {@code PropertyChangeEvent} value.
     * 
     * @param propertyName The programmatic name of the property that was changed.
     * @param oldValue The old value of the property.
     * @param newValue The new value of the property.
     */
    protected void fireIndexedPropertyChange(final String propertyName,
            final int index, Object oldValue, Object newValue) {
        changeSupport.fireIndexedPropertyChange(propertyName, index, oldValue,
                newValue);
    }

}
