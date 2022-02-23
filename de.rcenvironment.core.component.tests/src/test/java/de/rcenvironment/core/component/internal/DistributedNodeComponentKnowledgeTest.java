/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.api.DistributedNodeComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;

/**
 * Asserts correct behavior of {@link DistributedNodeComponentKnowledge}.
 * 
 * @author Alexander Weinert
 */
public class DistributedNodeComponentKnowledgeTest {
    
    private final String componentIdA = "componentA";
    private final String componentIdB = "componentB";
    private final String componentIdC = "componentC";
    
    private final DistributedComponentEntry componentA = EasyMock.createMock(DistributedComponentEntry.class);
    private final DistributedComponentEntry componentB = EasyMock.createMock(DistributedComponentEntry.class);
    private final DistributedComponentEntry componentC = EasyMock.createMock(DistributedComponentEntry.class);
    
    /**
     * Asserts that the factory method {@link DistributedNodeComponentKnowledgeImpl#createEmpty()} indeed creates an empty
     * DistributedNodeComponentKnowledge.
     */
    @Test
    public void testEmptyKnowledge() {
        final DistributedNodeComponentKnowledge emptyKnowledge = DistributedNodeComponentKnowledgeImpl.createEmpty();
        
        assertTrue(emptyKnowledge.getComponents().isEmpty());
        assertTrue(emptyKnowledge.getAccessibleComponents().isEmpty());
        assertTrue(emptyKnowledge.getInaccessibleComponents().isEmpty());
    }
    
    /**
     * Asserts that {@link DistributedNodeComponentKnowledgeImpl} correctly manages components changing from being accessible to
     * inaccessible as well as new components becoming known.
     */
    @Test
    public void testKnowledge() {
        final Map<String, DistributedComponentEntry> accessibleComponents = new HashMap<>();
        accessibleComponents.put(componentIdA, componentA);

        final Map<String, DistributedComponentEntry> inaccessibleComponents = new HashMap<>();
        inaccessibleComponents.put(componentIdB, componentB);

        DistributedNodeComponentKnowledge knowledge =
            DistributedNodeComponentKnowledgeImpl.fromMap(accessibleComponents, inaccessibleComponents);
        
        assertTrue(knowledge.componentExists(componentIdA));
        assertTrue(knowledge.componentExists(componentIdB));
        assertFalse(knowledge.componentExists(componentIdC));
        
        assertEquals(2, knowledge.getComponents().size());
        assertTrue(knowledge.getComponents().contains(componentA));
        assertTrue(knowledge.getComponents().contains(componentB));
        
        assertEquals(1, knowledge.getAccessibleComponents().size());
        assertTrue(knowledge.getAccessibleComponents().contains(componentA));

        assertEquals(1, knowledge.getInaccessibleComponents().size());
        assertTrue(knowledge.getInaccessibleComponents().contains(componentB));
        
        assertEquals(componentA, knowledge.getComponent(componentIdA));
        assertEquals(componentB, knowledge.getComponent(componentIdB));
        assertNull(knowledge.getComponent(componentIdC));

        assertEquals(componentA, knowledge.getAccessibleComponent(componentIdA));
        assertNull(knowledge.getAccessibleComponent(componentIdB));
        assertNull(knowledge.getAccessibleComponent(componentIdC));

        assertNull(knowledge.getInaccessibleComponent(componentIdA));
        assertEquals(componentB, knowledge.getInaccessibleComponent(componentIdB));
        assertNull(knowledge.getInaccessibleComponent(componentIdC));
        
        knowledge = knowledge
            .putInaccessibleComponent(componentIdA, componentA)
            .putAccessibleComponent(componentIdC, componentC);
        
        assertTrue(knowledge.componentExists(componentIdA));
        assertTrue(knowledge.componentExists(componentIdB));
        assertTrue(knowledge.componentExists(componentIdC));
        
        assertFalse(knowledge.isComponentAccessible(componentIdA));
        assertFalse(knowledge.isComponentAccessible(componentIdB));
        assertTrue(knowledge.isComponentAccessible(componentIdC));
        
        assertTrue(knowledge.isComponentInaccessible(componentIdA));
        assertTrue(knowledge.isComponentInaccessible(componentIdB));
        assertFalse(knowledge.isComponentInaccessible(componentIdC));
        
        assertEquals(3, knowledge.getComponents().size());
        assertTrue(knowledge.getComponents().contains(componentA));
        assertTrue(knowledge.getComponents().contains(componentB));
        assertTrue(knowledge.getComponents().contains(componentC));
        
        assertEquals(componentA, knowledge.getComponent(componentIdA));
        assertEquals(componentB, knowledge.getComponent(componentIdB));
        assertEquals(componentC, knowledge.getComponent(componentIdC));
        
        assertEquals(1, knowledge.getAccessibleComponents().size());
        assertTrue(knowledge.getAccessibleComponents().contains(componentC));

        assertEquals(2, knowledge.getInaccessibleComponents().size());
        assertTrue(knowledge.getInaccessibleComponents().contains(componentA));
        assertTrue(knowledge.getInaccessibleComponents().contains(componentB));

        assertNull(knowledge.getAccessibleComponent(componentIdA));
        assertNull(knowledge.getAccessibleComponent(componentIdB));
        assertEquals(componentC, knowledge.getAccessibleComponent(componentIdC));

        assertEquals(componentA, knowledge.getInaccessibleComponent(componentIdA));
        assertEquals(componentB, knowledge.getInaccessibleComponent(componentIdB));
        assertNull(knowledge.getInaccessibleComponent(componentIdC));
        
        knowledge = knowledge
            .putAccessibleComponent(componentIdA, componentA)
            .putInaccessibleComponent(componentIdC, componentC);
        
        assertTrue(knowledge.componentExists(componentIdA));
        assertTrue(knowledge.componentExists(componentIdB));
        assertTrue(knowledge.componentExists(componentIdC));
        
        assertTrue(knowledge.isComponentAccessible(componentIdA));
        assertFalse(knowledge.isComponentAccessible(componentIdB));
        assertFalse(knowledge.isComponentAccessible(componentIdC));
        
        assertFalse(knowledge.isComponentInaccessible(componentIdA));
        assertTrue(knowledge.isComponentInaccessible(componentIdB));
        assertTrue(knowledge.isComponentInaccessible(componentIdC));
        
        assertEquals(3, knowledge.getComponents().size());
        assertTrue(knowledge.getComponents().contains(componentA));
        assertTrue(knowledge.getComponents().contains(componentB));
        assertTrue(knowledge.getComponents().contains(componentC));
        
        assertEquals(componentA, knowledge.getComponent(componentIdA));
        assertEquals(componentB, knowledge.getComponent(componentIdB));
        assertEquals(componentC, knowledge.getComponent(componentIdC));
        
        assertEquals(1, knowledge.getAccessibleComponents().size());
        assertTrue(knowledge.getAccessibleComponents().contains(componentA));

        assertEquals(2, knowledge.getInaccessibleComponents().size());
        assertTrue(knowledge.getInaccessibleComponents().contains(componentB));
        assertTrue(knowledge.getInaccessibleComponents().contains(componentC));

        assertEquals(componentA, knowledge.getAccessibleComponent(componentIdA));
        assertNull(knowledge.getAccessibleComponent(componentIdB));
        assertNull(knowledge.getAccessibleComponent(componentIdC));

        assertNull(knowledge.getInaccessibleComponent(componentIdA));
        assertEquals(componentB, knowledge.getInaccessibleComponent(componentIdB));
        assertEquals(componentC, knowledge.getInaccessibleComponent(componentIdC));
    }

}
