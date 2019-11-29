/*
 * Copyright 2006-2019 DLR, Germany
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentImageContainerService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInstallationBuilder;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessageStore;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.gui.utils.incubator.ContextMenuItemRemover;
import de.rcenvironment.core.gui.wizards.toolintegration.ShowIntegrationEditWizardHandler;
import de.rcenvironment.core.gui.wizards.toolintegration.ShowIntegrationRemoveHandler;
import de.rcenvironment.core.gui.wizards.toolintegration.ShowIntegrationWizardHandler;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.GUIWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.gui.workflow.WorkflowNodeLabelConnectionHelper;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeDeleteCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeLabelConnectionCreateCommand;
import de.rcenvironment.core.gui.workflow.editor.documentation.ToolIntegrationDocumentationGUIHelper;
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
 */
public class WorkflowEditor extends GraphicalEditorWithFlyoutPalette
    implements ITabbedPropertySheetPageContributor, DistributedComponentKnowledgeListener {

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

    private static final int MOVEMENT = 1;

    private static final String REMOTEACCESS = "remoteaccess";

    private static final Log LOGGER = LogFactory.getLog(WorkflowEditor.class);

    private static final char SELECTION_KEYCODE = 's'; // for ALT + s as shortcut

    private static final char CONNECTION_KEYCODE = 'd'; // for ALT + d as shortcut

    private static final char OPEN_CONNECTION_VIEW_KEYCODE = 'c'; // for ALT + c as shortcut

    private static final String MENU_LISTENER_MARKER = "MENU_LISTENER_MARKER";

    private static final String TOOL_DEACTIVATE_LABEL = "Deactivate Tool...";

    private static final String TOOL_EDIT_LABEL = "Edit Tool...";

    private static final String TOOL_INTEGRATE_LABEL = "Integrate Tool...";

    private static final String TOOLINTEGRATION_ITEM = "toolIntegrationItem";

    private static final int MINUS_ONE = -1;

    private static final int UNDO_LIMIT = 10;

    private static final int TILE_OFFSET = 30;

    private static final int TILE_SIZE = 60;

    private static final int MAXIMUM_LOCAL_COMPONENT_LOADING_WAIT_SECONDS = 20;

    protected final ServiceRegistryPublisherAccess serviceRegistryAccess;

    protected final WorkflowExecutionService workflowExecutionService;

    protected final LocalComponentRegistrationService localComponentRegistrationService;

    protected WorkflowDescription workflowDescription;

    private boolean allLabelsShown = false;

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    private GraphicalViewer viewer;

    private PaletteRoot paletteRoot;

    private MenuItem toolIntegrationPaletteMenuItem;

    private MenuItem editToolPaletteMenuItem;

    private MenuItem deactivateToolPaletteMenuItem;

    private MenuItem documentationToolPaletteMenuItem;

    private PaletteViewer paletteViewer = null;

    private ConnectionCreationToolEntry connectionCreationToolEntry = null;

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

    public PaletteViewer getPaletteViewer() {
        return paletteViewer;
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
        final List<DistributedComponentEntry> componentInstallations = new ArrayList<>();
        IProgressService service = (IProgressService) PlatformUI.getWorkbench().getService(IProgressService.class);
        try {
            service.run(false, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
        WorkflowScalableFreeformRootEditPart rootEditPart = new WorkflowScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        viewer.setEditPartFactory(new WorkflowEditorEditPartFactory());

        getCommandStack().setUndoLimit(UNDO_LIMIT);

        viewer.getControl().addKeyListener(new WorkflowEditorKeyListener());

        viewer.setContents(workflowDescription);

        viewer.addDropTargetListener(new TemplateTransferDropTargetListener(viewer));

        ContextMenuProvider cmProvider = new WorkflowEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);

        WorkflowZoomManager zoomManager = (WorkflowZoomManager) rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(zoomManager));
        getActionRegistry().registerAction(new ZoomOutAction(zoomManager));
        viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
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
                    if (structuredSelectionObject instanceof WorkflowNodePart) {
                        WorkflowNodePart nodePart = (WorkflowNodePart) structuredSelectionObject;
                        ComponentInterface ci = ((WorkflowNode) nodePart.getModel()).getComponentDescription()
                            .getComponentInstallation().getComponentInterface();
                        String id = ci.getIdentifierAndVersion().substring(0,
                            ci.getIdentifierAndVersion().lastIndexOf(ComponentConstants.ID_SEPARATOR));
                        if (toolIntegrationRegistry.hasTIContextMatchingPrefix(id)) {
                            PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(),
                                ToolIntegrationConstants.CONTEXTUAL_HELP_PLACEHOLDER_ID);
                        } else {
                            PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), id);
                        }
                    } else {
                        PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), null);
                    }
                }
                removeConnectionColorsAndLabel();
            }
        });

        viewer.getControl().setData(DRAG_STATE_BENDPOINT, false);

        viewer.getControl().addMouseListener(new MouseListener() {

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

        });

        // Avoid graphical artifacts during loading from workflow file.
        getGraphicalControl().setVisible(true);

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
        ContextMenuItemRemover.removeUnwantedMenuEntries(viewer.getControl());
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);

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
            if (connectionPartObject instanceof ConnectionPart
                && !viewer.getSelectedEditParts().contains(connectionPartObject)) {
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
                Rectangle toleranceRectangle = new Rectangle(ev.x + offsetX - DEFAULT_TOLERANCE / 2,
                    ev.y + offsetY - DEFAULT_TOLERANCE / 2, DEFAULT_TOLERANCE, DEFAULT_TOLERANCE);
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
                // adds tool integration add, edit and deactivate wizards to palette's context
                // menu
                v.getControl().addMenuDetectListener(new MenuDetectListener() {

                    @Override
                    public void menuDetected(MenuDetectEvent menuDetectEvent) {
                        final Menu menu = ((Control) menuDetectEvent.widget).getMenu();
                        // prevents multiple listener registration
                        if (menu.getData(MENU_LISTENER_MARKER) == null) {
                            menu.setData(MENU_LISTENER_MARKER, true);
                            menu.addMenuListener(new MenuAdapter() {

                                @Override
                                public void menuShown(MenuEvent menuEvent) {
                                    Object selection = ((StructuredSelection) v.getSelection()).getFirstElement();
                                    if (selection instanceof EditPart) {
                                        String toolName = ((PaletteEntry) ((EditPart) selection).getModel())
                                            .getDescription();
                                        // Context menu for integrated tools
                                        if (toolName != null && getSelectedPaletteComponent(toolName) != null
                                            && getSelectedPaletteComponent(toolName).getComponentInterface().getIdentifier().matches(
                                                ToolIntegrationConstants.CONTEXTUAL_HELP_PLACEHOLDER_ID)) {
                                            extendPaletteContextMenu(menu,
                                                getSelectedPaletteComponent(toolName).getComponentInterface().getIdentifierAndVersion());
                                        }
                                        // Context menu for SSH remote access tools
                                        if (toolName != null && getSelectedPaletteComponent(toolName) != null
                                            && getSelectedPaletteComponent(toolName).getComponentInterface().getIdentifierAndVersion()
                                                .matches(
                                                    "de.rcenvironment.remoteaccess.*")) {
                                            extendPaletteContextMenuForSshComponent(menu,
                                                getSelectedPaletteComponent(toolName).getComponentInterface().getIdentifierAndVersion());
                                        }
                                    }
                                }

                                private void extendPaletteContextMenu(Menu menu, String toolID) {
                                    // add separator
                                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                                    separator.setData(TOOLINTEGRATION_ITEM, true);
                                    // add tool integration menu items
                                    addShowToolIntegrationWizard(menu);
                                    addShowEditToolIntegrationWizard(menu);
                                    addShowDeleteToolIntegrationWizard(menu);
                                    addGetDocumentation(menu, toolID);
                                    addExportDocumentation(menu, toolID);
                                }

                                private void extendPaletteContextMenuForSshComponent(Menu menu, String toolID) {
                                    // add separator
                                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                                    separator.setData(TOOLINTEGRATION_ITEM, true);
                                    // add ssh tool menu items
                                    addGetDocumentation(menu, toolID);
                                    addExportDocumentation(menu, toolID);
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
                            WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(label,
                                model, rectangle);
                            WorkflowNodeLabelConnectionCreateCommand createCommand = helper.createCommand();
                            getEditorsCommandStack().execute(createCommand);
                            getExistingPaletteGroups();
                            v.setActiveTool(v.getPaletteRoot().getDefaultEntry());
                        }
                        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
                        ComponentInstallation installation = getSelectedPaletteComponent(v.getActiveTool().getLabel());

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
                            WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(node,
                                model, rectangle);
                            WorkflowNodeLabelConnectionCreateCommand createCommand = helper.createCommand();
                            getEditorsCommandStack().execute(createCommand);
                            // activate properties tab for added node
                            for (Object editpart : viewer.getContents().getChildren()) {
                                if (editpart instanceof EditPart && editpart instanceof WorkflowNodePart) {
                                    EditPart currentEditPart = (EditPart) editpart;
                                    if (((WorkflowNode) currentEditPart.getModel()).equals(node)) {
                                        viewer.select(currentEditPart);
                                        tabbedPropertySheetPage.selectionChanged(WorkflowEditor.this,
                                            viewer.getSelection());
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

    private ComponentInstallation getSelectedPaletteComponent(final String label) {
        PlatformService platformService = serviceRegistryAccess.getService(PlatformService.class);
        LogicalNodeId localNode = platformService.getLocalDefaultLogicalNodeId();
        Collection<DistributedComponentEntry> installations = getInitialComponentKnowledge().getAllInstallations();
        installations = ComponentUtils.eliminateComponentInterfaceDuplicates(installations, localNode);

        ComponentInstallation installation = null;
        for (DistributedComponentEntry entry : installations) {
            ComponentInstallation inst = entry.getComponentInstallation();
            String name = (inst.getComponentInterface().getDisplayName());
            if (inst.getComponentInterface().getVersion() != null
                && (toolIntegrationRegistry.hasTIContextMatchingPrefix(inst.getComponentInterface().getIdentifierAndVersion())
                    || inst.getComponentInterface().getIdentifierAndVersion()
                        .startsWith(ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + REMOTEACCESS))) {
                name = name
                    + StringUtils.format(COMPONENTNAMES_WITH_VERSION, inst.getComponentInterface().getVersion());
            }
            if (name.equals(label)) {
                installation = inst;
                break;
            }
        }
        return installation;
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalControl().setVisible(false);
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
                                if (viewer.getControl() != null) {
                                    viewer.setContents(workflowDescription);
                                    getGraphicalControl().setVisible(true);
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

    private void validateWorkflow() {
        ComponentValidationMessageStore.getInstance().emptyMessageStore(); // delete any previous validation errors

        List<?> list = viewer.getRootEditPart().getChildren();
        WorkflowDescriptionValidationUtils.validateWorkflowDescription(workflowDescription, false, true);
        for (Object object : list) {
            if (object instanceof WorkflowPart) {
                WorkflowPart workflowPart = (WorkflowPart) object;
                List<?> children = workflowPart.getChildren();
                for (Object child : children) {
                    if (child instanceof WorkflowNodePart) {
                        WorkflowNodePart workflowNodePart = (WorkflowNodePart) child;
                        if (!((WorkflowNode) workflowNodePart.getModel()).isValid()) {
                            workflowNodePart.updateValid();
                        }
                    }
                }
            }
        }
        firePropertyChange(PROP_WORKFLOW_VAILDATION_FINISHED);
    }

    private void cleanNewDescriptionOfDisabledAndNotAvailableNodes(WorkflowDescription newWorkflowDescription) {
        Set<WorkflowNode> nodesToDelete = new HashSet<>();
        nodesToDelete.addAll(getDisabledNodes(newWorkflowDescription));
        nodesToDelete.addAll(getNotAvailableNodes(newWorkflowDescription));
        if (!nodesToDelete.isEmpty()) {
            new WorkflowNodeDeleteCommand(newWorkflowDescription, new ArrayList<WorkflowNode>(nodesToDelete)).execute();
        }
    }

    private List<WorkflowNode> getInvalidWorkflowNodes(WorkflowDescription newWorkflowDescription) {
        List<WorkflowNode> result = new ArrayList<>();
        for (WorkflowNode node : newWorkflowDescription.getWorkflowNodes()) {
            if (!node.isValid()) {
                result.add(node);
            }
        }
        return result;
    }

    private List<WorkflowNode> getNotAvailableNodes(WorkflowDescription newWorkflowDescription) {
        List<WorkflowNode> result = new ArrayList<>();
        for (WorkflowNode node : newWorkflowDescription.getWorkflowNodes()) {
            if (!isNodeAvailable(node)) {
                result.add(node);
            }
        }
        return result;
    }

    private List<WorkflowNode> getDisabledNodes(WorkflowDescription newWorkflowDescription) {
        List<WorkflowNode> result = new ArrayList<>();
        for (WorkflowNode node : newWorkflowDescription.getWorkflowNodes()) {
            if (!node.isEnabled()) {
                result.add(node);
            }
        }
        return result;
    }

    private boolean isNodeAvailable(WorkflowNode node) {
        return !node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX);
    }

    private void markTargetsOfInvalidOrDisabledOrNotAvailableNodesInvalid(WorkflowDescription newWorkflowDescription) {
        Set<WorkflowNode> nodesOfInterest = new HashSet<>();
        nodesOfInterest.addAll(getInvalidWorkflowNodes(newWorkflowDescription));
        nodesOfInterest.addAll(getNotAvailableNodes(newWorkflowDescription));
        nodesOfInterest.addAll(getDisabledNodes(newWorkflowDescription));

        for (WorkflowNode node : nodesOfInterest) {
            for (Connection connection : newWorkflowDescription.getConnections()) {
                if (connection.getSourceNode().equals(node) && connection.getTargetNode().isEnabled()
                    && !nodesOfInterest.contains(connection.getTargetNode())) {
                    EndpointDescription inputEp = connection.getInput();

                    EndpointDefinition.InputExecutionContraint exeConstraint = inputEp.getEndpointDefinition()
                        .getDefaultInputExecutionConstraint();
                    if (inputEp.getMetaDataValue(
                        ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null) {
                        exeConstraint = EndpointDefinition.InputExecutionContraint.valueOf(inputEp
                            .getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT));
                    }

                    if (exeConstraint.equals(EndpointDefinition.InputExecutionContraint.Required)) {
                        newWorkflowDescription.getWorkflowNode(connection.getTargetNode().getIdentifierAsObject())
                            .setValid(false);
                        // this triggers a visual update of the component in the editor later on
                        workflowDescription.getWorkflowNode(connection.getTargetNode().getIdentifierAsObject())
                            .setValid(false);
                    }
                }
            }
        }
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
        } else if (type == IContentOutlinePage.class) {
            return new OutlineView(getGraphicalViewer());
        } else if (type == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
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
                        paletteEntries.add(((PaletteEntry) innerChild).getLabel());
                    }
                }
            }
        }
        return paletteEntries;
    }

    private List<String> getExistingComponentNames(Collection<DistributedComponentEntry> entries) {
        List<String> existingComponentNames = new ArrayList<String>();
        for (DistributedComponentEntry entry : entries) {
            ComponentInstallation installation = entry.getComponentInstallation();
            String name = installation.getComponentInterface().getDisplayName();
            if (installation.getComponentInterface().getVersion() != null
                && (toolIntegrationRegistry.hasTIContextMatchingPrefix(installation.getComponentInterface().getIdentifierAndVersion())
                    || installation.getComponentInterface().getIdentifierAndVersion()
                        .startsWith(ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "remoteaccess"))) {
                name = name + StringUtils.format(COMPONENTNAMES_WITH_VERSION,
                    installation.getComponentInterface().getVersion());
            }
            existingComponentNames.add(name);
        }
        return existingComponentNames;
    }

    private CombinedTemplateCreationEntry createPaletteEntry(ComponentInstallation installation) {
        // set the default platform of the ComponendDescription
        // to null
        ComponentInterface componentInterface = installation.getComponentInterface();
        // prepare the icon of the component
        ImageDescriptor imageDescriptor = null;
        Image image = ServiceRegistry.createAccessFor(this).getService(ComponentImageContainerService.class)
            .getComponentImageContainer(componentInterface).getComponentIcon16();
        imageDescriptor = ImageDescriptor.createFromImage(image);

        String name = componentInterface.getDisplayName();
        if (componentInterface.getVersion() != null
            && (toolIntegrationRegistry.hasTIContextMatchingPrefix(componentInterface.getIdentifierAndVersion())
                || componentInterface.getIdentifierAndVersion()
                    .startsWith(ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + REMOTEACCESS))) {
            name = name + StringUtils.format(COMPONENTNAMES_WITH_VERSION, componentInterface.getVersion());
        }
        // create the palette entry
        CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(name, name,
            new WorkflowNodeFactory(installation), imageDescriptor, imageDescriptor);

        return component;
    }

    private void addComponentToGroup(CombinedTemplateCreationEntry component, String groupLabel) {
        for (Object group : paletteRoot.getChildren()) {
            if (group instanceof PaletteDrawer) {
                if (((PaletteDrawer) group).getLabel().equals(groupLabel)) {
                    ((PaletteDrawer) group).add(getIndexForComponentToAdd((PaletteDrawer) group, component.getLabel()),
                        component);
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
        group.setInitialState(PaletteDrawer.INITIAL_STATE_CLOSED);
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

    private synchronized void refreshPalette(Collection<DistributedComponentEntry> entries) {
        // create entry lists
        List<String> componentNames = getExistingComponentNames(entries);
        // getting the componentImageContaineService
        ComponentImageContainerService componentImageContainerService =
            ServiceRegistry.createAccessFor(this).getService(ComponentImageContainerService.class);

        // order: remove entries, add entries and update icons; reduces count of icon calls
        // the new order will not change the behavior if a component is "changed" by unpublish and publish but allow icon updates if the
        // "change" does not affect the name

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
        
       //create list with the palette state at the moment of execution 
        List<String> paletteEntries = getExistingPaletteEntries();
        List<String> paletteGroups = getExistingPaletteGroups();

        // check for every component entry if contained in current palette - if not :
        // add
        for (DistributedComponentEntry entry : entries) {
            ComponentInstallation installation = entry.getComponentInstallation();
            String name = (installation.getComponentInterface().getDisplayName());
            if (installation.getComponentInterface().getVersion() != null
                && (toolIntegrationRegistry.hasTIContextMatchingPrefix(installation.getComponentInterface().getIdentifierAndVersion())
                    || installation.getComponentInterface().getIdentifierAndVersion()
                        .startsWith(ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + REMOTEACCESS))) {
                name = name + StringUtils.format(COMPONENTNAMES_WITH_VERSION,
                    installation.getComponentInterface().getVersion());
            }
            if (!paletteEntries.contains(name)) {
                String group = installation.getComponentInterface().getGroupName();
                CombinedTemplateCreationEntry component = createPaletteEntry(installation);
                paletteEntries.add(name);
                if (!paletteGroups.contains(group)) {
                    createPaletteDrawer(group);
                    paletteGroups.add(group);
                }
                addComponentToGroup(component, group);
            } else {
                // if the component is not new, the component has to be already there, so the icon will be updated
                for (Object group : paletteRoot.getChildren()) {
                    if (group instanceof PaletteDrawer) {
                        for (Object component : ((PaletteDrawer) group).getChildren()) {
                            if (component instanceof PaletteEntry && ((PaletteEntry) component).getLabel().equals(name)) {
                                ImageDescriptor descriptor = ImageDescriptor.createFromImage(componentImageContainerService
                                    .getComponentImageContainer(entry.getComponentInterface()).getComponentIcon16());
                                ((PaletteEntry) component).setLargeIcon(descriptor);
                                ((PaletteEntry) component).setSmallIcon(descriptor);

                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge newState) {
        final Collection<DistributedComponentEntry> cis = newState.getAllInstallations();
        if (PlatformUI.isWorkbenchRunning() && !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (viewer != null && viewer.getControl() != null && !viewer.getControl().isDisposed()) {
                        refreshPalette(cis);
                        // TODO review: Seems not to be necessary to set the viewers content at this
                        // place and causes the editor to refresh
                        // the view (https://mantis.sc.dlr.de/view.php?id=14697). Was added in revision
                        // 18983 for unknown reasons.
                        // flink 2016/11/04
                        // viewer.setContents(workflowDescription);
                    } else {
                        LogFactory.getLog(getClass()).warn("Got callback (onDistributedComponentKnowledgeChanged)"
                            + " but widget(s) already disposed; the listener might not be disposed properly");
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

    // removed as it causes NPE on start up, if connection is not established, when
    // workflow is
    // opened under some circumstances
    // @Override
    // public void
    // onDistributedComponentKnowledgeChanged(DistributedComponentKnowledge
    // newState) {
    //
    // final Collection<ComponentInstallation> componentInstallations =
    // newState.getAllInstallations();
    //
    // refreshWorkflowDescription(componentInstallations);
    //
    // if (PlatformUI.isWorkbenchRunning() &&
    // !PlatformUI.getWorkbench().getDisplay().isDisposed())
    // {
    //
    // PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
    //
    // @Override
    // public void run() {
    // viewer.setContents(workflowDescription);
    // WorkflowPart wfPart = (WorkflowPart)
    // viewer.getRootEditPart().getChildren().get(0);
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
    // private void refreshWorkflowDescription(Collection<ComponentInstallation>
    // componentInstallations) {
    // for (WorkflowNode workflowNode : workflowDescription.getWorkflowNodes()) {
    // ComponentInstallation componentInstallation =
    // workflowNode.getComponentDescription().getComponentInstallation();
    // ComponentInstallation alternativeComponentInstallation = null;
    // if (componentInstallation.getComponentInterface().getIdentifier()
    // .startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
    // alternativeComponentInstallation = ComponentUtils.getComponentInstallation(
    // componentInstallation.getComponentInterface()
    // .getIdentifier().replace(ComponentUtils.MISSING_COMPONENT_PREFIX, ""),
    // componentInstallations);
    // } else if (!componentInstallations.contains(componentInstallation)) {
    // // if it is not an user-integrated component (the interface of a
    // user-integrated component
    // might changed, which results in
    // // synchronization issues with the underlying wf file and the workflow
    // description in the
    // editor)
    // if
    // (!toolIntegrationRegistry.hasId(componentInstallation.getComponentRevision()
    // .getComponentInterface().getIdentifier())) {
    // alternativeComponentInstallation = ComponentUtils.getComponentInstallation(
    // componentInstallation.getComponentInterface()
    // .getIdentifier(), componentInstallations);
    // }
    // if (alternativeComponentInstallation == null) {
    // ComponentInterface componentInterface =
    // componentInstallation.getComponentInterface();
    // alternativeComponentInstallation =
    // ComponentUtils.createPlaceholderComponentInstallation(
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
        DistributedComponentKnowledgeService registry = serviceRegistryAccess
            .getService(DistributedComponentKnowledgeService.class);
        return registry.getCurrentSnapshot();
    }

    private void addShowToolIntegrationWizard(Menu menu) {
        toolIntegrationPaletteMenuItem = new MenuItem(menu, SWT.NONE);
        toolIntegrationPaletteMenuItem.setText(TOOL_INTEGRATE_LABEL);
        toolIntegrationPaletteMenuItem.setData(TOOLINTEGRATION_ITEM, true);
        toolIntegrationPaletteMenuItem
            .setImage(ImageManager.getInstance().getSharedImage(StandardImages.INTEGRATION_NEW));
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
        deactivateToolPaletteMenuItem
            .setImage(ImageManager.getInstance().getSharedImage(StandardImages.INTEGRATION_REMOVE));
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

    private void addGetDocumentation(Menu menu, final String toolID) {
        documentationToolPaletteMenuItem = new MenuItem(menu, SWT.NONE);
        documentationToolPaletteMenuItem.setText("Open Documentation");
        documentationToolPaletteMenuItem.setData(TOOLINTEGRATION_ITEM, true);

        documentationToolPaletteMenuItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                ToolIntegrationDocumentationGUIHelper.getInstance().showComponentDocumentation(toolID, false);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                widgetSelected(selectionEvent);
            }
        });
    }

    private void addExportDocumentation(Menu menu, final String toolID) {
        documentationToolPaletteMenuItem = new MenuItem(menu, SWT.NONE);
        documentationToolPaletteMenuItem.setText("Export Documentation");
        documentationToolPaletteMenuItem.setData(TOOLINTEGRATION_ITEM, true);

        documentationToolPaletteMenuItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                ToolIntegrationDocumentationGUIHelper.getInstance().showComponentDocumentation(toolID, true);
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
     * @author Jascha Riedel(#0013977) moved selection and connection tool to ui.bindings
     */
    private final class WorkflowEditorKeyListener implements KeyListener {

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
            file.getWorkspace().removeResourceChangeListener(resourceListener);
        }

        super.setInput(input);

        if (getEditorInput() != null) {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().addResourceChangeListener(resourceListener);
            setPartName(file.getName());
        }
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
