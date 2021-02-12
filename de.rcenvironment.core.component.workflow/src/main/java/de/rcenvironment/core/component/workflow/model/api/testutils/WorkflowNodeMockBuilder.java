/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.model.api.testutils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentRevision;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;

public class WorkflowNodeMockBuilder {

    private final WorkflowNode node = EasyMock.createMock(WorkflowNode.class);
    
    private final Map<String, EndpointDefinition> inputDefinitions = new HashMap<>();

    private final Map<String, EndpointDefinition> outputDefinitions = new HashMap<>();

    private String workflowIdentifier;
    
    public WorkflowNodeMockBuilder addStaticInput(final String name, final EndpointDefinition inputDefinition) {
        this.inputDefinitions.put(name, inputDefinition);
        return this;
    }
    
    public WorkflowNodeMockBuilder addStaticOutput(final String name, final EndpointDefinition outputDefinition) {
        this.outputDefinitions.put(name, outputDefinition);
        return this;
    }

    public WorkflowNodeMockBuilder identifier(String identifier) {
        this.workflowIdentifier = identifier;
        return this;
    }

    public WorkflowNode build() {

        EasyMock.expect(node.getInputDescriptionsManager()).andStubAnswer(() -> {
            final EndpointDescriptionsManager manager = EasyMock.createNiceMock("InputManager", EndpointDescriptionsManager.class);

            EasyMock.expect(manager.getStaticEndpointDefinitions()).andStubAnswer(() -> {
                final Set<EndpointDefinition> returnValue = new HashSet<>();
                for (final EndpointDefinition inputDefinition : this.inputDefinitions.values()) {
                    returnValue.add(inputDefinition);
                }
                return returnValue;

            });
            EasyMock.expect(manager.getDynamicEndpointDefinitions()).andStubReturn(Collections.emptySet());
            EasyMock.expect(manager.getStaticEndpointDescriptions()).andStubAnswer(() -> {
                final Set<EndpointDescription> returnValue = new HashSet<>();
                for (Map.Entry<String, EndpointDefinition> inputDefinition : this.inputDefinitions.entrySet()) {
                    returnValue.add(new EndpointDescription(inputDefinition.getValue(), inputDefinition.getKey()));
                }
                return returnValue;
            });
            EasyMock.expect(manager.getDynamicEndpointDescriptions()).andStubReturn(Collections.emptySet());

            EasyMock.replay(manager);
            return manager;
        });

        EasyMock.expect(node.getOutputDescriptionsManager()).andStubAnswer(() -> {
            final EndpointDescriptionsManager manager = EasyMock.createNiceMock("OutputManager", EndpointDescriptionsManager.class);

            EasyMock.expect(manager.getStaticEndpointDefinitions()).andStubAnswer(() -> {
                final Set<EndpointDefinition> returnValue = new HashSet<>();
                for (final EndpointDefinition outputDefinition : this.outputDefinitions.values()) {
                    returnValue.add(outputDefinition);
                }
                return returnValue;

            });
            EasyMock.expect(manager.getDynamicEndpointDefinitions()).andStubReturn(Collections.emptySet());
            EasyMock.expect(manager.getStaticEndpointDescriptions()).andStubAnswer(() -> {
                final Set<EndpointDescription> returnValue = new HashSet<>();
                for (Map.Entry<String, EndpointDefinition> outputDefinition : this.outputDefinitions.entrySet()) {
                    returnValue.add(new EndpointDescription(outputDefinition.getValue(), outputDefinition.getKey()));
                }
                return returnValue;
            });
            EasyMock.expect(manager.getDynamicEndpointDescriptions()).andStubReturn(Collections.emptySet());
            
            EasyMock.replay(manager);
            return manager;
        });

        EasyMock.expect(node.getIdentifierAsObject()).andStubReturn(new WorkflowNodeIdentifier(this.workflowIdentifier));
        EasyMock.expect(node.getComponentDescription()).andStubAnswer(() -> {
            final ComponentDescription description = EasyMock.createMock(ComponentDescription.class);
            EasyMock.expect(description.getComponentInstallation()).andStubAnswer(() -> {
                final ComponentInstallation installation = EasyMock.createMock(ComponentInstallation.class);
                EasyMock.expect(installation.getComponentRevision()).andStubAnswer(() -> {
                    final ComponentRevision revision = EasyMock.createMock(ComponentRevision.class);
                    EasyMock.expect(revision.getClassName()).andStubReturn("some implementing class name");
                    EasyMock.replay(revision);
                    return revision;
                });
                EasyMock.replay(installation);
                return installation;
            });
            EasyMock.replay(description);
            return description;
        });
        EasyMock.replay(node);
        return node;

    }
}