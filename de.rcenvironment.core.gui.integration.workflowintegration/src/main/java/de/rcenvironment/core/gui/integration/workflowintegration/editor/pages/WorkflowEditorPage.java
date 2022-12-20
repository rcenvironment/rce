/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessageStore;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.integration.common.editor.IIntegrationEditorPage;
import de.rcenvironment.core.gui.integration.common.editor.IntegrationEditorButtonBar;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditor;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditorInput;
import de.rcenvironment.core.gui.integration.workflowintegration.handlers.WIOpenConnectionEditorHandler;
import de.rcenvironment.core.gui.integration.workflowintegration.handlers.WIWorkflowNodeDisEnableHandler;
import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorContextMenuProvider;
import de.rcenvironment.core.gui.workflow.editor.WorkflowScalableFreeformRootEditPart;
import de.rcenvironment.core.gui.workflow.editor.WorkflowZoomManager;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeDisEnableCommand;
import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.ReadOnlyWorkflowEditorEditPartFactory;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;
import de.rcenvironment.core.gui.workflow.view.outline.OutlineView;

/**
 * {@link WorkflowEditor} extension that allows minimum editing during the workflow integration process. Only enabling and disabling of
 * components and some commands regarding connections are allowed.
 *
 * @author Jan Flink
 */
public class WorkflowEditorPage extends WorkflowEditor implements IIntegrationEditorPage {

