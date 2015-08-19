/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.update.api;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

/**
 * Test cases for {@link PersistentComponentDescription}.
 * @author Doreen Seider
 */
public class PersistentComponentDescriptionTest {

    /**
     * Test.
     * @throws IOException on error
     * */
    @Test
    public void test() throws IOException {
        
        String persistentComponentDescription = "{"
            + "\"identifier\" : \"86881b19-105c-4e48-85a5-8ee0ee7197be\","
            + "\"name\" : \"Add\","
            + "\"location\" : \"335:129\","
            + "\"component\" : {"
            + "\"identifier\" : \"de.rcenvironment.rce.components.python.PythonComponent_Python\""
            + "}"
            + "}\"";
        
        PersistentComponentDescription description = new PersistentComponentDescription(persistentComponentDescription);
        
        assertEquals("de.rcenvironment.rce.components.python.PythonComponent_Python", description.getComponentIdentifier());
        assertEquals("", description.getComponentVersion());
        assertEquals(null, description.getComponentNodeIdentifier());
        assertEquals(persistentComponentDescription, description.getComponentDescriptionAsString());
        
        persistentComponentDescription = "{"
            + "\"identifier\" : \"86881b19-105c-4e48-85a5-8ee0ee7197be\","
            + "\"name\" : \"Add\","
            + "\"location\" : \"335:129\","
            + "\"component\" : {"
            + "\"identifier\" : \"de.rcenvironment.rce.components.python.PythonComponent_Python\","
            + "\"version\" : \"2.0\""
            + "},"
            + "\"platform\" : \"node-id\""
            + "}\"";
        
        description = new PersistentComponentDescription(persistentComponentDescription);
        
        assertEquals("de.rcenvironment.rce.components.python.PythonComponent_Python", description.getComponentIdentifier());
        assertEquals("2.0", description.getComponentVersion());
        assertEquals("node-id", description.getComponentNodeIdentifier().getIdString());
        assertEquals(persistentComponentDescription, description.getComponentDescriptionAsString());
    }
}
