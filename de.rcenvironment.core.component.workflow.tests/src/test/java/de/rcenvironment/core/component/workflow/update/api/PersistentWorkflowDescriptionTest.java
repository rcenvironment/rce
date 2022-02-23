/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.update.api;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;

import de.rcenvironment.core.component.update.api.PersistentComponentDescription;

/**
 * Test cases for {@link PersistentWorkflowDescription}.
 * @author Doreen Seider
 */
public class PersistentWorkflowDescriptionTest {

    /**
     * Test.
     * @throws IOException on error
     * @throws JsonParseException on error
     */
    @Test
    public void test() throws JsonParseException, IOException {
        
        String persistentDescription = "{"
            + "\"identifier\" : \"697261b6-eaf5-44ab-af40-6c161a4f26f8\","
            + "\"workflowVersion\" : \"1\","
            + "\"connections\" : [ {"
            + "\"source\" : \"fb19289d-7b59-4f5f-8bcd-049ba605f1c1\","
            + "\"output\" : \"DesignVariable\","
            + "\"target\" : \"86881b19-105c-4e48-85a5-8ee0ee7197be\","
            + "\"input\" : \"inc\""
            + "} ]"
            + "}";
        
        List<PersistentComponentDescription> descriptions = new ArrayList<PersistentComponentDescription>();
        descriptions.add(EasyMock.createNiceMock(PersistentComponentDescription.class));
        descriptions.add(EasyMock.createNiceMock(PersistentComponentDescription.class));

        PersistentWorkflowDescription persistentWorkflowDescription
            = new PersistentWorkflowDescription(descriptions, persistentDescription);
        
        assertEquals(persistentDescription, persistentWorkflowDescription.getWorkflowDescriptionAsString());
        assertEquals(descriptions, persistentWorkflowDescription.getComponentDescriptions());
        assertEquals("1", persistentWorkflowDescription.getWorkflowVersion());

    }
}
