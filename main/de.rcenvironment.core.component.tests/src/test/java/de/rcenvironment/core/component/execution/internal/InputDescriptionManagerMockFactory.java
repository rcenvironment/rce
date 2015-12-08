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
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDescription;

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
            EasyMock.expect(endpointDescriptionMock.getParentGroupName()).andReturn(info.parentGroup).anyTimes();
            EasyMock.replay(endpointDescriptionMock);
            endpointDescriptions.add(endpointDescriptionMock);
        }
        EasyMock.expect(endpointDescriptionsManagerMock.getEndpointDescriptions()).andReturn(endpointDescriptions).anyTimes();

        Set<EndpointGroupDescription> endpointGroupDescriptions = new HashSet<>();
        for (InputGroupMockInformation info : inputGroupMockInformations) {
            EndpointGroupDefinition endpointGroupDefinitionMock = EasyMock.createMock(EndpointGroupDefinition.class);
            EasyMock.expect(endpointGroupDefinitionMock.getLogicOperation()).andReturn(info.type).anyTimes();
            EasyMock.replay(endpointGroupDefinitionMock);
            EndpointGroupDescription endpointGroupDescriptionMock = EasyMock.createMock(EndpointGroupDescription.class);
            EasyMock.expect(endpointGroupDescriptionMock.getEndpointGroupDefinition()).andReturn(endpointGroupDefinitionMock).anyTimes();
            EasyMock.expect(endpointGroupDescriptionMock.getName()).andReturn(info.name).anyTimes();
            EasyMock.expect(endpointGroupDescriptionMock.getParentGroupName()).andReturn(info.parentGroup).anyTimes();
            EasyMock.replay(endpointGroupDescriptionMock);
            endpointGroupDescriptions.add(endpointGroupDescriptionMock);
            EasyMock.expect(endpointDescriptionsManagerMock.getEndpointGroupDescription(info.name)).andReturn(endpointGroupDescriptionMock)
                .anyTimes();
        }
        EasyMock.expect(endpointDescriptionsManagerMock.getEndpointGroupDescriptions()).andReturn(endpointGroupDescriptions).anyTimes();

        EasyMock.replay(endpointDescriptionsManagerMock);
        return endpointDescriptionsManagerMock;
    }

}
