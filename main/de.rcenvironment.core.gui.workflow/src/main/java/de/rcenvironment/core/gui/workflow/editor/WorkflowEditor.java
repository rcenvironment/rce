/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.incubator.ContextMenuItemRemover;
import de.rcenvironment.core.gui.wizards.toolintegration.ShowIntegrationEditWizardHandler;
import de.rcenvironment.core.gui.wizards.toolintegration.ShowIntegrationRemoveHandler;
import de.rcenvironment.core.gui.wizards.toolintegration.ShowIntegrationWizardHandler;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.LoadingWorkflowDescriptionHelper;
import de.rcenvironment.core.gui.workflow.WorkflowNodeLabelConnectionHelper;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeLabelConnectionCreateCommand;
import de.rcenvironment.core.gui.workflow.editor.handlers.OpenConnectionEditorHandler;
import de.rcenvironment.core.gui.workflow.editor.handlers.OpenConnectionsViewHandler;
import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.EditorEditPartFactory;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;
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
 */
public class WorkflowEditor extends GraphicalEditorWithFlyoutPalette implements ITabbedPropertySheetPageContributor,
    DistributedComponentKnowledgeListener {

    /** Property change event. */
    public static final int PROP_FINAL_WORKFLOW_DESCRIPTION_SET = 0x300;

    /** Property change event. */
    public static final int PROP_WORKFLOW_VAILDATION_FINISHED = 0x400;

    /** Constant. */
    public static final String COMPONENTNAMES_WITH_VERSION = " (%s)";

    /** Key for show labels preference. */
    public static final String SHOW_LABELS_PREFERENCE_KEY = "showConnections";

    protected static final int DEFAULT_TOLERANCE = 10;

    private static final String DRAG_STATE = "DRAG_STATE";

    private static final char SELECTION_KEYCODE = 's'; // for ALT + s as shortcut

    private static final char CONNECTION_KEYCODE = 'd'; // for ALT + d as shortcut

    private static final char OPEN_CONNECTION_VIEW_KEYCODE = 'c'; // for ALT + c as shortcut

    private static final String MENU_LISTENER_MARKER = "MENU_LISTENER_MARKER";

    private static final String TOOL_DEACTIVATE_LABEL = "Deactivate Tool";

    private static final String TOOL_EDIT_LABEL = "Edit Tool";

    private static final String TOOL_INTEGRATE_LABEL = "Integrate Tool";

    private static final String TOOLINTEGRATION_ITEM = "toolIntegrationItem";

    private static final int MINUS_ONE = -1;

    private static final int UNDO_LIMIT = 10;

    private static final int TILE_OFFSET = 30;

    private static final int TILE_SIZE = 60;

    private boolean allLabelsShown = false;

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    private GraphicalViewer viewer;

    private WorkflowDescription workflowDescription;

    private ZoomManager zoomManager;

    private PaletteRoot paletteRoot;

    private MenuItem toolIntegrationPaletteMenuItem;

    private MenuItem editToolPaletteMenuItem;

    private MenuItem deactivateToolPaletteMenuItem;

    private PaletteViewer paletteViewer = null;

    private ConnectionCreationToolEntry connectionCreationToolEntry = null;

    private File inputFile = null;

    private IFile workspaceFile = null;

    private int mouseX;

    private int mouseY;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private final ToolIntegrationContextRegistry toolIntegrationRegistry;

    public WorkflowEditor() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        toolIntegrationRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
        setEditDomain(new DefaultEditDomain(this));
    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    // Switch activate tool in palette to the Draw Connection tool
    private void switchToConnectionTool() {
        if (connectionCreationToolEntry == null) {
            for (Object paletteGroupObject : paletteViewer.getPaletteRoot().getChildren()) {
                if (paletteGroupObject instanceof PaletteGroup) {
                    PaletteGroup paletteGroup = (PaletteGroup) paletteGroupObject;
                    for (Object paletteEntryObject : paletteGroup.getChildren()) {
                        if (paletteEntryObject instanceof ConnectionCreationToolEntry) {
                            ConnectionCreationToolEntry entry = (ConnectionCreationToolEntry) paletteEntryObject;
                            connectionCreationToolEntry = entry;
                            paletteViewer.setActiveTool(entry);
                        }
                    }
                }
            }
        } else {
            paletteViewer.setActiveTool(connectionCreationToolEntry);
        }
    }

    // Switch activate tool in palette to the Selection tool
    private void switchToSelectionTool() {
        paletteViewer.setActiveTool(paletteViewer.getPaletteRoot().getDefaultEntry());
    }

    public CommandStack getEditorsCommandStack() {
        return getCommandStack();
    }

    private void openConnectionEditor() {

        OpenConnectionsViewHandler openConnectionViewHandler = new OpenConnectionsViewHandler();
        try {
            openConnectionViewHandler.execute(new ExecutionEvent());
        } catch (ExecutionException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        final List<ComponentInstallation> componentInstallations = new ArrayList<ComponentInstallation>();
        IProgressService service = (IProgressService) PlatformUI.getWorkbench()
            .getService(IProgressService.class);
        try {
            service.run(false, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask(Messages.fetchingComponents, 3);
                        monitor.worked(2);
                        componentInstallations.addAll(getInitialComponentKnowledge().getAllInstallations());
                        monitor.worked(1);
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        WorkflowPaletteFactory factory = new WorkflowPaletteFactory();
        paletteRoot = factory.createPalette(componentInstallations);
        return paletteRoot;
    }

    @Override
    protected void initializeGraphicalViewer() {

        viewer = getGraphicalViewer();

        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setEditPartFactory(new EditorEditPartFactory());

        getCommandStack().setUndoLimit(UNDO_LIMIT);

        viewer.getControl().addKeyListener(new WorkflowEditorKeyListener());

        viewer.setContents(workflowDescription);

        viewer.addDropTargetListener(new TemplateTransferDropTargetListener(
            viewer));

        ContextMenuProvider cmProvider = new WorkflowEditorContextMenuProvider(
            viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);

        zoomManager = ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        zoomManager.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
        viewer.getControl().addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseScrolled(MouseEvent arg0) {
                if (arg0.stateMask == SWT.CONTROL) {
                    int notches = arg0.count;
                    if (notches < 0) {
                        zoomManager.zoomOut();
                    } else {
                        zoomManager.zoomIn();
                    }
                }
            }
        });

        tabbedPropertySheetPage = new TabbedPropertySheetPage(this);

        registerChangeListeners();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
                StructuredSelection structuredSelection = ((StructuredSelection) selectionChangedEvent.getSelection());
                for (Object structuredSelectionObject : structuredSelection.toList()) {
                    if (structuredSelectionObject instanceof ConnectionPart) {
                        ConnectionPart connectionPart = ((ConnectionPart) structuredSelectionObject);
                        if (viewer.getSelectedEditParts().contains(connectionPart)) {
                            connectionPart.getConnectionFigure().setForegroundColor(ColorConstants.blue);
                            connectionPart.showLabel();
                        }
                    }
                }
                removeConnectionColorsAndLabel();
            }
        });

        viewer.getControl().setData(DRAG_STATE, false);
        viewer.getControl().addDragDetectListener(new DragDetectListener() {

            @Override
            public void dragDetected(DragDetectEvent arg0) {
                // mark control as currently dragging to prevent erroneous selection of connection
                viewer.getControl().setData(DRAG_STATE, true);
            }
        });

        viewer.getControl().addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent ev) {
                // only try to select connection when no dragging is active
                if (!((boolean) viewer.getControl().getData(DRAG_STATE))) {
                    selectConnection(ev);
                }
                viewer.getControl().setData(DRAG_STATE, false);
            }

            @Override
            public void mouseDown(MouseEvent ev) {
                selectConnection(ev);
                mouseX = ev.x;
                mouseY = ev.y;
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
                    OpenConnectionEditorHandler openConnectionEditorHandler =
                        new OpenConnectionEditorHandler(source, target);
                    try {
                        openConnectionEditorHandler.execute(new ExecutionEvent());
                    } catch (ExecutionException e1) {
                        e1.printStackTrace();
                    }
                }
            }

        });
        // Snap to grid and geometry actions, enable geometry automatically.
        getActionRegistry().registerAction(new ToggleGridAction(getGraphicalViewer()));
        getActionRegistry().registerAction(new ToggleSnapToGeometryAction(getGraphicalViewer()));

        getViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, true);
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(WorkflowNodePart.SMALL_WORKFLOW_NODE_WIDTH - 1,
            WorkflowNodePart.SMALL_WORKFLOW_NODE_WIDTH - 1));

        // register activate context for context sensitive key bindings
        IContextService contextService = (IContextService) getSite().getService(IContextService.class);
        contextService.activateContext("de.rcenvironment.rce.gui.workflow.editor.scope");

        // preferences store - initialize labels not to be shown
        IPreferenceStore preferenceStore = Activator.getInstance().getPreferenceStore();
        preferenceStore.setValue(WorkflowEditor.SHOW_LABELS_PREFERENCE_KEY, false);

        // remove unwanted menu entries from workflow editor's context menu
        ContextMenuItemRemover.removeUnwantedMenuEntries(viewer.getControl());

    }

    /**
     * Shows the number of channels for all connections.
     */
    public void showAllConnectionLabels() {
        for (Object connectionPartObject : viewer.getEditPartRegistry().values()) {
            if (connectionPartObject instanceof ConnectionPart) {
                ((ConnectionPart) connectionPartObject).showLabel();
            }
        }
    }

    /**
     * Hides the number of channel for all connections.
     */
    public void hideUnselectedConnectionLabels() {
        for (Object connectionPartObject : viewer.getEditPartRegistry().values()) {
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

    private void removeConnectionColorsAndLabel() {
        for (Object connectionPartObject : viewer.getEditPartRegistry().values()) {
            if (connectionPartObject instanceof ConnectionPart && !viewer.getSelectedEditParts().contains(connectionPartObject)) {
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

    private ConnectionPart selectConnection(MouseEvent ev) {
        for (Object editPart : viewer.getEditPartRegistry().values()) {
            if (editPart instanceof ConnectionPart) {
                int offsetX = ((FigureCanvas) getViewer().getControl()).getViewport().getViewLocation().x;
                int offsetY = ((FigureCanvas) getViewer().getControl()).getViewport().getViewLocation().y;
                ConnectionPart connectionPart = ((ConnectionPart) editPart);
                PointList connectionPoints = connectionPart.getConnectionFigure().getPoints();
                Rectangle toleranceRectangle =
                    new Rectangle(ev.x + offsetX - DEFAULT_TOLERANCE / 2, ev.y + offsetY - DEFAULT_TOLERANCE / 2,
                        DEFAULT_TOLERANCE, DEFAULT_TOLERANCE);
                if (connectionPoints.intersects(toleranceRectangle)) {
                    viewer.select(connectionPart);
                    viewer.reveal(connectionPart);
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

    @Override
    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            @Override
            protected void configurePaletteViewer(final PaletteViewer v) {
                super.configurePaletteViewer(v);
                paletteViewer = v;
                v.getControl().addKeyListener(new KeyAdapter() {

                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.stateMask == SWT.ALT && e.keyCode == CONNECTION_KEYCODE) {
                            switchToConnectionTool();
                        } else if (e.stateMask == SWT.ALT && e.keyCode == SELECTION_KEYCODE) {
                            switchToSelectionTool();
                        }
                    }
                });
                // adds tool integration add, edit and deactivate wizards to palette's context menu
                v.getControl().addMenuDetectListener(new MenuDetectListener() {

                    @Override
                    public void menuDetected(MenuDetectEvent menuDetectEvent) {
                        Menu menu = ((Control) menuDetectEvent.widget).getMenu();
                        // prevents multiple listener registration
                        if (menu.getData(MENU_LISTENER_MARKER) == null) {
                            menu.setData(MENU_LISTENER_MARKER, true);
                            menu.addMenuListener(new MenuAdapter() {

                                @Override
                                public void menuShown(MenuEvent menuEvent) {
                                    Menu menu = (Menu) menuEvent.widget;
                                    boolean foundToolIntegrationPaletteExtension = false;
                                    for (MenuItem item : menu.getItems()) {
                                        if (TOOLINTEGRATION_ITEM.equals(item.getData())) {
                                            foundToolIntegrationPaletteExtension = true;
                                            break;
                                        }
                                    }
                                    if (!foundToolIntegrationPaletteExtension) {
                                        extendPaletteContextMenu(menu);
                                    }
                                }

                                private void extendPaletteContextMenu(Menu menu) {
                                    // add separator
                                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                                    separator.setData(TOOLINTEGRATION_ITEM, true);
                                    // add tool integration menu items
                                    addShowToolIntegrationWizard(menu);
                                    addShowEditToolIntegrationWizard(menu);
                                    addShowDeleteToolIntegrationWizard(menu);
                                }
                            });
                        }
                    }
                });
                v.getControl().addMouseListener(new MouseAdapter() {

                    private final int offset = 20;

                    @Override
                    public void mouseDoubleClick(MouseEvent e) {
                        if (v.getActiveTool().getLabel().equals(WorkflowLabel.PALETTE_ENTRY_NAME)) {
                            WorkflowLabel label = new WorkflowLabel(WorkflowLabel.INITIAL_TEXT);
                            WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
                            // Proper size is set within the command
                            Rectangle rectangle = new Rectangle(TILE_OFFSET, TILE_OFFSET, MINUS_ONE, MINUS_ONE);
                            WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(label, model, rectangle);
                            WorkflowNodeLabelConnectionCreateCommand createCommand = helper.createCommand();
                            getEditorsCommandStack().execute(createCommand);
                            getExistingPaletteGroups();
                            v.setActiveTool(v.getPaletteRoot().getDefaultEntry());
                        }
                        PlatformService platformService = serviceRegistryAccess.getService(PlatformService.class);
                        NodeIdentifier localNode = platformService.getLocalNodeId();
                        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
                        Collection<ComponentInstallation> installations = getInitialComponentKnowledge().getAllInstallations();
                        installations = ComponentUtils.eliminateComponentInterfaceDuplicates(installations, localNode);

                        ComponentInstallation installation = null;
                        for (ComponentInstallation inst : installations) {
                            String name = (inst.getComponentRevision().getComponentInterface().getDisplayName());
                            if (inst.getComponentRevision().getComponentInterface().getVersion() != null
                                && toolIntegrationRegistry.hasId(inst.getComponentRevision().getComponentInterface().getIdentifier())) {
                                name = name
                                    + StringUtils.format(COMPONENTNAMES_WITH_VERSION, inst.getComponentRevision().getComponentInterface()
                                        .getVersion());
                            }
                            if (name.equals(v.getActiveTool().getLabel())) {
                                installation = inst;
                                break;
                            }
                        }

                        // Set bounds of new component, if intersects with existing translate by
                        // 30,30
                        Rectangle rectangle = new Rectangle(TILE_OFFSET, TILE_OFFSET, TILE_SIZE, TILE_SIZE);
                        for (WorkflowNode node : model.getWorkflowNodes()) {
                            Rectangle nodeRect = new Rectangle(node.getX(), node.getY(), TILE_SIZE, TILE_SIZE);
                            if (nodeRect.intersects(rectangle)) {
                                rectangle.translate(offset, offset);
                            }
                        }

                        // if tool was found, add it
                        if (installation != null) {
                            ComponentDescription description = new ComponentDescription(installation);
                            description.initializeWithDefaults();
                            WorkflowNode node = new WorkflowNode(description);
                            WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(node, model, rectangle);
                            WorkflowNodeLabelConnectionCreateCommand createCommand = helper.createCommand();
                            getEditorsCommandStack().execute(createCommand);
                            // activate properties tab for added node
                            for (Object editpart : viewer.getContents().getChildren()) {
                                if (editpart instanceof EditPart && editpart instanceof WorkflowNodePart) {
                                    EditPart currentEditPart = (EditPart) editpart;
                                    if (((WorkflowNode) currentEditPart.getModel()).equals(node)) {
                                        viewer.select(currentEditPart);
                                        tabbedPropertySheetPage.selectionChanged(WorkflowEditor.this, viewer.getSelection());
                                        break;
                                    }
                                }
                            }
                            // set active tool to "select" when double click was performed.
                            // getExistingPaletteGroups() has to be called, for whatever reason
                            getExistingPaletteGroups();
                            v.setActiveTool(v.getPaletteRoot().getDefaultEntry());
                        }
                    }
                });
                v.addDragSourceListener(new TemplateTransferDragSourceListener(v));
            }
        };

    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalControl().setVisible(false);
    }

    @Override
    protected void setInput(final IEditorInput input) {
        super.setInput(input);

        String name = null;
        if (input instanceof FileEditorInput) {
            FileEditorInput fileEditorInput = (FileEditorInput) input;
            workspaceFile = fileEditorInput.getFile();
            name = workspaceFile.getName();
        } else if (input instanceof FileStoreEditorInput) {
            FileStoreEditorInput fileStoreEditorInput = (FileStoreEditorInput) input;
            inputFile = new File(fileStoreEditorInput.getURI());
            name = inputFile.getName();
        }
        workflowDescription = new WorkflowDescription("");
        setPartName(name);

        Job job = new Job(Messages.openWorkflow) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.loadingComponents, 2);
                    monitor.worked(1);
                    workflowDescription = LoadingWorkflowDescriptionHelper.loadWorkflowDescription(inputFile, workspaceFile, true);
                    initializeWorkflowDescriptionListener();
                    monitor.worked(1);

                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            if (viewer.getControl() != null) {
                                viewer.setContents(workflowDescription);
                                getGraphicalControl().setVisible(true);
                                setFocus();
                                firePropertyChange(PROP_FINAL_WORKFLOW_DESCRIPTION_SET);
                            }
                        }
                    });
                } catch (RuntimeException e) {
                    // caught and only logged as an error dialog already pops up if an error occur
                    LogFactory.getLog(getClass()).error("Failed to load workflow: "
                        + LoadingWorkflowDescriptionHelper.getNameOfWorkflowFile(inputFile, workspaceFile), e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            };
        };
        job.setUser(true);
        job.schedule();
    }

    /**
     * Makes the editor listen to changes in the underlying {@link WorkflowDescription}.
     */
    private void initializeWorkflowDescriptionListener() {
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
                WorkflowDescriptionPersistenceHandler wdHandler = new WorkflowDescriptionPersistenceHandler();
                file.setContents(
                    new ByteArrayInputStream(wdHandler.writeWorkflowDescriptionToStream(workflowDescription).toByteArray()),
                    true, // keep saving, even if IFile is out of sync with the Workspace
                    false, // dont keep history
                    monitor); // progress monitor
            } else if (getEditorInput() instanceof FileStoreEditorInput) {
                File file = new File(((FileStoreEditorInput) getEditorInput()).getURI().getPath().replaceFirst("/", ""));
                WorkflowDescriptionPersistenceHandler wdHandler = new WorkflowDescriptionPersistenceHandler();
                ByteArrayOutputStream outStream = wdHandler.writeWorkflowDescriptionToStream(workflowDescription);
                FileUtils.writeByteArrayToFile(file, outStream.toByteArray());
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

    private void validateWorkflow() {
        List<?> list = viewer.getRootEditPart().getChildren();
        for (Object object : list) {
            if (object instanceof WorkflowPart) {
                WorkflowPart workflowPart = (WorkflowPart) object;
                List<?> children = workflowPart.getChildren();
                for (Object child : children) {
                    if (child instanceof WorkflowNodePart) {
                        WorkflowNodePart workflowNodePart = (WorkflowNodePart) child;
                        WorkflowNode workflowNode = (WorkflowNode) workflowNodePart.getModel();
                        if (!workflowNode.isValid()) {
                            workflowNodePart.verifyValid();
                        }
                    }
                }
            }
        }
        firePropertyChange(PROP_WORKFLOW_VAILDATION_FINISHED);
    }

    @Override
    public void doSaveAs() {
        FileDialog fd = new FileDialog(new Shell(), SWT.SAVE);
        fd.setText("Save As...");
        String[] filterExt = { "*.wf" };
        fd.setFilterExtensions(filterExt);
        fd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
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
            getCommandStack().markSaveLocation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (OutOfMemoryError error) {
            if (file != null) {
                file.deleteOnExit();
            }
            showMemoryExceedingWarningMessage();
            error.printStackTrace();
        }
        // set the dirty state relative to the current command stack position
        getCommandStack().markSaveLocation();
    }

    private void showMemoryExceedingWarningMessage() {
        MessageBox messageBox = new MessageBox(viewer.getControl().getShell(), SWT.ICON_WARNING | SWT.OK);
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
            return new WorkflowEditorHelpContextProvider(viewer);
        }
        return super.getAdapter(type);
    }

    @Override
    protected FlyoutPreferences getPalettePreferences() {
        FlyoutPreferences prefs = super.getPalettePreferences();
        prefs.setPaletteState(FlyoutPaletteComposite.STATE_PINNED_OPEN);
        return prefs;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    public GraphicalViewer getViewer() {
        return viewer;
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * @param display
     */
    private void registerChangeListeners() {
        serviceRegistryAccess.registerService(DistributedComponentKnowledgeListener.class, this);
    }

    private List<String> getExistingPaletteGroups() {
        List<String> paletteGroups = new ArrayList<String>();
        for (Object child : paletteRoot.getChildren()) {
            if (child instanceof PaletteDrawer) {
                paletteGroups.add(((PaletteDrawer) child).getLabel());
            }
        }
        return paletteGroups;
    }

    private List<String> getExistingPaletteEntries() {
        List<String> paletteEntries = new ArrayList<String>();
        for (Object child : paletteRoot.getChildren()) {
            if (child instanceof PaletteDrawer) {
                for (Object innerChild : ((PaletteDrawer) child).getChildren()) {
                    if (innerChild instanceof PaletteEntry) {
                        paletteEntries.add(((PaletteEntry) innerChild)
                            .getLabel());
                    }
                }
            }
        }
        return paletteEntries;
    }

    private List<String> getExistingComponentNames(Collection<ComponentInstallation> cis) {
        List<String> existingComponentNames = new ArrayList<String>();
        for (ComponentInstallation installation : cis) {
            String name = installation.getComponentRevision().getComponentInterface().getDisplayName();
            if (installation.getComponentRevision().getComponentInterface().getVersion() != null
                && toolIntegrationRegistry.hasId(installation.getComponentRevision().getComponentInterface().getIdentifier())) {
                name = name
                    + StringUtils.format(COMPONENTNAMES_WITH_VERSION, installation.getComponentRevision().getComponentInterface()
                        .getVersion());
            }
            existingComponentNames.add(name);
        }
        return existingComponentNames;
    }

    private CombinedTemplateCreationEntry createPaletteEntry(ComponentInstallation installation) {
        // set the default platform of the ComponendDescription
        // to null
        ComponentInterface componentInterface = installation.getComponentRevision().getComponentInterface();
        // prepare the icon of the component
        ImageDescriptor image = null;
        byte[] icon = componentInterface.getIcon16();
        if (icon != null) {
            try {
                image = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), new ByteArrayInputStream(icon)));
            } catch (SWTException e) { // see https://www.sistec.dlr.de/mantis/view.php?id=11453
                if (e.getMessage().contains("Invalid image")) {
                    LogFactory.getLog(getClass()).debug("Setting component icon failed. Use default icon as fallback", e);
                    image = Activator.getInstance().getImageRegistry().getDescriptor(Activator.IMAGE_RCE_ICON_16);
                } else {
                    throw e;
                }
            }
        } else {
            image = Activator.getInstance().getImageRegistry().getDescriptor(Activator.IMAGE_RCE_ICON_16);
        }
        String name = componentInterface.getDisplayName();
        if (componentInterface.getVersion() != null
            && toolIntegrationRegistry.hasId(componentInterface.getIdentifier())) {
            name = name + StringUtils.format(COMPONENTNAMES_WITH_VERSION, componentInterface.getVersion());
        }
        // create the palette entry
        CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(name, name,
            new WorkflowNodeFactory(installation), image, image);

        return component;
    }

    private void addComponentToGroup(CombinedTemplateCreationEntry component, String groupLabel) {
        for (Object group : paletteRoot.getChildren()) {
            if (group instanceof PaletteDrawer) {
                if (((PaletteDrawer) group).getLabel().equals(groupLabel)) {
                    ((PaletteDrawer) group).add(getIndexForComponentToAdd((PaletteDrawer) group, component.getLabel()), component);
                }
            }
        }
    }

    private int getIndexForComponentToAdd(PaletteDrawer group, String componentLabel) {
        int index = 0;
        for (Object child : group.getChildren()) {
            if (child instanceof PaletteEntry) {
                if (((PaletteEntry) child).getLabel().compareToIgnoreCase(componentLabel) > 0) {
                    return index;
                }
            }
            index++;
        }
        return index;
    }

    private PaletteDrawer createPaletteDrawer(String groupLabel) {
        PaletteDrawer group = new PaletteDrawer(groupLabel);
        group.setDescription(groupLabel);
        paletteRoot.add(getIndexForGroupToAdd(groupLabel), group);
        return group;
    }

    private int getIndexForGroupToAdd(String groupLabel) {
        int index = 0;

        // if group starts with underscore, append it to the end.
        if (groupLabel.startsWith("_")) {
            return paletteRoot.getChildren().size();
        }

        for (Object group : paletteRoot.getChildren()) {
            if (group instanceof PaletteDrawer) {

                if (((PaletteDrawer) group).getLabel().compareToIgnoreCase(groupLabel) > 0) {
                    return index;
                }
            }
            index++;
        }
        return index;
    }

    private void removeComponentFromGroup(PaletteDrawer group, PaletteEntry entry) {
        group.remove(entry);
        if (group.getChildren().size() <= 0) {
            paletteRoot.remove(group);
        }
    }

    private synchronized void refreshPalette(Collection<ComponentInstallation> cis) {
        // create entry lists
        List<String> paletteEntries = getExistingPaletteEntries();
        List<String> paletteGroups = getExistingPaletteGroups();
        List<String> componentNames = getExistingComponentNames(cis);
        // check for every component entry if contained in current palette - if not : add
        for (ComponentInstallation installation : cis) {
            String name = (installation.getComponentRevision().getComponentInterface().getDisplayName());
            if (installation.getComponentRevision().getComponentInterface().getVersion() != null
                && toolIntegrationRegistry.hasId(installation.getComponentRevision().getComponentInterface().getIdentifier())) {
                name = name
                    + StringUtils.format(COMPONENTNAMES_WITH_VERSION, installation.getComponentRevision().getComponentInterface()
                        .getVersion());
            }
            if (!paletteEntries.contains(name)) {
                CombinedTemplateCreationEntry component = createPaletteEntry(installation);
                String group = installation.getComponentRevision().getComponentInterface().getGroupName();
                if (!paletteGroups.contains(group)) {
                    createPaletteDrawer(group);
                    paletteGroups.add(group);
                }
                addComponentToGroup(component, group);
            }
        }

        // check for every palette entry if contained in existing components -
        // if not : remove
        Map<PaletteDrawer, List<PaletteEntry>> componentsToRemove = new HashMap<PaletteDrawer, List<PaletteEntry>>();

        for (Object group : paletteRoot.getChildren()) {
            if (group instanceof PaletteDrawer) {
                for (Object component : ((PaletteDrawer) group).getChildren()) {
                    if (component instanceof PaletteEntry) {
                        if (!componentNames.contains(((PaletteEntry) component).getLabel())) {
                            if (!componentsToRemove.containsKey(group)) {
                                componentsToRemove.put((PaletteDrawer) group, new ArrayList<PaletteEntry>());
                            }
                            componentsToRemove.get(group).add((PaletteEntry) component);
                        }
                    }
                }
            }
        }

        for (PaletteDrawer group : componentsToRemove.keySet()) {
            for (PaletteEntry entry : componentsToRemove.get(group)) {
                removeComponentFromGroup(group, entry);
            }
        }
    }

    @Override
    public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
        final Collection<ComponentInstallation> cis = newState.getAllInstallations();
        if (PlatformUI.isWorkbenchRunning() && !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    refreshPalette(cis);
                    try {
                        viewer.setContents(workflowDescription);
                    } catch (SWTException e) {
                        //To avoid the error described in https://www.sistec.dlr.de/mantis/view.php?id=12228
                        LogFactory.getLog(getClass()).debug("Caught SWTException, probably the affected widget is already disposed.");
                    }
                }
            });
        }
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    // removed as it causes NPE on start up, if connection is not established, when workflow is opened under some circumstances
    // @Override
    // public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
    //
    // final Collection<ComponentInstallation> componentInstallations = newState.getAllInstallations();
    //
    // refreshWorkflowDescription(componentInstallations);
    //
    // if (PlatformUI.isWorkbenchRunning() && !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
    //
    // PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
    //
    // @Override
    // public void run() {
    // viewer.setContents(workflowDescription);
    // WorkflowPart wfPart = (WorkflowPart) viewer.getRootEditPart().getChildren().get(0);
    // EditPart contents = wfPart.getViewer().getContents();
    // for (Object o : contents.getChildren()) {
    // ((WorkflowNodePart) o).refresh();
    // }
    // refreshPalette(componentInstallations);
    // }
    // });
    // }
    // }
    //
    // // should be moved to non-gui code
    // private void refreshWorkflowDescription(Collection<ComponentInstallation> componentInstallations) {
    // for (WorkflowNode workflowNode : workflowDescription.getWorkflowNodes()) {
    // ComponentInstallation componentInstallation = workflowNode.getComponentDescription().getComponentInstallation();
    // ComponentInstallation alternativeComponentInstallation = null;
    // if (componentInstallation.getComponentRevision().getComponentInterface().getIdentifier()
    // .startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
    // alternativeComponentInstallation = ComponentUtils.getComponentInstallation(
    // componentInstallation.getComponentRevision().getComponentInterface()
    // .getIdentifier().replace(ComponentUtils.MISSING_COMPONENT_PREFIX, ""),
    // componentInstallations);
    // } else if (!componentInstallations.contains(componentInstallation)) {
    // // if it is not an user-integrated component (the interface of a user-integrated component might changed, which results in
    // // synchronization issues with the underlying wf file and the workflow description in the editor)
    // if (!toolIntegrationRegistry.hasId(componentInstallation.getComponentRevision()
    // .getComponentInterface().getIdentifier())) {
    // alternativeComponentInstallation = ComponentUtils.getComponentInstallation(
    // componentInstallation.getComponentRevision().getComponentInterface()
    // .getIdentifier(), componentInstallations);
    // }
    // if (alternativeComponentInstallation == null) {
    // ComponentInterface componentInterface = componentInstallation.getComponentRevision().getComponentInterface();
    // alternativeComponentInstallation = ComponentUtils.createPlaceholderComponentInstallation(
    // componentInterface.getIdentifier(),
    // componentInterface.getVersion(),
    // componentInterface.getDisplayName(),
    // componentInstallation.getNodeId());
    // }
    // }
    // if (alternativeComponentInstallation != null) {
    // workflowNode.getComponentDescription()
    // .setComponentInstallation(alternativeComponentInstallation);
    // }
    // }
    // }

    private DistributedComponentKnowledge getInitialComponentKnowledge() {
        DistributedComponentKnowledgeService registry = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class);
        return registry.getCurrentComponentKnowledge();
    }

    private void addShowToolIntegrationWizard(Menu menu) {
        toolIntegrationPaletteMenuItem = new MenuItem(menu, SWT.NONE);
        toolIntegrationPaletteMenuItem.setText(TOOL_INTEGRATE_LABEL);
        toolIntegrationPaletteMenuItem.setData(TOOLINTEGRATION_ITEM, true);
        toolIntegrationPaletteMenuItem.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INTEGRATION_NEW));
        toolIntegrationPaletteMenuItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {

                ShowIntegrationWizardHandler handler = new ShowIntegrationWizardHandler();
                try {
                    handler.execute(new ExecutionEvent());
                } catch (ExecutionException e) {
                    LogFactory.getLog(getClass()).error("Opening Tool Integration wizard failed", e);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                widgetSelected(selectionEvent);
            }
        });
    }

    private void addShowEditToolIntegrationWizard(Menu menu) {
        editToolPaletteMenuItem = new MenuItem(menu, SWT.NONE);
        editToolPaletteMenuItem.setText(TOOL_EDIT_LABEL);
        editToolPaletteMenuItem.setData(TOOLINTEGRATION_ITEM, true);
        editToolPaletteMenuItem.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INTEGRATION_EDIT));
        editToolPaletteMenuItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {

                ShowIntegrationEditWizardHandler handler = new ShowIntegrationEditWizardHandler();
                try {
                    handler.execute(new ExecutionEvent());
                } catch (ExecutionException e) {
                    LogFactory.getLog(getClass()).error("Opening Tool Edit wizard failed", e);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                widgetSelected(selectionEvent);
            }
        });
    }

    private void addShowDeleteToolIntegrationWizard(Menu menu) {
        deactivateToolPaletteMenuItem = new MenuItem(menu, SWT.NONE);
        deactivateToolPaletteMenuItem.setText(TOOL_DEACTIVATE_LABEL);
        deactivateToolPaletteMenuItem.setData(TOOLINTEGRATION_ITEM, true);
        deactivateToolPaletteMenuItem.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INTEGRATION_REMOVE));
        deactivateToolPaletteMenuItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {

                ShowIntegrationRemoveHandler handler = new ShowIntegrationRemoveHandler();
                try {
                    handler.execute(new ExecutionEvent());
                } catch (ExecutionException e) {
                    LogFactory.getLog(getClass()).error("Opening Tool Deactivation wizard failed", e);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                widgetSelected(selectionEvent);
            }
        });
    }

    public boolean isAllLabelsShown() {
        return allLabelsShown;
    }

    public void setAllLabelsShown(boolean allLabelsShown) {
        this.allLabelsShown = allLabelsShown;
    }

    /**
     * Key listener that implements some shortcuts for the workflow editor.
     * 
     * @author Oliver Seebach
     */
    private final class WorkflowEditorKeyListener implements KeyListener {

        @Override
        public void keyReleased(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.stateMask == SWT.ALT && e.keyCode == CONNECTION_KEYCODE) {
                switchToConnectionTool();
            } else if (e.stateMask == SWT.ALT && e.keyCode == SELECTION_KEYCODE) {
                switchToSelectionTool();
            } else if (e.stateMask == SWT.ALT && e.keyCode == OPEN_CONNECTION_VIEW_KEYCODE) {
                openConnectionEditor();
            }
        }
    }

}
