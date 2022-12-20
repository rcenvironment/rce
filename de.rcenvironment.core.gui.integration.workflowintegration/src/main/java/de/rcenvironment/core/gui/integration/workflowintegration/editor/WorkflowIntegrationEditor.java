/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.gui.integration.common.editor.IntegrationEditor;
import de.rcenvironment.core.gui.integration.common.editor.IntegrationEditorPage;
import de.rcenvironment.core.gui.integration.workflowintegration.WorkflowIntegrationController;
import de.rcenvironment.core.gui.integration.workflowintegration.WorkflowIntegrationController.ConfigurationContext;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.pages.ComponentDescriptionPage;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.pages.MappingPage;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.pages.MappingTreeContentProvider;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.pages.WorkflowEditorPage;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Multi Page Editor for integrating a workflow.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class WorkflowIntegrationEditor extends IntegrationEditor implements IPartListener {

    private static final String DIALOG_TITLE = "Workflow Integration";

    private static final String BUTTON_UPDATE = "Update Integration";

    private static final String BUTTON_INTEGRATE = "Integrate Workflow";

    private static final Log LOG = LogFactory.getLog(WorkflowIntegrationEditor.class);

    private static final String COMPONENT_DESCRIPTION = "Component Description";

    private static final String WORKFLOW_EDITOR = "Integrated Workflow";

    private static final String MAPPING = "Mapping";

    private WorkflowIntegrationController workflowIntegrationController;

    private CTabFolder container;

    private WorkflowEditorPage workflowEditorPage;

    private MappingTreeContentProvider mappingTreeContentProvider;

    private int workflowEditorPageIndex;

    private IAction undoAction;

    private IAction redoAction;

    public WorkflowIntegrationEditor() {
        super();
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(this);
    }

    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        updateContent();
        updateActions();
    }

    private void updateActions() {
        boolean enable =
            getSelectedPage() instanceof WorkflowEditorPage
                && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() == this;
        IToolBarManager toolBarManager = getEditorSite().getActionBars().getToolBarManager();
        toolBarManager.find("org.eclipse.gef.zoom_in").setVisible(enable);
        toolBarManager.find("org.eclipse.gef.zoom_out").setVisible(enable);
        toolBarManager.find("org.eclipse.gef.zoom_widget").setVisible(enable);
        if (undoAction != null && redoAction != null) {
            if (enable) {
                toolBarManager.add(undoAction);
                toolBarManager.add(redoAction);
                getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
                getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
                getEditorSite().getActionBarContributor().setActiveEditor(workflowEditorPage);
            } else {
                toolBarManager.remove(toolBarManager.find(undoAction.getId()));
                toolBarManager.remove(toolBarManager.find(redoAction.getId()));
                getEditorSite().getActionBars().clearGlobalActionHandlers();
            }
        }
        getEditorSite().getActionBars().updateActionBars();
        toolBarManager.update(true);
    }

    @Override
    protected void createPages() {
        container = (CTabFolder) getContainer();
        container.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        container.setVisible(false);

        try {
            createComponentDescriptionPage();
            createWorkflowEditorPage();
            createMappingPage();
        } catch (PartInitException e) {
            LOG.warn(StringUtils.format("Error opening the workflow integration editor.\n%s", e.getMessage()));
            MessageDialog.openError(getEditorSite().getShell(), "Error opening editor", "Could not open the editor.\n"
                + e.getMessage());
        }
        container.setVisible(true);
    }

    private void createComponentDescriptionPage() {
        ComponentDescriptionPage componentDescriptionPage = new ComponentDescriptionPage(this, container);
        componentDescriptionPage.setText(COMPONENT_DESCRIPTION);
        componentDescriptionPage.setControl(componentDescriptionPage.generatePage());
    }

    private void createWorkflowEditorPage() throws PartInitException {
        workflowEditorPage = new WorkflowEditorPage(this);
        workflowEditorPageIndex = addPage(workflowEditorPage, getEditorInput());
        setPageText(workflowEditorPageIndex, WORKFLOW_EDITOR);
        ActionRegistry registry = (ActionRegistry) workflowEditorPage.getAdapter(ActionRegistry.class);
        undoAction = registry.getAction(ActionFactory.UNDO.getId());
        redoAction = registry.getAction(ActionFactory.REDO.getId());
    }

    private void createMappingPage() {
        mappingTreeContentProvider = new MappingTreeContentProvider();
        MappingPage mappingPage = new MappingPage(this, container, mappingTreeContentProvider);
        mappingPage.setText(MAPPING);
        mappingPage.setControl(mappingPage.generatePage());
        mappingPage.update();
        if (isEditMode()) {
            mappingTreeContentProvider.restoreCheckedMappingNodes(getController().getPersistedEndpointAdapters());
            mappingPage.refreshTree();
        }
    }

    @Override
    public void integrate() {
        getController().createEndpointAdapters(mappingTreeContentProvider.getEndpointAdapters());
        if (getController().integrateWorkflow()) {
            getSite().getPage().closeEditor(this, false);
        } else {
            MessageDialog.openError(getSite().getShell(), "Error integrating component.",
                "The integration of the workflow was not successfull. Please see the the log for more details.");
        }
    }

    @Override
    public boolean isEditMode() {
        return getController().isEditMode();
    }

    public WorkflowIntegrationController getController() {
        return this.workflowIntegrationController;
    }

    public WorkflowEditorPage getWorkflowEditor() {
        return workflowEditorPage;
    }

    public void setWorkflowEditorPageImage(Image image) {
        setPageImage(workflowEditorPageIndex, image);
    }

    @Override
    protected void setInput(IEditorInput input) throws IllegalArgumentException {
        if (input instanceof WorkflowIntegrationEditorInput) {
            this.workflowIntegrationController = input.getAdapter(WorkflowIntegrationController.class);
            getController().setValue(IntegrationConstants.IS_ACTIVE, true, ConfigurationContext.COMMON_SETTINGS);
        }
        setPartName(input.getName());
        super.setInput(input);
    }

    public void updatePartName() {
        setPartName(workflowIntegrationController.getEditorTitle());
    }

    @Override
    public void dispose() {
        if (getWorkflowEditor() != null) {
            getWorkflowEditor().dispose();
        }
        IToolBarManager toolBarManager = getEditorSite().getActionBars().getToolBarManager();
        toolBarManager.remove(toolBarManager.find(undoAction.getId()));
        toolBarManager.remove(toolBarManager.find(redoAction.getId()));
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(this);
        super.dispose();
    }

    public void updateContent() {
        CTabItem selection = ((CTabFolder) getContainer()).getSelection();
        if (selection instanceof IntegrationEditorPage) {
            ((IntegrationEditorPage) selection).update();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IContentOutlinePage.class || adapter == ZoomManager.class || adapter == ActionRegistry.class
            || adapter == IPropertySheetPage.class) {
            return (T) getWorkflowEditor().getAdapter(adapter);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void partActivated(IWorkbenchPart part) {
        if (part == this) {
            updateActions();
        }
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
        if (part == this) {
            updateActions();
        }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart arg0) {
        // Intentionally left empty
    }

    @Override
    public void partClosed(IWorkbenchPart part) {
        // Intentionally left empty
    }

    @Override
    public void partOpened(IWorkbenchPart arg0) {
        // Intentionally left empty
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().bringToTop(this);
        MessageDialog.openInformation(this.getContainer().getShell(), "Unable to save for Workflow Integration Editor tabs.",
            StringUtils.format(
                "The Workflow Integration configuration of editor tab '%s' cannot be saved to a file.\n"
                    + "In order to persist a Workflow Integration configuration, please use the button "
                    + "'%s' in the Workflow Integration Editor.",
                this.getTitle(), getButtonTextIntegrate()));
    }

    @Override
    public String getButtonTextIntegrate() {
        if (isEditMode()) {
            return BUTTON_UPDATE;
        }
        return BUTTON_INTEGRATE;
    }

    @Override
    public String getDialogTitle() {
        return DIALOG_TITLE;
    }
}
