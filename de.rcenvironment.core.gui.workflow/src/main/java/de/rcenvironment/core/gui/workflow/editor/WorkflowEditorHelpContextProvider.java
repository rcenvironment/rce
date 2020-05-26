/*
 * Copyright 2006-2020 DLR, Germany
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
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        toolIntegrationRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
    }

    @Override
    public IContext getContext(Object arg0) {
        Object object = (((IStructuredSelection) viewer.getSelection()).getFirstElement());
        if (object instanceof WorkflowNodePart) {
            WorkflowNodePart nodePart = (WorkflowNodePart) object;
            String componentIdentifier = ((WorkflowNode) nodePart.getModel()).getComponentDescription().getIdentifier();
            // remove version suffix otherwise the context help system does not work properly for some reason (even if the contexts ids in
            // contexts.xml are adapted and version suffix is added)
            // as a consequence, context help can only be provided per component and not per version
            // - seid_do, Dec 2013
            if (toolIntegrationRegistry.hasTIContextMatchingPrefix(componentIdentifier)) {
                return HelpSystem.getContext(ToolIntegrationConstants.CONTEXTUAL_HELP_PLACEHOLDER_ID);
            } else if (componentIdentifier.contains("de.rcenvironment.remoteaccess")) {
                return HelpSystem.getContext("de.rcenvironment.remoteaccess.*");
            } else {
                return HelpSystem.getContext(componentIdentifier.substring(0,
                    componentIdentifier.lastIndexOf(ComponentConstants.ID_SEPARATOR)));
            }
        } else if (object instanceof ConnectionPart) {
            return HelpSystem.getContext("de.rcenvironment.connectionEditorContext");
        } else if (object instanceof WorkflowLabelPart) {
            return HelpSystem.getContext("de.rcenvironment.workflowLabelContext");
        } else if (object instanceof WorkflowPart) {
            return HelpSystem.getContext("de.rcenvironment.workflowEditorContext");
        } else if (object instanceof WorkflowExecutionInformationPart) {
            return HelpSystem.getContext("de.rcenvironment.runtimeWorkflowEditorContext");
        }
        return HelpSystem.getContext("de.rcenvironment.rce.gui.workflow.editor"); //$NON-NLS-1$
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
