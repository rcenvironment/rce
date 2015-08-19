/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.HashedMap;
import org.easymock.EasyMock;
import org.omg.IOP.ExceptionDetailMessage;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;

/**
 * Factory for {@link EndpointDescriptionsManager} mocks.
 * 
 * @author Doreen Seider
 */
public final class InputDescriptionManagerMockFactory implements ExceptionDetailMessage {

    private InputDescriptionManagerMockFactory() {}

    /**
     * Creates a mock for {@link EndpointDescriptionsManager}, which mocks methods on the base of the information passed, which are
     * scheduling-related.
     * 
     * @param inputMockInformations expected input declarations
     * @return mock for {@link EndpointDescriptionsManager}
     */
    public static EndpointDescriptionsManager createInputDescriptionManagerMock(List<InputMockInformation> inputMockInformations) {
        return createInputDescriptionManagerMock(inputMockInformations, new ArrayList<InputGroupMockInformation>());
    }
    
    /**
     * Creates a mock for {@link EndpointDescriptionsManager}, which mocks methods on the base of the information passed, which are
     * scheduling-related.
     * 
     * @param inputMockInformations expected input declarations
     * @param inputGroupMockInformations expected input group declarations
     * @return mock for {@link EndpointDescriptionsManager}
     */
    public static EndpointDescriptionsManager createInputDescriptionManagerMock(List<InputMockInformation> inputMockInformations,
        List<InputGroupMockInformation> inputGroupMockInformations) {
        
        EndpointDescriptionsManager endpointDescriptionsManagerMock = EasyMock.createMock(EndpointDescriptionsManager.class);

        Set<EndpointDescription> endpointDescriptions = new HashSet<>();
        for (InputMockInformation info : inputMockInformations) {
            EndpointDescription endpointDescriptionMock = EasyMock.createMock(EndpointDescription.class);
            EasyMock.expect(endpointDescriptionMock.getName()).andReturn(info.name).anyTimes();
            Map<String, String> metaData = new HashedMap<>();
            metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING, info.inputDatumHandling.name());
            metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT, info.inputExecutionContraint.name());
            EasyMock.expect(endpointDescriptionMock.getMetaData()).andReturn(metaData).anyTimes();
            EasyMock.expect(endpointDescriptionMock.isConnected()).andReturn(info.connected).anyTimes();
            EasyMock.expect(endpointDescriptionMock.getGroupName()).andReturn(info.parentGroup).anyTimes();
            EasyMock.replay(endpointDescriptionMock);
            endpointDescriptions.add(endpointDescriptionMock);
        }
        EasyMock.expect(endpointDescriptionsManagerMock.getEndpointDescriptions()).andReturn(endpointDescriptions).anyTimes();
        
        Set<EndpointGroupDefinition> endpointGroupDefinitons = new HashSet<>();
        for (InputGroupMockInformation info : inputGroupMockInformations) {
            EndpointGroupDefinition endpointGroupDefinitionMock = EasyMock.createMock(EndpointGroupDefinition.class);
            EasyMock.expect(endpointGroupDefinitionMock.getIdentifier()).andReturn(info.name).anyTimes();
            EasyMock.expect(endpointGroupDefinitionMock.getType()).andReturn(info.type).anyTimes();
            EasyMock.expect(endpointGroupDefinitionMock.getGroupName()).andReturn(info.parentGroup).anyTimes();
            EasyMock.replay(endpointGroupDefinitionMock);
            endpointGroupDefinitons.add(endpointGroupDefinitionMock);
            EasyMock.expect(endpointDescriptionsManagerMock.getEndpointGroupDefnition(info.name)).andReturn(endpointGroupDefinitionMock)
                .anyTimes();
        }
        EasyMock.expect(endpointDescriptionsManagerMock.getEndpointGroupDefinitions()).andReturn(endpointGroupDefinitons)
            .anyTimes();
        
        EasyMock.replay(endpointDescriptionsManagerMock);
        return endpointDescriptionsManagerMock;
    }

}
