/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;

import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;

/**
 * Factory for {@link ComponentInstallation} instances.
 * 
 * @author Doreen Seider
 */
public final class ComponentInstallationMockFactory {

    private ComponentInstallationMockFactory() {}
    
    /**
     * Creates {@link ComponentInstallation} instance.
     * 
     * @param identifier component identifier
     * @param version component version
     * @param nodeId node id the component is installed on
     * @return {@link ComponentInstallation} instance
     */
    public static ComponentInstallation createComponentInstallationMock(String identifier, String version, String nodeId) {
        ComponentInterface compInterface = EasyMock.createStrictMock(ComponentInterface.class);
        EasyMock.expect(compInterface.getIdentifier()).andStubReturn(identifier);
        List<String> identifiers = new ArrayList<>();
        identifiers.add(identifier);
        EasyMock.expect(compInterface.getIdentifiers()).andStubReturn(identifiers);
        EasyMock.expect(compInterface.getVersion()).andStubReturn(version);
        EasyMock.replay(compInterface);
        
        ComponentRevision compRevision = EasyMock.createStrictMock(ComponentRevision.class);
        EasyMock.expect(compRevision.getComponentInterface()).andStubReturn(compInterface);
        EasyMock.replay(compRevision);
        
        ComponentInstallation compInstallation = EasyMock.createStrictMock(ComponentInstallation.class);
        EasyMock.expect(compInstallation.getComponentRevision()).andStubReturn(compRevision);
        EasyMock.expect(compInstallation.getNodeId()).andStubReturn(nodeId);
        EasyMock.replay(compInstallation);
        
        return compInstallation;
    }
    
}
