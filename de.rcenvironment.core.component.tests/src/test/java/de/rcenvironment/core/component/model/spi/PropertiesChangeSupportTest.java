/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link PropertiesChangeSupport}.
 * 
 * @author Doreen Seider
 */
public class PropertiesChangeSupportTest {

    private final String propertyName = "prop.key";
    
    private final String oldValue = "oldValue";
    
    private final String newValue = "newValue";

    private PropertyChangeListener listener;

    private PropertiesChangeSupport propChangeSupport = new PropertiesChangeSupportStub();

    private Capture<PropertyChangeEvent> capture;
    
    /**
     * Set up mock objects.
     */
    @Before
    public void setUpMockObjects() {
        listener = EasyMock.createStrictMock(PropertyChangeListener.class);
        capture = new Capture<PropertyChangeEvent>(CaptureType.ALL);
        listener.propertyChange(EasyMock.capture(capture));
        EasyMock.replay(listener);
    }
    
    /**
     * Tests if properties are fired correctly.
     */
    @Test
    public void testFirePropertyChange() {
        
        propChangeSupport.addPropertyChangeListener(listener);
        propChangeSupport.firePropertyChange(propertyName);
        
        assertEquals(1, capture.getValues().size());
        PropertyChangeEvent event = capture.getValues().get(0);

        assertEquals(propertyName, event.getPropertyName());
        assertNull(propertyName, event.getNewValue());
        assertNull(propertyName, event.getOldValue());

        EasyMock.verify(listener);
        
    }

    /**
     * Tests if properties are fired correctly.
     */
    @Test
    public void testFirePropertyChangeOnlyNewValue() {
        
        propChangeSupport.addPropertyChangeListener(listener);
        propChangeSupport.firePropertyChange(propertyName, newValue);
        
        assertEquals(1, capture.getValues().size());
        PropertyChangeEvent event = capture.getValues().get(0);

        assertEquals(propertyName, event.getPropertyName());
        assertEquals(newValue, event.getNewValue());
        assertNull(propertyName, event.getOldValue());

        EasyMock.verify(listener);
        
    }

    /**
     * Tests if properties are fired correctly.
     */
    @Test
    public void testFirePropertyChangeOldAndNewValue() {

        propChangeSupport.addPropertyChangeListener(listener);
        propChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        
        assertEquals(1, capture.getValues().size());
        PropertyChangeEvent event = capture.getValues().get(0);

        assertEquals(propertyName, event.getPropertyName());
        assertEquals(newValue, event.getNewValue());
        assertEquals(oldValue, event.getOldValue());

        EasyMock.verify(listener);
        
    }

    /**
     * Stub without any additional to test abstract class.
     *
     * @author Doreen Seider
     */
    private class PropertiesChangeSupportStub extends PropertiesChangeSupport { }

}
