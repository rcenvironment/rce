/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.help.IContext;

import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePartMock;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

class WorkflowEditorHelpContextProviderTestHarness {

    class WorkflowEditorHelpContextProviderMock extends WorkflowEditorHelpContextProvider {

        private Object selectedElement;

        private IContext contextFromHelpSystem;

        private String componentIdentifier;

        private final Capture<String> requestedContexts = Capture.newInstance(CaptureType.ALL);

        WorkflowEditorHelpContextProviderMock(GraphicalViewer viewer) {
            super(viewer);
        }

        @Override
        protected ServiceRegistryAccess createServiceRegistryAccess() {
            registry = EasyMock.createMock(ToolIntegrationContextRegistry.class);

            final ServiceRegistryAccess mockedAccess = EasyMock.createMock(ServiceRegistryAccess.class);
            EasyMock.expect(mockedAccess.getService(ToolIntegrationContextRegistry.class)).andReturn(registry);
            EasyMock.replay(mockedAccess);

            return mockedAccess;
        }

        protected void setSelectedElement(Object element) {
            this.selectedElement = element;
        }

        @Override
        protected Object getSelectedElement() {
            return this.selectedElement;
        }

        protected void setContextFromHelpSystem(IContext context) {
            this.contextFromHelpSystem = context;
        }

        @Override
        protected IContext getContextFromHelpSystem(String contextId) {
            this.getRequestedContexts().setValue(contextId);
            return this.contextFromHelpSystem;
        }

        Capture<String> getRequestedContexts() {
            return requestedContexts;
        }

        void setComponentIdentifier(String componentIdentifier) {
            this.componentIdentifier = componentIdentifier;
        }

        @Override
        protected String getComponentIdentifier(WorkflowNodePart nodePart) {
            return this.componentIdentifier;
        }
    }

    private WorkflowEditorHelpContextProviderMock provider;

    private ToolIntegrationContextRegistry registry;

    private WorkflowNodePart selectedWorkflowNodePart;

    private IContext expectedContext;

    private IContext returnedContext;

    public WorkflowEditorHelpContextProviderMock getProvider() {
        return this.provider;
    }

    public GivenSteps given() {
        return new GivenSteps();
    }

    public class GivenSteps {

        GivenSteps providerIsConstructedWithNullViewer() {
            provider = new WorkflowEditorHelpContextProviderMock(null);
            return this;
        }

        GivenSteps providerIsConstructedWithArbitraryViewer() {
            // We use a mock here instead of constructing an actual GraphicalViewer as the constructor of the only implementation, i.e., of
            // GraphicalViewerImpl, has a lot of side effects and construction of the object has quite a heavy overhead
            final GraphicalViewer viewer = EasyMock.createMock(GraphicalViewer.class);
            EasyMock.replay(viewer);

            provider = new WorkflowEditorHelpContextProviderMock(viewer);
            return this;
        }

        GivenSteps selectedElementIs(Object selectedElement) {
            provider.setSelectedElement(selectedElement);
            return this;
        }

        GivenSteps selectedElementIsWorkflowNodePartWithComponentIdentifier(String componentIdentifier) {
            // We use a mock here instead of constructing an actual WorkflowNodePart as the constructor of that class has a hidden
            // dependency on ToolIntegrationContextRegistry
            selectedWorkflowNodePart = new WorkflowNodePartMock();

            provider.setSelectedElement(selectedWorkflowNodePart);
            provider.setComponentIdentifier(componentIdentifier);
            return this;
        }

        GivenSteps registryHasTIContextMatchingPrefix(String componentIdentifier) {
            EasyMock.expect(registry.hasTIContextMatchingPrefix(componentIdentifier)).andReturn(true);
            return this;
        }

        GivenSteps registryHasNoTIContextMatchingPrefix(String componentIdentifier) {
            EasyMock.expect(registry.hasTIContextMatchingPrefix(componentIdentifier)).andReturn(false);
            return this;
        }
    }

    WhenSteps when() {
        EasyMock.replay(registry);
        return new WhenSteps();
    }

    class WhenSteps {

        WhenSteps gettingContext() {
            expectedContext = EasyMock.createMock(IContext.class);
            EasyMock.replay(expectedContext);
            provider.setContextFromHelpSystem(expectedContext);

            returnedContext = provider.getContext(null);
            return this;
        }
    }

    ThenSteps then() {
        return new ThenSteps();
    }

    class ThenSteps {

        public ThenSteps queriedContextIdWas(String contextId) {
            assertEquals(expectedContext, returnedContext);
            assertTrue(provider.getRequestedContexts().hasCaptured());
            assertEquals(1, provider.getRequestedContexts().getValues().size());
            assertEquals(contextId, provider.getRequestedContexts().getValue());
            return this;
        }

    }

}
