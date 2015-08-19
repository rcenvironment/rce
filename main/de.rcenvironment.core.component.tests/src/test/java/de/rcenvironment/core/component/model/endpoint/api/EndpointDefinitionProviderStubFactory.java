/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Creates stub implementations of {@link EndpointDefinitionsProvider}.
 * 
 * @author Doreen Seider
 */
public final class EndpointDefinitionProviderStubFactory {
    
    /** Constant. */
    public static final String STATICOUTPUTNAME = "my Output";

    /** Constant. */
    public static final String DYNAMICOUTPUTID = "outputId";

    /** Constant. */
    public static final String STATICINPUTNAME = "my Input";

    /** Constant. */
    public static final String DYNAMICINPUTID1 = "inputId";

    /** Constant. */
    public static final String DYNAMICINPUTID2 = "inputId2";
    
    private EndpointDefinitionProviderStubFactory() {}
    
    /**
     * Creates {@link EndpointDefinitionsProviderImpl} object from test files.
     * @return new {@link EndpointDefinitionsProviderImpl} object
     * @throws IOException on error
     */
    public static EndpointDefinitionsProviderImpl createInputDefinitionsProviderFromTestFile() throws IOException {
        List<InputStream> staticInputStreams = new ArrayList<>();
        staticInputStreams.add(EndpointDefinitionProviderStubFactory.class.getResourceAsStream("/inputs_meta_data_ext.json"));
        Set<EndpointDefinitionImpl> outputDescriptions = ComponentUtils
            .extractStaticEndpointDefinition(EndpointDefinitionProviderStubFactory.class.getResourceAsStream("/inputs.json"),
                staticInputStreams, EndpointType.INPUT);
        List<InputStream> dynamicInputStreams = new ArrayList<>();
        dynamicInputStreams.add(EndpointDefinitionProviderStubFactory.class.getResourceAsStream("/inputs_meta_data_ext.json"));
        outputDescriptions.addAll(ComponentUtils
            .extractDynamicEndpointDefinition(EndpointDefinitionProviderStubFactory.class.getResourceAsStream("/inputs.json"),
                dynamicInputStreams, EndpointType.INPUT));

        EndpointDefinitionsProviderImpl inputProvider = new EndpointDefinitionsProviderImpl();
        inputProvider.setEndpointDefinitions(outputDescriptions);
        return inputProvider;
    }
    
}
