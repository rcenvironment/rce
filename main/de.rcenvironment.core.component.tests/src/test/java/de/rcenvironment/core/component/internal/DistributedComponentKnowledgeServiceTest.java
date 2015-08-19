/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;

/**
 * Test for {@link DistributedComponentKnowledgeService}.
 * 
 * @author Doreen Seider
 */
public class DistributedComponentKnowledgeServiceTest {

    private DistributedComponentKnowledgeServiceImpl knowledgeService = new DistributedComponentKnowledgeServiceImpl();
    
    private Collection<ComponentInstallation> allInstallations = new ArrayList<ComponentInstallation>();
    
    private Collection<ComponentInstallation> publishedInstallations = new ArrayList<ComponentInstallation>();
    
    /**
     * Set up.
     */
    @Before
    public void setUp() {
        
        ComponentInstallationImpl installation1 = new ComponentInstallationImpl();
        installation1.setInstallationId("id-1");
        allInstallations.add(installation1);
        publishedInstallations.add(installation1);
        ComponentInstallationImpl installation2 = new ComponentInstallationImpl();
        installation2.setInstallationId("id-2");
        allInstallations.add(installation2);
        
    }

    /**
     * Tests if there are no null values in the delta of published components if no installation was
     * removed, but one added.
     */
    @Test
    public void testDeltaOfPublishedInstallations() {

        NodePropertiesService nodePropertiesService = EasyMock.createStrictMock(NodePropertiesService.class);
        Capture<Map<String, String>> firstDelta = new Capture<Map<String, String>>();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(firstDelta));
        Capture<Map<String, String>> secondDelta = new Capture<Map<String, String>>();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(secondDelta));
        Capture<Map<String, String>> thrirdDelta = new Capture<Map<String, String>>();
        nodePropertiesService.addOrUpdateLocalNodeProperties(EasyMock.capture(thrirdDelta));
        knowledgeService.bindNodePropertiesService(nodePropertiesService);
        EasyMock.replay(nodePropertiesService);
        
        knowledgeService.setLocalComponentInstallations(allInstallations, publishedInstallations);
        
        Map<String, String> delta = firstDelta.getValue();
        assertTrue(!delta.containsValue(null));
        assertEquals(1, delta.size());

        ComponentInstallationImpl installation3 = new ComponentInstallationImpl();
        installation3.setInstallationId("id-3");
        installation3.setNodeId("node.id-3");
        allInstallations.add(installation3);
        publishedInstallations.add(installation3);
        
        ComponentInstallationImpl installation4 = new ComponentInstallationImpl();
        installation4.setInstallationId("id-4");
        allInstallations.add(installation4);
        publishedInstallations.add(installation4);
        
        knowledgeService.setLocalComponentInstallations(allInstallations, publishedInstallations);
        
        delta = secondDelta.getValue();
        assertTrue(!delta.containsValue(null));
        assertEquals(2, delta.size());
        
        allInstallations.remove(installation3);
        publishedInstallations.remove(installation3);
        
        knowledgeService.setLocalComponentInstallations(allInstallations, publishedInstallations);
        
        delta = thrirdDelta.getValue();
        assertTrue(delta.containsValue(null));
        assertEquals(1, delta.size());
        
        
    }
}
