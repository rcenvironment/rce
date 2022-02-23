/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view;

import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowRunNodePart;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * {@link ContextMenuProvider} for the {@link WorkflowRunEditor}.
 * 
 * @author Doreen Seider
 */
public class WorkflowRunEditorContextMenuProvider extends ContextMenuProvider {

    private final GraphicalViewer viewer;

    public WorkflowRunEditorContextMenuProvider(GraphicalViewer viewer) {
        super(viewer);
        this.viewer = viewer;
    }

    @Override
    public void buildContextMenu(IMenuManager menu) {

        @SuppressWarnings("rawtypes") List selection = viewer.getSelectedEditParts();
        if (selection.size() == 0) {
            // fixes IndexOutOfBoundsException occurring if no element is currently selected
            return;
        }
        WorkflowRunNodePart part = null;
        if (selection.get(0) instanceof WorkflowRunNodePart) {
            part = (WorkflowRunNodePart) selection.get(0);
        }
        if (part != null) {
            WorkflowExecutionInformation wfExeInfo = (WorkflowExecutionInformation) ((WorkflowExecutionInformationPart)
                ((WorkflowPart) part.getParent()).getParent()).getModel();
            final WorkflowNode wfNode = (WorkflowNode) part.getModel();
            final ComponentExecutionInformation compExeInfo = wfExeInfo.getComponentExecutionInformation(wfNode.getIdentifierAsObject());
            
            // Find registered views
            IExtensionRegistry extReg = Platform.getExtensionRegistry();
            IConfigurationElement[] confElements =
                extReg.getConfigurationElementsFor("de.rcenvironment.core.gui.workflow.monitoring"); //$NON-NLS-1$
            IConfigurationElement[] viewConfElements =
                extReg.getConfigurationElementsFor("org.eclipse.ui.views"); //$NON-NLS-1$

            for (final IConfigurationElement confElement : confElements) {

                if (compExeInfo.getComponentIdentifier().startsWith(confElement.getAttribute("component"))) { //$NON-NLS-1$

                    for (final IConfigurationElement viewConfElement : viewConfElements) {

                        if (viewConfElement.getAttribute("id").equals(confElement.getAttribute("view"))) {

                            menu.add(new Action() {

                                @Override
                                public String getText() {
                                    return viewConfElement.getAttribute("name");
                                }

                                @Override
                                public void run() {
                                    final IViewPart view;
                                    try {
                                        view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().
                                            showView(viewConfElement.getAttribute("class"),
                                                compExeInfo.getExecutionIdentifier(), IWorkbenchPage.VIEW_VISIBLE); //$NON-NLS-1$

                                        ConcurrencyUtils.getAsyncTaskService().execute("Initialize component runtime view data", () -> {

                                            ((ComponentRuntimeView) view).initializeData(compExeInfo);
                                            Display.getDefault().asyncExec(new Runnable() {

                                                @Override
                                                public void run() {
                                                    ((ComponentRuntimeView) view).initializeView();
                                                }
                                            });

                                        });
                                        view.setFocus();
                                    } catch (PartInitException e) {
                                        throw new RuntimeException(e);
                                    } catch (InvalidRegistryObjectException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public boolean isEnabled() {
                                    @SuppressWarnings("rawtypes") List selection = viewer.getSelectedEditParts();
                                    return selection.size() == 1 && selection.get(0).getClass() == WorkflowRunNodePart.class;
                                }
                            });
                            break;
                        }
                    }
                }
            }
        }
    }

}