    private static final Image VALIDATION_ERROR_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.FAILED);

    private static final String COMPONENT_PLACEHOLDER_MESSAGE = "This workflow component is configured "
        + "to require user input when starting the workflow.\n   "
        + "Since exposing those inputs is currently not supported for integrated workflows,\n   "
        + "this component needs to be disabled.";

    private static final String ONLY_DISABLED_MESSAGE = "The integrated workflow consists only of disabled components.\n"
        + "Please enable at least one component to ensure that the workflow is executable.";

    private static final String PLACEHOLDER_MESSAGE = "At least one component is configured "
        + "to require user input when starting the workflow.\n"
        + "Since exposing those inputs is currently not supported for integrated workflows, "
        + "all of these components need to be disabled.";

    private static final String WORKFLOWINTEGRATION_EDITOR_SCOPE = "de.rcenvironment.rce.gui.integration.workflowintegration.editor.scope";

    private static final String DEFAULT_MESSAGE =
        "Delete individual connections via the connection editor or disable components in order to map\n"
            + "open inputs/outputs of the integrated workflow to inputs/outputs of the component.";

    private static final String TITLE = "Integrated Workflow";

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.integration.workflowintegration.integration_workflowEditor";

    private WorkflowIntegrationEditor integrationEditor;

    private IntegrationEditorButtonBar buttonBar;

    private Composite editorArea;

    private boolean pageValid = false;

    private CLabel messageComposite;

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    public WorkflowEditorPage(WorkflowIntegrationEditor integrationEditor) {
        super();
        this.integrationEditor = integrationEditor;
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return false;
    }

    @Override
    protected void setInput(IEditorInput input) {
        if (input instanceof WorkflowIntegrationEditorInput) {
            workflowDescription = input.getAdapter(WorkflowDescription.class);
            setPartName(workflowDescription.getName());
            Display.getDefault().asyncExec(() -> {
                if (getViewer().getControl() != null) {
                    getViewer().setContents(workflowDescription);
                    validateComponents();
                }
            });
        } else {
            super.setInput(input);
            integrationEditor.getController().setWorkflowDescription(getWorkflowDescription());
        }
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // Intentionally left empty.
    }

    private void validateComponents() {
        ComponentValidationMessageStore componentValidationMessageStore = ComponentValidationMessageStore.getInstance();
        componentValidationMessageStore.emptyMessageStore(); // delete any workflow validation errors
        getWorkflowNodeParts().stream().forEach(workflowNodePart -> {
            final WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
            workflowNode.getConfigurationDescription().getConfiguration().keySet().stream()
                .filter(key -> WorkflowPlaceholderHandler.isActivePlaceholder(
                    workflowNode.getConfigurationDescription().getConfiguration().get(key), workflowNode.getConfigurationDescription()))
                .forEach(key -> {
                    ArrayList<ComponentValidationMessage> messages = new ArrayList<>();
                    messages.add(
                        new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, key, "Placeholders not supported.",
                            COMPONENT_PLACEHOLDER_MESSAGE));
                    componentValidationMessageStore.addValidationMessagesByComponentId(workflowNode.getIdentifierAsObject().toString(),
                        messages);
                });
            workflowNodePart.updateValid();
        });
        updatePageValid();
    }

    public boolean isPageValid() {
        return pageValid;
    }

    private void updatePageValid() {
        Optional<String> message = Optional.empty();
        boolean noPlaceholders = getWorkflowNodeParts().stream()
            .map(WorkflowNodePart::getModel)
            .filter(WorkflowNode.class::isInstance)
            .map(WorkflowNode.class::cast)
            .noneMatch(node -> !ComponentValidationMessageStore.getInstance()
                .getMessagesByComponentId(node.getIdentifierAsObject().toString())
                .isEmpty() && node.isEnabled());
        boolean anyComponentEnabled = getWorkflowNodeParts().stream()
            .map(WorkflowNodePart::getModel)
            .filter(WorkflowNode.class::isInstance)
            .map(WorkflowNode.class::cast)
            .anyMatch(WorkflowNode::isEnabled);
        if (!anyComponentEnabled) {
            message = Optional.of(ONLY_DISABLED_MESSAGE);
        } else if (!noPlaceholders) {
            message = Optional.of(PLACEHOLDER_MESSAGE);
        }
        setMessage(message);
        pageValid = noPlaceholders && anyComponentEnabled;
        integrationEditor.updateValid();
    }

    private List<WorkflowNodePart> getWorkflowNodeParts() {
        List<WorkflowNodePart> parts = new ArrayList<>();
        for (Object o : getViewer().getRootEditPart().getChildren()) {
            if (!(o instanceof WorkflowPart)) {
                continue;
            }
            WorkflowPart wfPart = (WorkflowPart) o;
            for (Object part : wfPart.getChildren()) {
                if (part instanceof WorkflowNodePart) {
                    parts.add((WorkflowNodePart) part);
                }
            }
        }
        return parts;
    }

    @Override
    public void createPartControl(Composite parent) {

        Composite container = new Composite(parent, SWT.FILL);
        container.setLayout(new GridLayout(1, true));
        final CLabel pageTitle = new CLabel(container, SWT.NONE);
        pageTitle.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        pageTitle.setText(TITLE);
        pageTitle.setMargins(4, 3, 4, 3);
        pageTitle.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_LIGHT_GREY));
        messageComposite = new CLabel(container, SWT.NONE);
        messageComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        messageComposite.setText(DEFAULT_MESSAGE);
        messageComposite.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_WHITE));
        editorArea = new Composite(container, SWT.BORDER);
        editorArea.setLayout(new GridLayout());
        editorArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        super.createPartControl(editorArea);
        editorArea.setFocus();
        getViewer().getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        buttonBar = new IntegrationEditorButtonBar(container, integrationEditor);
    }

    private void setMessage(Optional<String> message) {
        if (!message.isPresent()) {
            this.messageComposite.setText(DEFAULT_MESSAGE);
            this.messageComposite.setImage(null);
            integrationEditor.setWorkflowEditorPageImage(null);
        } else {
            this.messageComposite.setText(message.get());
            this.messageComposite.setImage(VALIDATION_ERROR_IMAGE);
            integrationEditor.setWorkflowEditorPageImage(VALIDATION_ERROR_IMAGE);
        }
    }


    @Override
    public void setFocus() {
        editorArea.setFocus();
    }

    public void setSaveButtonEnabled(boolean enable) {
        buttonBar.setSaveButtonEnabled(enable);
    }

    @Override
    public void mouseDoubleClick(MouseEvent ev) {

        // Open Connection Editor filtered to the selected connection
        ConnectionPart connectionPart = selectConnection(ev);
        if (connectionPart != null) {
            WorkflowNode source = null;
            WorkflowNode target = null;
            if (connectionPart.getSource().getModel() instanceof WorkflowNode) {
                source = (WorkflowNode) connectionPart.getSource().getModel();
            }
            if (connectionPart.getTarget().getModel() instanceof WorkflowNode) {
                target = (WorkflowNode) connectionPart.getTarget().getModel();
            }
            WIOpenConnectionEditorHandler openConnectionEditorHandler = new WIOpenConnectionEditorHandler(source,
                target);
            try {
                openConnectionEditorHandler.execute(new ExecutionEvent());
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            }
        } else {
            switchComponentActivation();
        }
    }

    @Override
    public String getContributorId() {
        return "de.rcenvironment.rce.gui.integration.workflowintegration.WorkflowEditorPage";
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IContentOutlinePage.class) {
            return new OutlineView(getGraphicalViewer());
        }
        if (type == IPropertySheetPage.class) {
            if (tabbedPropertySheetPage == null || tabbedPropertySheetPage.getControl() == null
                || tabbedPropertySheetPage.getControl().isDisposed()) {
                tabbedPropertySheetPage = new TabbedPropertySheetPage(this);
            }
            return tabbedPropertySheetPage;
        }
        
        if (type == IContextProvider.class) {
            return new IContextProvider() {

                @Override
                public String getSearchExpression(Object arg0) {
                    return null;
                }

                @Override
                public int getContextChangeMask() {
                    return 0;
                }

                @Override
                public IContext getContext(Object arg0) {
                    return HelpSystem.getContext(HELP_CONTEXT_ID);
                }
            };
        }
        if (type == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }
        return super.getAdapter(type);
    }

    @Override
    protected void initializeGraphicalViewer() {

        setViewer(getGraphicalViewer());
        WorkflowScalableFreeformRootEditPart rootEditPart = new WorkflowScalableFreeformRootEditPart();
        getViewer().setRootEditPart(rootEditPart);
        getViewer().setEditPartFactory(new ReadOnlyWorkflowEditorEditPartFactory());

        getCommandStack().setUndoLimit(UNDO_LIMIT);

        getCommandStack().addCommandStackEventListener(event -> {
            if (event.getCommand().getClass().equals(WorkflowNodeDisEnableCommand.class)) {
                validateComponents();
            }
        });

        getViewer().setContents(workflowDescription);

        ContextMenuProvider cmProvider = new WorkflowEditorContextMenuProvider(getViewer(), getActionRegistry());
        getViewer().setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, getViewer());
        WorkflowZoomManager zoomManager = (WorkflowZoomManager) rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(zoomManager));
        getActionRegistry().registerAction(new ZoomOutAction(zoomManager));
        getViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);

        getViewer().addSelectionChangedListener(selectionChangedEvent -> {
            StructuredSelection structuredSelection = ((StructuredSelection) selectionChangedEvent.getSelection());
            for (Object structuredSelectionObject : structuredSelection.toList()) {
                if (structuredSelectionObject instanceof ConnectionPart) {
                    ConnectionPart connectionPart = ((ConnectionPart) structuredSelectionObject);
                    if (getViewer().getSelectedEditParts().contains(connectionPart)) {
                        connectionPart.getConnectionFigure().setForegroundColor(ColorConstants.blue);
                        connectionPart.showLabel();
                    }
                }
            }
            removeConnectionColorsAndLabel();
        });

        int[] eventTypes = { SWT.MouseDoubleClick, SWT.DragDetect };
        removeListeners(getViewer().getControl(), eventTypes);
        getViewer().getControl().addMouseListener(this);
        getGraphicalViewer().getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
        IContextService contextService = getSite().getService(IContextService.class);
        contextService.activateContext(WORKFLOWINTEGRATION_EDITOR_SCOPE);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(getResourceListener());
    }

    public static void removeListeners(Widget ctrl, int[] eventTypes) {

        for (int eventType : eventTypes) {
            Listener[] listeners = ctrl.getListeners(eventType);
            for (Listener listener : listeners) {
                ctrl.removeListener(eventType, listener);
            }
        }
    }
    
    private void switchComponentActivation() {
        List<?> selectedParts = getViewer().getSelectedEditParts();
        if (selectedParts.isEmpty() || selectedParts.size() > 1) {
            return;
        }
        if (selectedParts.iterator().hasNext() && selectedParts.iterator().next() instanceof WorkflowNodePart) {
            try {
                new WIWorkflowNodeDisEnableHandler().execute(new ExecutionEvent());
            } catch (ExecutionException e) {
                // Nothing to do here.
            }
        }
    }

    @Override
    public void setBackButtonEnabled(boolean enable) {
        buttonBar.setBackButtonEnabled(enable);
    }

    @Override
    public void setNextButtonEnabled(boolean enable) {
        buttonBar.setNextButtonEnabled(enable);
    }

    @Override
    public boolean hasChanges() {
        return isDirty();
    }

}
