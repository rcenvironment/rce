/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Class that provides context ID of components.
 *
 * @author Doreen Seider
 */
public class WorkflowEditorHelpContextProvider implements IContextProvider {

    private GraphicalViewer viewer;

    private ToolIntegrationContextRegistry toolIntegrationRegistry;

    public WorkflowEditorHelpContextProvider(GraphicalViewer viewer) {
        this.viewer = viewer;
        ServiceRegistryAccess serviceRegistryAccess = createServiceRegistryAccess();
        toolIntegrationRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
    }

    protected ServiceRegistryAccess createServiceRegistryAccess() {
        return ServiceRegistry.createAccessFor(this);
    }

    @Override
    public IContext getContext(Object arg0) {
        Object object = getSelectedElement();
        if (object instanceof WorkflowNodePart) {
            WorkflowNodePart nodePart = (WorkflowNodePart) object;
            String componentIdentifier = getComponentIdentifier(nodePart);
            // remove version suffix otherwise the context help system does not work properly for some reason (even if the contexts ids in
            // contexts.xml are adapted and version suffix is added)
            // as a consequence, context help can only be provided per component and not per version
            // - seid_do, Dec 2013
            if (componentIdentifier.startsWith("de.rcenvironment.integration.workflow")) {
                // We give components that integrate some workflow a special treatment here, since they have a different F1-help than other
                // user-integrated components. This will be changed in future versions and components backed by tools and by workflows will
                // be indistinguishable by the user
                return getContextFromHelpSystem("de.rcenvironment.workflow");
            } else if (toolIntegrationRegistry.hasTIContextMatchingPrefix(componentIdentifier)) {
                return getContextFromHelpSystem(ToolIntegrationConstants.CONTEXTUAL_HELP_PLACEHOLDER_ID);
            } else if (componentIdentifier.contains("de.rcenvironment.remoteaccess")) {
                return getContextFromHelpSystem("de.rcenvironment.remoteaccess.*");
            } else {
                return getContextFromHelpSystem(componentIdentifier.substring(0,
                    componentIdentifier.lastIndexOf(ComponentConstants.ID_SEPARATOR)));
            }
        } else if (object instanceof ConnectionPart) {
            return getContextFromHelpSystem("de.rcenvironment.connectionEditorContext");
        } else if (object instanceof WorkflowLabelPart) {
            return getContextFromHelpSystem("de.rcenvironment.workflowLabelContext");
        } else if (object instanceof WorkflowPart) {
            return getContextFromHelpSystem("de.rcenvironment.workflowEditorContext");
        } else if (object instanceof WorkflowExecutionInformationPart) {
            return getContextFromHelpSystem("de.rcenvironment.runtimeWorkflowEditorContext");
        }
        return getContextFromHelpSystem("de.rcenvironment.rce.gui.workflow.editor"); //$NON-NLS-1$
    }

    protected String getComponentIdentifier(WorkflowNodePart nodePart) {
        return ((WorkflowNode) nodePart.getModel()).getComponentDescription().getIdentifier();
    }

    protected Object getSelectedElement() {
        return ((IStructuredSelection) viewer.getSelection()).getFirstElement();
    }

    protected IContext getContextFromHelpSystem(final String contextId) {
        return HelpSystem.getContext(contextId);
    }

    @Override
    public int getContextChangeMask() {
        return IContextProvider.SELECTION;
    }

    @Override
    public String getSearchExpression(Object arg0) {
        return null;
    }

}
