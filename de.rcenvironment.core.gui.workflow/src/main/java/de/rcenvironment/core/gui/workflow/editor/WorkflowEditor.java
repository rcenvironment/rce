/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.Tool;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.model.api.ComponentInstallationBuilder;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessageStore;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.gui.utils.incubator.ContextMenuItemRemover;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.GUIWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.gui.workflow.WorkflowNodeLabelConnectionHelper;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeLabelConnectionCreateCommand;
import de.rcenvironment.core.gui.workflow.editor.handlers.OpenConnectionEditorHandler;
import de.rcenvironment.core.gui.workflow.editor.handlers.OpenConnectionsViewHandler;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowDescriptionValidationUtils;
import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowEditorEditPartFactory;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;
import de.rcenvironment.core.gui.workflow.view.outline.OutlineView;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Editor window which opens when selecting a workflow file in the project explorer.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Sascha Zur
 * @author Doreen Seider
 * @author Oliver Seebach
 * @author Robert Mischke
 * @author David Scholz
 * @author Goekhan Guerkan
 * @author Jan Flink
 * @author Martin Misiak
 * @author Brigitte Boden
 * @author Alexander Weinert
 */
public class WorkflowEditor extends GraphicalEditor
    implements ITabbedPropertySheetPageContributor, MouseListener {

    /** Property change event. */
    public static final int PROP_FINAL_WORKFLOW_DESCRIPTION_SET = 0x300;

    /** Property change event. */
    public static final int PROP_WORKFLOW_VAILDATION_FINISHED = 0x400;

    /** Constant. */
    public static final String COMPONENTNAMES_WITH_VERSION = " (%s)";

    /** Key for show labels preference. */
    public static final String SHOW_LABELS_PREFERENCE_KEY = "showConnections";

    /**
     * Flag to indicate if the workflow editor is in dragging state regarding bendpoints.
     */
    public static final String DRAG_STATE_BENDPOINT = "DRAG_STATE_BENDPOINT";

    protected static final int DEFAULT_TOLERANCE = 10;

    protected static final int UNDO_LIMIT = 10;

    private static final int MOVEMENT = 1;

    private static final Log LOGGER = LogFactory.getLog(WorkflowEditor.class);

    private static final char OPEN_CONNECTION_VIEW_KEYCODE = 'c'; // for ALT + c as shortcut

    private static final int MINUS_ONE = -1;

    private static final int TILE_OFFSET = 30;

    private static final int TILE_SIZE = 60;

    private static final int MAXIMUM_LOCAL_COMPONENT_LOADING_WAIT_SECONDS = 20;

    protected final ServiceRegistryPublisherAccess serviceRegistryAccess;

    protected final WorkflowExecutionService workflowExecutionService;

    protected final LocalComponentRegistrationService localComponentRegistrationService;

    protected WorkflowDescription workflowDescription;

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    private GraphicalViewer viewer;

    private int mouseX;

    private int mouseY;

    private final ToolIntegrationContextRegistry toolIntegrationRegistry;

    private ResourceTracker resourceListener = new ResourceTracker();

    public WorkflowEditor() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        // note: these are not all services; decide whether they should be fetched centrally here, or where they are needed
        toolIntegrationRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
        workflowExecutionService = serviceRegistryAccess.getService(WorkflowExecutionService.class);
        localComponentRegistrationService = serviceRegistryAccess.getService(LocalComponentRegistrationService.class);
        setEditDomain(new DefaultEditDomain(this));

    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    public CommandStack getEditorsCommandStack() {
        return getCommandStack();
    }

    protected void openConnectionEditor() {

        OpenConnectionsViewHandler openConnectionViewHandler = new OpenConnectionsViewHandler();
        try {
            openConnectionViewHandler.execute(new ExecutionEvent());
        } catch (ExecutionException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    protected void initializeGraphicalViewer() {

        setViewer(getGraphicalViewer());
        WorkflowScalableFreeformRootEditPart rootEditPart = new WorkflowScalableFreeformRootEditPart();
        getViewer().setRootEditPart(rootEditPart);
        getViewer().setEditPartFactory(new WorkflowEditorEditPartFactory());

        getCommandStack().setUndoLimit(UNDO_LIMIT);

        getViewer().getControl().addKeyListener(new WorkflowEditorKeyListener());

        getViewer().setContents(workflowDescription);

        getViewer().addDropTargetListener(new TemplateTransferDropTargetListener(getViewer()));

        ContextMenuProvider cmProvider = new WorkflowEditorContextMenuProvider(getViewer(), getActionRegistry());
        getViewer().setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, getViewer());

        WorkflowZoomManager zoomManager = (WorkflowZoomManager) rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(zoomManager));
        getActionRegistry().registerAction(new ZoomOutAction(zoomManager));
        getViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
        tabbedPropertySheetPage = new TabbedPropertySheetPage(this);

        getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
                StructuredSelection structuredSelection = ((StructuredSelection) selectionChangedEvent.getSelection());
                for (Object structuredSelectionObject : structuredSelection.toList()) {
                    if (structuredSelectionObject instanceof ConnectionPart) {
                        ConnectionPart connectionPart = ((ConnectionPart) structuredSelectionObject);
                        if (getViewer().getSelectedEditParts().contains(connectionPart)) {
                            connectionPart.getConnectionFigure().setForegroundColor(ColorConstants.blue);
                            connectionPart.showLabel();
                        }
                    }
                    if (structuredSelectionObject instanceof WorkflowNodePart) {
                        WorkflowNodePart nodePart = (WorkflowNodePart) structuredSelectionObject;
                        ComponentInterface ci = ((WorkflowNode) nodePart.getModel()).getComponentDescription()
                            .getComponentInstallation().getComponentInterface();
                        String id = ci.getIdentifierAndVersion().substring(0,
                            ci.getIdentifierAndVersion().lastIndexOf(ComponentConstants.ID_SEPARATOR));
                        if (toolIntegrationRegistry.hasTIContextMatchingPrefix(id)) {
                            setHelp(ComponentConstants.INTEGRATION_CONTEXTUAL_HELP_PLACEHOLDER_ID);
                        } else {
                            setHelp(id);
                        }
                    } else {
                        setHelp(null);
                    }
                }
                removeConnectionColorsAndLabel();
            }
        });

        getViewer().getControl().setData(DRAG_STATE_BENDPOINT, false);

        getViewer().getControl().addMouseListener(this);

        // Snap to grid and geometry actions, enable geometry automatically.
        getActionRegistry().registerAction(new ToggleGridAction(getGraphicalViewer()));
        getActionRegistry().registerAction(new ToggleSnapToGeometryAction(getGraphicalViewer()));

        getViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, true);
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING,
            new Dimension((WorkflowNodePart.SMALL_WORKFLOW_NODE_WIDTH - 1) / 2,
                (WorkflowNodePart.SMALL_WORKFLOW_NODE_WIDTH - 1) / 2));

        // register activate context for context sensitive key bindings
        IContextService contextService = (IContextService) getSite().getService(IContextService.class);
        contextService.activateContext("de.rcenvironment.rce.gui.workflow.editor.scope");

        // preferences store - initialize labels not to be shown
        IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
        preferenceStore.setValue(WorkflowEditor.SHOW_LABELS_PREFERENCE_KEY, false);

        // remove unwanted menu entries from workflow editor's context menu
        ContextMenuItemRemover.removeUnwantedMenuEntries(getViewer().getControl());
        ResourcesPlugin.getWorkspace().addResourceChangeListener(getResourceListener());

    }

    @Override
    public void mouseUp(MouseEvent ev) {
        // Nothing to do here.
    }

    @Override
    public void mouseDown(MouseEvent ev) {
        // Nothing to do here.
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
            OpenConnectionEditorHandler openConnectionEditorHandler = new OpenConnectionEditorHandler(source,
                target);
            try {
                openConnectionEditorHandler.execute(new ExecutionEvent());
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Shows the number of channels for all connections.
     */
    public void showAllConnectionLabels() {
        for (Object connectionPartObject : getViewer().getEditPartRegistry().values()) {
            if (connectionPartObject instanceof ConnectionPart) {
                ((ConnectionPart) connectionPartObject).showLabel();
            }
        }
    }

    /**
     * Hides the number of channel for all connections.
     */
    public void hideUnselectedConnectionLabels() {
        for (Object connectionPartObject : getViewer().getEditPartRegistry().values()) {
            if (connectionPartObject instanceof ConnectionPart) {
                ConnectionPart part = ((ConnectionPart) connectionPartObject);
                // only hide label if connection is not selected
                int selectedCode = 2;
                if (part.getSelected() != selectedCode) {
                    part.hideLabel();
                }
            }
        }

    }

    public void removeConnectionColorsAndLabel() {
        for (Object connectionPartObject : getViewer().getEditPartRegistry().values()) {
            if (connectionPartObject instanceof ConnectionPart
                && !getViewer().getSelectedEditParts().contains(connectionPartObject)) {
                ConnectionPart connectionPart = (ConnectionPart) connectionPartObject;
                connectionPart.getConnectionFigure().setForegroundColor(ColorConstants.black);
                IPreferenceStore prefs = Activator.getInstance().getPreferenceStore();
                boolean labelsVisible = prefs.getBoolean(SHOW_LABELS_PREFERENCE_KEY);
                // reset color and selection only when not selected
                if (!labelsVisible) {
                    connectionPart.hideLabel();
                }
            }
        }
    }

    protected ConnectionPart selectConnection(MouseEvent ev) {
        for (Object editPart : getViewer().getEditPartRegistry().values()) {
            if (editPart instanceof ConnectionPart) {
                int offsetX = ((FigureCanvas) getViewer().getControl()).getViewport().getViewLocation().x;
                int offsetY = ((FigureCanvas) getViewer().getControl()).getViewport().getViewLocation().y;
                ConnectionPart connectionPart = ((ConnectionPart) editPart);
                PointList connectionPoints = connectionPart.getConnectionFigure().getPoints();
                Rectangle toleranceRectangle = new Rectangle(ev.x + offsetX - DEFAULT_TOLERANCE / 2,
                    ev.y + offsetY - DEFAULT_TOLERANCE / 2, DEFAULT_TOLERANCE, DEFAULT_TOLERANCE);
                if (connectionPoints.intersects(toleranceRectangle)) {
                    getViewer().select(connectionPart);
                    getViewer().reveal(connectionPart);
                    return connectionPart;
                }
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        serviceRegistryAccess.dispose();
        super.dispose();
    }

    /**
     * Used in {@link #setInput(IEditorInput)}. Can be overridden.
     */
    protected void loadWorkflowFromFile(final File wfFile, final GUIWorkflowDescriptionLoaderCallback wfdc) {

        if (wfFile != null) {
            Job job = new Job(Messages.openWorkflow) {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask("Waiting for component availability", 1);
                        if (!localComponentRegistrationService.waitForLocalComponentInitialization(
                            MAXIMUM_LOCAL_COMPONENT_LOADING_WAIT_SECONDS, TimeUnit.SECONDS)) {
                            LOGGER.warn("Local component/tool initialization did not complete within "
                                + MAXIMUM_LOCAL_COMPONENT_LOADING_WAIT_SECONDS + " seconds; attempting to open the workflow anyway");
                        }
                        monitor.worked(1);

                        monitor.beginTask(Messages.loadingComponents, 2);
                        monitor.worked(1);
                        workflowDescription = workflowExecutionService
                            .loadWorkflowDescriptionFromFileConsideringUpdates(wfFile, wfdc);
                        initializeWorkflowDescriptionListener();
                        monitor.worked(1);

                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                if (getViewer().getControl() != null) {
                                    getViewer().setContents(workflowDescription);
                                    if (getEditorSite() != null) {
                                        setFocus();
                                    }
                                    validateWorkflow();
                                    firePropertyChange(PROP_FINAL_WORKFLOW_DESCRIPTION_SET);
                                }
                            }
                        });
                    } catch (final WorkflowFileException | InterruptedException e) {
                        LogFactory.getLog(getClass()).error("Failed to open workflow: " + wfFile.getAbsolutePath(), e);
                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                // do not use Display.getDefault().getActiveShell() as this might return
                                // the progress monitor dialog
                                closeEditorAndShowMessage(e.getMessage());
                            }

                        });
                    } finally {
                        monitor.done();
                    }
                    return Status.OK_STATUS;

                };
            };
            job.setUser(true);
            job.schedule();
        }

    }

    @Override
    protected void setInput(final IEditorInput input) {

        super.setInput(input);
        workflowDescription = new WorkflowDescription("");
        GUIWorkflowDescriptionLoaderCallback workflowDescriptionLoader = null;
        final File wfFile;

        if (input instanceof FileEditorInput) {
            FileEditorInput fileEditorInput = (FileEditorInput) input;
            IFile workspaceWfFile = fileEditorInput.getFile();
            workflowDescriptionLoader = new GUIWorkflowDescriptionLoaderCallback(workspaceWfFile);
            if (workspaceWfFile != null && workspaceWfFile.getRawLocation() != null) {
                setPartName(workspaceWfFile.getName());
                wfFile = new File(workspaceWfFile.getLocation().toOSString());
            } else {
                closeEditorAndShowMessage(
                    StringUtils.format("Workflow file could not be found: %s", fileEditorInput.getFile()));
                wfFile = null;
            }
        } else if (input instanceof FileStoreEditorInput) {
            FileStoreEditorInput fileStoreEditorInput = (FileStoreEditorInput) input;
            wfFile = new File(fileStoreEditorInput.getURI());
            setPartName(wfFile.getName());
            workflowDescriptionLoader = new GUIWorkflowDescriptionLoaderCallback();
        } else {
            // should not happen
            MessageDialog.openError(Display.getDefault().getActiveShell(), "Workflow File Error",
                "Failed to load workflow file for an unknown reason.");
            wfFile = null;
        }

        loadWorkflowFromFile(wfFile, workflowDescriptionLoader);

    }

    protected void closeEditorAndShowMessage(final String message) {
        MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Workflow File Error",
            message);
        WorkflowEditor.this.getSite().getPage().closeEditor(WorkflowEditor.this, false);
    }

    /**
     * Makes the editor listen to changes in the underlying {@link WorkflowDescription}.
     */
    protected void initializeWorkflowDescriptionListener() {
        workflowDescription.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                WorkflowEditor.this.updateDirty();
            }
        });
    }

    protected void updateDirty() {
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        try {
            if (getEditorInput() instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) getEditorInput()).getFile();
                if (file.exists()) {
                    WorkflowDescriptionPersistenceHandler wdHandler = new WorkflowDescriptionPersistenceHandler();
                    workflowDescription = updateExecutionInformation(workflowDescription, wdHandler,
                        file.getRawLocation().toFile());
                    file.setContents(
                        new ByteArrayInputStream(
                            wdHandler.writeWorkflowDescriptionToStream(workflowDescription).toByteArray()),
                        true, // keep saving, even if IFile is out of sync with the Workspace
                        false, // dont keep history
                        monitor); // progress monitor
                    workflowDescription.firePropertyChange(WorkflowDescription.PROPERTY_NODES);
                    workflowDescription.firePropertyChange(WorkflowDescription.PROPERTY_LABEL);
                } else {
                    doSaveAs();
                }

            } else if (getEditorInput() instanceof FileStoreEditorInput) {
                File file = new File(
                    ((FileStoreEditorInput) getEditorInput()).getURI().getPath().replaceFirst("/", ""));
                WorkflowDescriptionPersistenceHandler wdHandler = new WorkflowDescriptionPersistenceHandler();
                try (ByteArrayOutputStream outStream = wdHandler
                    .writeWorkflowDescriptionToStream(workflowDescription);) {
                    if (file.canWrite()) {
                        FileUtils.writeByteArrayToFile(file, outStream.toByteArray());
                    }
                }
            }
            getCommandStack().markSaveLocation();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (OutOfMemoryError error) {
            showMemoryExceedingWarningMessage();
            error.printStackTrace();
        }
        // set the dirty state relative to the current command stack position
        getCommandStack().markSaveLocation();
        validateWorkflow();

    }

    // get the latest execution information (executing nodes for wf controller and
    // components) from underlying .wf file and set this
    // information to the given workflow description needed to ensure that changes
    // made by the workflow execution wizard regarding executing
    // nodes are not overwritten by the workflow editor which must not change those
    // information at all (consequence of merging workflow and
    // execution information in one .wf file which is intended to be changed anyway)
    // --seid_do
    private WorkflowDescription updateExecutionInformation(WorkflowDescription wd,
        WorkflowDescriptionPersistenceHandler wdHandler, File file) {
        try {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                String workflowControllerNodeId = wdHandler.readWorkflowControllerNodeId(fileInputStream);
                wd.setControllerNode(NodeIdentifierUtils
                    .parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(workflowControllerNodeId));
            }
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                Map<WorkflowNodeIdentifier, String> componentControllerNodeIds = wdHandler
                    .readComponentControllerNodeIds(fileInputStream);
                for (WorkflowNode wn : wd.getWorkflowNodes()) {
                    ComponentInstallationBuilder builder = ComponentInstallationBuilder
                        .fromComponentInstallation(wn.getComponentDescription().getComponentInstallation());
                    if (componentControllerNodeIds.containsKey(wn.getIdentifierAsObject())) { // newly added components
                                                                                              // doesn't appear in the
                                                                                              // map
                        String compNodeId = componentControllerNodeIds.get(wn.getIdentifierAsObject());
                        builder.setNodeId(NodeIdentifierUtils
                            .parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(compNodeId));
                    }
                    wn.getComponentDescription().setComponentInstallation(builder.build());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to consider stored execution information on workflow saving", e);
        }
        return wd;
    }

    public void validateWorkflow() {
        ComponentValidationMessageStore.getInstance().emptyMessageStore(); // delete any previous validation errors

        List<?> list = getViewer().getRootEditPart().getChildren();
        WorkflowDescriptionValidationUtils.validateWorkflowDescription(workflowDescription, false, true);
        for (Object object : list) {
            if (!(object instanceof WorkflowPart)) {
                continue;
            }

            WorkflowPart workflowPart = (WorkflowPart) object;
            List<?> children = workflowPart.getChildren();
            for (Object child : children) {
                if (!(child instanceof WorkflowNodePart)) {
                    continue;
                }

                WorkflowNodePart workflowNodePart = (WorkflowNodePart) child;
                final WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
                if (!workflowNode.isValid()) {
                    workflowNodePart.updateValid();
                }
            }
        }
        firePropertyChange(PROP_WORKFLOW_VAILDATION_FINISHED);
    }

    @Override
    public void doSaveAs() {
        FileDialog fd = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
        fd.setText("Save As...");
        String[] filterExt = { "*.wf" };
        fd.setFilterExtensions(filterExt);
        fd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
        fd.setFileName(getTitle());
        fd.setOverwrite(true);
        String selected = fd.open();
        if (selected == null) {
            return;
        }
        if (!selected.substring(selected.lastIndexOf('.') + 1).toLowerCase().equals("wf")) {
            selected += ".wf";
        }
        File file = null;
        try {
            file = new File(selected);
            FileWriter fw = new FileWriter(file);
            WorkflowDescriptionPersistenceHandler wdHandler = new WorkflowDescriptionPersistenceHandler();
            byte[] stream = wdHandler.writeWorkflowDescriptionToStream(workflowDescription).toByteArray();
            for (byte element : stream) {
                fw.append((char) element); // progress monitor
            }
            fw.flush();
            fw.close();
            this.getEditorSite().getPage().closeEditor(this, false);
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            if (file.getAbsolutePath().startsWith(workspace.getRoot().getFullPath().toFile().getAbsolutePath())) {
                IFile[] ifile = workspace.getRoot().findFilesForLocationURI(file.toURI());
                if (ifile.length == 1) {
                    EditorsHelper.openFileInEditor(ifile[0]);
                }
            } else {
                EditorsHelper.openExternalFileInEditor(file);
                LOGGER.warn(
                    "Saved workflow openend as external file (not in workspace location). Executing the workflow might not work.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (OutOfMemoryError error) {
            if (file != null) {
                file.deleteOnExit();
            }
            showMemoryExceedingWarningMessage();
            error.printStackTrace();
        } catch (PartInitException e) {
            LOGGER.warn("Could not open new file. ", e);
        }
        // Refresh current project
        try {
            ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException e) {
            LOGGER.warn("Could not refresh Project Explorer. ", e);
        }
    }

    private void showMemoryExceedingWarningMessage() {
        MessageBox messageBox = new MessageBox(getViewer().getControl().getShell(), SWT.ICON_WARNING | SWT.OK);
        messageBox.setMessage(Messages.memoryExceededWarningMessage);
        messageBox.setText(Messages.memoryExceededWarningHeading);
        messageBox.open();
    }

    @Override
    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    @Override
    public String getContributorId() {
        return getSite().getId();
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IPropertySheetPage.class) {
            if (tabbedPropertySheetPage == null || tabbedPropertySheetPage.getControl() == null
                || tabbedPropertySheetPage.getControl().isDisposed()) {
                tabbedPropertySheetPage = new TabbedPropertySheetPage(this);
            }
            return tabbedPropertySheetPage;
        } else if (type == IContextProvider.class) {
            return new WorkflowEditorHelpContextProvider(getViewer());
        } else if (type == IContentOutlinePage.class) {
            return new OutlineView(getGraphicalViewer());
        } else if (type == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }

        return super.getAdapter(type);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    public GraphicalViewer getViewer() {
        return viewer;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    /**
     * Key listener that implements some shortcuts for the workflow editor.
     * 
     * @author Oliver Seebach
     * @author Jascha Riedel(#0013977) moved selection and connection tool to ui.bindings
     */
    public final class WorkflowEditorKeyListener implements KeyListener {

        @Override
        public void keyReleased(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.stateMask == SWT.ALT && e.keyCode == OPEN_CONNECTION_VIEW_KEYCODE) {
                openConnectionEditor();
            } else if (e.keyCode == SWT.ARROW_UP) {
                movePart(0, -MOVEMENT);
            } else if (e.keyCode == SWT.ARROW_DOWN) {
                movePart(0, MOVEMENT);
            } else if (e.keyCode == SWT.ARROW_LEFT) {
                movePart(-MOVEMENT, 0);
            } else if (e.keyCode == SWT.ARROW_RIGHT) {
                movePart(MOVEMENT, 0);
            }

        }
    }

    private void movePart(final int movementHorizontal, final int movementVertical) {
        @SuppressWarnings("unchecked") List<AbstractGraphicalEditPart> selected = getGraphicalViewer().getSelectedEditParts();
        if (selected.isEmpty()) {
            return;
        }
        getCommandStack().execute(new MovementCommand(selected, movementHorizontal, movementVertical));
    }

    /**
     * Command to move node via arrow keys.
     * 
     * @author Hendrik Abbenhaus
     */
    private class MovementCommand extends Command {

        private int movementHorizontal = 0;

        private int movementVertical = 0;

        private List<AbstractGraphicalEditPart> selected;

        /**
         * Constructor for command.
         * 
         * @param sel selected Parts
         * @param h horizontal movement
         * @param v vertival movement
         */
        MovementCommand(List<AbstractGraphicalEditPart> sel, int h, int v) {
            this.movementHorizontal = h;
            this.movementVertical = v;
            this.selected = sel;
        }

        @Override
        public void undo() {
            super.undo();
            setValue(-movementHorizontal, -movementVertical);
        }

        @Override
        public void redo() {
            super.redo();
            setValue(movementHorizontal, movementVertical);
        }

        @Override
        public void execute() {
            super.execute();
            setValue(movementHorizontal, movementVertical);
        }

        private void setValue(int h, int v) {
            for (AbstractGraphicalEditPart s : selected) {
                if (s.getModel() instanceof WorkflowLabel) {
                    WorkflowLabel label = (WorkflowLabel) s.getModel();
                    label.setLocation(label.getX() + h, label.getY() + v);
                } else if (s.getModel() instanceof WorkflowNode) {
                    WorkflowNode node = (WorkflowNode) s.getModel();
                    node.setLocation(node.getX() + h, node.getY() + v);
                }
            }
        }
    }

    private void closeEditor(boolean save) {
        getSite().getPage().closeEditor(WorkflowEditor.this, save);
    }

    private void superSetInput(IEditorInput input) {

        if (getEditorInput() != null) {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().removeResourceChangeListener(getResourceListener());
        }

        super.setInput(input);

        if (getEditorInput() != null) {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().addResourceChangeListener(getResourceListener());
            setPartName(file.getName());
        }
    }

    public void onPaletteDoubleClick(Tool tool) {
        if (!(tool instanceof PaletteCreationTool)) {
            return;
        }
        PaletteCreationTool creationTool = (PaletteCreationTool) tool;
        CreationFactory factory = creationTool.getFactory();
        Object objectType = factory.getObjectType();
        Object newObject = factory.getNewObject();

        WorkflowNodeLabelConnectionCreateCommand createCommand = getCreateCommand(newObject, objectType);
        getEditorsCommandStack().execute(createCommand);

        // select new part and activate corresponding properties view
        for (Object editpart : getViewer().getContents().getChildren()) {
            if (editpart instanceof EditPart) {
                EditPart currentEditPart = (EditPart) editpart;
                if (currentEditPart.getModel().equals(newObject)) {
                    getViewer().select(currentEditPart);
                    tabbedPropertySheetPage.selectionChanged(WorkflowEditor.this,
                        getViewer().getSelection());
                    break;
                }
            }
        }
    }

    private WorkflowNodeLabelConnectionCreateCommand getCreateCommand(Object newObject, Object objectType) {
        final int offset = 20;
        WorkflowDescription model = (WorkflowDescription) getViewer().getContents().getModel();
        if (objectType == WorkflowNode.class) {
            // Set bounds of new component, if intersects with existing translate by
            // 30,30
            Rectangle rectangle = new Rectangle(TILE_OFFSET, TILE_OFFSET, TILE_SIZE, TILE_SIZE);
            for (WorkflowNode node : model.getWorkflowNodes()) {
                Rectangle nodeRect = new Rectangle(node.getX(), node.getY(), TILE_SIZE, TILE_SIZE);
                if (nodeRect.intersects(rectangle)) {
                    rectangle.translate(offset, offset);
                }
            }
            WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(
                (WorkflowNode) newObject, model, rectangle);
            return helper.createCommand();
        }

        if (objectType == WorkflowLabel.class) {
            Rectangle rectangle = new Rectangle(TILE_OFFSET, TILE_OFFSET, MINUS_ONE, MINUS_ONE);
            WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(
                (WorkflowLabel) newObject, model,
                rectangle);
            return helper.createCommand();
        }
        return null;
    }

    public void setHelp(String id) {
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getViewer().getControl(), id);
//        LogFactory.getLog(this.getClass()).debug(StringUtils.format("Set help ID to %s", id));
    }

    public ResourceTracker getResourceListener() {
        return resourceListener;
    }

    public void setViewer(GraphicalViewer viewer) {
        this.viewer = viewer;
    }

    /**
     * This class listens to changes to the file system in the workspace, and makes changes accordingly. An open, saved file gets deleted =
     * close the editor. An open file gets renamed or moved = change the editor's input accordingly.
     * 
     * @author Goekhan Guerkan
     */
    private class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {

        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            IResourceDelta delta = event.getDelta();
            try {
                if (delta != null) {
                    delta.accept(this);
                }
            } catch (CoreException exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public boolean visit(IResourceDelta delta) {

            if (delta == null || !(getEditorInput() instanceof IFileEditorInput)
                || !delta.getResource().equals(((IFileEditorInput) getEditorInput()).getFile())) {
                return true;
            }

            if (delta.getKind() == IResourceDelta.REMOVED) {
                Display display = getSite().getShell().getDisplay();
                if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) { // if the file was deleted
                    display.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            if (!isDirty()) {
                                closeEditor(false);
                            }
                        }
                    });

                    // moved or renamed
                } else {
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
                    display.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            superSetInput(new FileEditorInput(newFile));
                        }
                    });
                }
            }
            return false;
        }
    }
}
