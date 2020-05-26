/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;

/**
 * Factory for {@link ComponentInstallation} instances.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public final class ComponentInstallationMockFactory {

    private ComponentInstallationMockFactory() {}

    /**
     * Creates a {@link ComponentInstallation} instance. This method variant should be preferred over the one taking a node string id for
     * better abstraction.
     * 
     * @param identifier component identifier
     * @param version component version
     * @param nodeId node id the component is installed on
     * @return {@link ComponentInstallation} instance
     */
    public static ComponentInstallation createComponentInstallationMock(String identifier, String version, LogicalNodeId nodeId) {
        return createComponentInstallationMock(identifier, version, nodeId.getLogicalNodeIdString());
    }

    /**
     * Creates a {@link ComponentInstallation} instance.
     * 
     * @param identifier component identifier
     * @param version component version
     * @param nodeId node id the component is installed on
     * @return {@link ComponentInstallation} instance
     */
    public static ComponentInstallation createComponentInstallationMock(String identifier, String version, String nodeId) {
        ComponentInterface compInterface = EasyMock.createStrictMock(ComponentInterface.class);
        EasyMock.expect(compInterface.getDisplayName()).andStubReturn(identifier);
        EasyMock.expect(compInterface.getIdentifierAndVersion()).andStubReturn(identifier);
        List<String> identifiers = new ArrayList<>();
        identifiers.add(identifier);
        EasyMock.expect(compInterface.getIdentifiers()).andStubReturn(identifiers);
        EasyMock.expect(compInterface.getVersion()).andStubReturn(version);
        EasyMock.expect(compInterface.getLocalExecutionOnly()).andStubReturn(false);
        EasyMock.replay(compInterface);

        ComponentRevision compRevision = EasyMock.createStrictMock(ComponentRevision.class);
        EasyMock.expect(compRevision.getComponentInterface()).andStubReturn(compInterface);
        EasyMock.replay(compRevision);

        ComponentInstallation compInstallation = EasyMock.createStrictMock(ComponentInstallation.class);
        EasyMock.expect(compInstallation.getComponentRevision()).andStubReturn(compRevision);
        EasyMock.expect(compInstallation.getComponentInterface()).andStubReturn(compInterface);
        EasyMock.expect(compInstallation.getNodeIdObject())
            .andStubReturn(NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(nodeId));
        EasyMock.expect(compInstallation.getNodeId()).andStubReturn(nodeId);
        EasyMock.replay(compInstallation);

        return compInstallation;
    }

}
