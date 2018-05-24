/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeUtils;
import de.rcenvironment.core.gui.datamanagement.commons.DataManagementWorkbenchUtils;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.gui.workflow.view.timeline.TimelineView;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * A visual tree-based navigator for data represented in the RCE data management.
 * 
 * @author Markus Litz
 * @author Robert Mischke
 * @author Jan Flink
 * @author Doreen Seider
 */
public class DataManagementBrowser extends ViewPart implements DMBrowserNodeContentAvailabilityHandler {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "de.rcenvironment.rce.gui.datamanagement.browser.DataManagementBrowser";

    private static final String ROOT_NODE_TITLE = "<root>";

    private static final String NODE_TEXT_FORMAT_TITLE_PLUS_HOSTNAME = "%s %s";

    private static final String BRACKET_OPEN = "[";

    private static final String BRACKET_CLOSE = "]";

    private static final String LOCAL = "local";

    private static final MetaData META_DATA_WORKFLOW_FINAL_STATE = new MetaData(MetaDataKeys.WORKFLOW_FINAL_STATE, true, true);

    private static final MetaData METADATA_WORKFLOW_FILES_DELETED = new MetaData(
        MetaDataKeys.WORKFLOW_FILES_DELETED, true, true);

    private static final MetaData METADATA_WORKFLOW_IS_MARKED_FOR_DELETION = new MetaData(
        MetaDataKeys.WORKFLOW_MARKED_FOR_DELETION, true, true);

    private static final List<DMBrowserNodeType> SAVABLE_NODE_TYPES = new ArrayList<>();

    private static final List<DMBrowserNodeType> SAVE_AS_FOLDER_NODE_TYPES = new ArrayList<>();

    private static final List<DMBrowserNodeType> DELETABLE_NODE_TYPES = new ArrayList<>();

    private static final List<DMBrowserNodeType> REFRESHABLE_NODE_TYPES = new ArrayList<>();

    private static final List<DMBrowserNodeType> COMPARABLE_NODE_TYPES = new ArrayList<>();

    private static final List<DMBrowserNodeType> OPEN_IN_EDITOR_NODE_TYPES = new ArrayList<>();

    protected final Log log = LogFactory.getLog(getClass());

    protected DMTreeSorter treeSorter;

    private Object[] visibleExpandedElements = null;

    private TreeViewer viewer;

    private DrillDownAdapter drillDownAdapter;

    private DMContentProvider contentProvider;

    private Action sortAscendingName;

    private Action actionRefreshAll;

    private Action openInEditorAction;

    private Action openTiglAction;

    private Action doubleClickAction;

    private Action deleteNodeAction;

    private Action deleteFilesAction;

    private Action exportNodeToFileSystemAction;

    private Action exportNodeToProjectAction;

    private RefreshNodeAction refreshNodeAction;

    private CollapseAllNodesAction collapseAllNodesAction;

    /**
     * FileDataService for storing/loading resources to the data management.
     */
    private FileDataService fileDataService;

    private IAction sortDescendingName;

    private IAction sortTimestampAsc;

    private Action compareAction;

    private Action timelineAction;

    private Action copyAction;

    private Action sortTimestampDesc;

    private ServiceRegistryAccess serviceRegistryAccess;

    static {
        /*
         * Set all savable DMBrowserNodeTypes.
         */
        // Set all DMBrowserNodeTypes which are to save as folder.
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.Timeline);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.Components);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.Component);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.HistoryObject);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.Input);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.Output);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.IntermediateInputsFolder);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.LogFolder);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.ToolInputOutputFolder);
        SAVE_AS_FOLDER_NODE_TYPES.add(DMBrowserNodeType.DMDirectoryReference);
        // Set all savable DMBrowserNodeTypes.
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.HistoryRoot);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.DMFileResource);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.Resource);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.Float);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.Vector);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.ShortText);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.Boolean);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.Integer);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.SmallTable);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.Indefinite);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.File);
        SAVABLE_NODE_TYPES.add(DMBrowserNodeType.CommonText);

        /*
         * Set all deletable DMBrowserNodeTypes.
         */
        DELETABLE_NODE_TYPES.add(DMBrowserNodeType.Workflow);

        /*
         * Whitelist: Nodes which can be refreshed.
         */
        REFRESHABLE_NODE_TYPES.add(DMBrowserNodeType.Workflow);
        REFRESHABLE_NODE_TYPES.add(DMBrowserNodeType.Timeline);
        REFRESHABLE_NODE_TYPES.add(DMBrowserNodeType.Components);
        REFRESHABLE_NODE_TYPES.add(DMBrowserNodeType.WorkflowRunInformation);

        /*
         * Set all comparable DMBrowserNodeTypes.
         */
        COMPARABLE_NODE_TYPES.add(DMBrowserNodeType.DMFileResource);

        /*
         * Node types that can be opened in an editor
         */
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.Boolean);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.CommonText);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.DMFileResource);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.File);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.Float);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.Integer);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.ShortText);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.SmallTable);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.Matrix);
        OPEN_IN_EDITOR_NODE_TYPES.add(DMBrowserNodeType.Vector);

    }

    /**
     * Export locations.
     *
     * @author Oliver Seebach
     */
    private enum ExportType {
        FILESYSTEM, PROJECT;
    }

    /**
     * An {@link Action} to export data management entries to local files.
     * 
     * @author Christian Weiss
     * 
     */
    private final class CustomExportAction extends SelectionProviderAction {

        private final List<DMBrowserNode> selectedNodes = new LinkedList<>();

        private Display display;

        private ExportType exportType;

        private CustomExportAction(ISelectionProvider provider, String text, ExportType exportType) {
            super(provider, text);
            setEnabled(false);
            this.exportType = exportType;
        }

        @Override
        public void selectionChanged(final IStructuredSelection selection) {
            // clear the old selection
            selectedNodes.clear();
            // the 'save' action is only enabled, if a DataService is
            // connected to delegate the save request to and the
            // selected is not empty
            boolean enabled = fileDataService != null && !selection.isEmpty();
            if (enabled) {
                @SuppressWarnings("unchecked") final Iterator<DMBrowserNode> iter = selection.iterator();
                while (iter.hasNext()) {
                    DMBrowserNode selectedNode = iter.next();
                    DMBrowserNodeType nodeType = selectedNode.getType();
                    if (selectedNode.isEnabled() && !selectedNode.areAllChildrenDisabled()
                        && (SAVABLE_NODE_TYPES.contains(nodeType) || SAVE_AS_FOLDER_NODE_TYPES.contains(nodeType))) {
                        selectedNodes.add(selectedNode);
                    } else {
                        enabled = false;
                    }
                }
                // action is only enabled if a potential content node is
                // selected
                enabled &= mightHaveContent(selectedNodes);
                // store the Display to show the DirectoryDialog in 'run'
                display = Display.getCurrent();
            }
            setEnabled(enabled);
        }

        @Override
        public void run() {
            final List<DMBrowserNode> browserNodesToSave = new LinkedList<>(selectedNodes);
            FileDialog fileDialog = new FileDialog(display.getActiveShell(), SWT.SAVE);
            if (exportType == ExportType.PROJECT) {
                String exportLocationDefaultPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath();
                IViewPart projectExplorerView = getProjectExplorer();
                if (projectExplorerView != null) {
                    TreeSelection treeSelection = (TreeSelection) projectExplorerView.getViewSite().getSelectionProvider().getSelection();
                    if (treeSelection.size() == 1) {
                        if (treeSelection.getFirstElement() instanceof org.eclipse.core.internal.resources.File) {
                            // take absolute path of parent folder of file
                            exportLocationDefaultPath = ((org.eclipse.core.internal.resources.File) treeSelection.getFirstElement())
                                .getParent().getLocation().toFile().getAbsolutePath();
                        } else if (treeSelection.getFirstElement() instanceof Folder) {
                            // take absolute path of folder itself
                            exportLocationDefaultPath = ((Folder) treeSelection.getFirstElement()).getLocation().toFile().getAbsolutePath();
                        } else if (treeSelection.getFirstElement() instanceof Project) {
                            // take absolute path of project itself
                            exportLocationDefaultPath =
                                ((Project) treeSelection.getFirstElement()).getLocation().toFile().getAbsolutePath();
                        }
                    }
                }
                fileDialog.setFilterPath(exportLocationDefaultPath);
                fileDialog.setText("Export to selected folder...");
            } else {
                fileDialog.setText("Export to file system...");
            }
            fileDialog.setFileName(browserNodesToSave.get(0).getTitle().replace(":", "_"));
            final String directoryPath = fileDialog.open();
            if (directoryPath == null) {
                return;
            }
            final File targetDirectory = new File(directoryPath);
            final ExportJob job = new ExportJob("Exporting", browserNodesToSave, targetDirectory);
            job.addJobChangeListener(new IJobChangeListener() {

                @Override
                public void done(IJobChangeEvent event) {
                    if (event.getResult() == Status.OK_STATUS) {
                        display.syncExec(new Runnable() {

                            @Override
                            public void run() {
                                final String location;
                                if (job.getTargetFile() != null) {
                                    location = StringUtils.format(Messages.exportLocationText,
                                        job.getTargetFile().getAbsolutePath());
                                } else {
                                    location = StringUtils.format(Messages.exportLocationText,
                                        targetDirectory.getAbsolutePath());
                                }
                                if (exportType == ExportType.PROJECT) {
                                    try {
                                        ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
                                    } catch (CoreException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                new CustomPopupDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                                    "Workflow Data Browser\nData export", StringUtils.format(Messages.exportSuccessText,
                                        browserNodesToSave.toString().replace(BRACKET_OPEN, "").replace(BRACKET_CLOSE, "")),
                                    location).open();
                            }
                        });
                    } else if (event.getResult() == Status.CANCEL_STATUS) {
                        display.syncExec(new Runnable() {

                            @Override
                            public void run() {
                                MessageDialog.openError(display.getActiveShell(), "Error",
                                    Messages.exportErrorText);
                            }
                        });
                    }
                }

                @Override
                public void awake(IJobChangeEvent arg0) {}

                @Override
                public void aboutToRun(IJobChangeEvent arg0) {}

                @Override
                public void sleeping(IJobChangeEvent arg0) {}

                @Override
                public void scheduled(IJobChangeEvent arg0) {}

                @Override
                public void running(IJobChangeEvent arg0) {}
            });
            job.setUser(true);
            job.schedule();
        }

        private IViewPart getProjectExplorer() {
            IWorkbenchWindow[] workbenchs = PlatformUI.getWorkbench().getWorkbenchWindows();
            for (IWorkbenchWindow workbench : workbenchs) {
                for (IWorkbenchPage page : workbench.getPages()) {
                    IViewPart projectExplorerView = page.findView("org.eclipse.ui.navigator.ProjectExplorer");
                    if (projectExplorerView != null) {
                        return projectExplorerView;
                    }
                }
            }
            return null;
        }

        /**
         * An eclipse-style read-only info popup. The popup closes automatically after the DISPLAY_TIME.
         *
         * @author Jan Flink
         */
        private final class CustomPopupDialog extends PopupDialog {

            private static final int OFFSET = 15;

            private static final int DISPLAY_TIME = 10000;

            private final MouseListener mouseListener = new MouseListener() {

                @Override
                public void mouseUp(MouseEvent arg0) {
                    close();
                }

                @Override
                public void mouseDown(MouseEvent arg0) {
                    // Nothing to do here.
                }

                @Override
                public void mouseDoubleClick(MouseEvent arg0) {
                    // Nothing to do here.

                }
            };

            private String messageText;

            private String titleText;

            CustomPopupDialog(Shell parent, String titleText, String messageText, String infoText) {
                super(parent, PopupDialog.INFOPOPUP_SHELLSTYLE, false, false, false, false, false, titleText, infoText);
                this.titleText = titleText;
                this.messageText = messageText;
                display.timerExec(DISPLAY_TIME, new Runnable() {

                    @Override
                    public void run() {
                        CustomPopupDialog.this.close();
                    }
                });
            }

            @Override
            protected Control createDialogArea(Composite parent) {
                Label infoText = new Label(parent, SWT.SINGLE);
                infoText.setText(messageText);
                return infoText;
            }

            @Override
            protected Control createTitleControl(Composite parent) {
                Composite c = new Composite(parent, SWT.NONE);
                GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).numColumns(2).applyTo(c);
                Label titleLabel = new Label(c, SWT.NONE);
                if (titleText != null) {
                    titleLabel.setText(titleText);
                }
                Label closeLabel = new Label(parent, SWT.NONE);
                closeLabel.setText("X");
                closeLabel.setAlignment(SWT.END);

                Font font = titleLabel.getFont();
                FontData[] fontDatas = font.getFontData();
                for (int i = 0; i < fontDatas.length; i++) {
                    fontDatas[i].setStyle(SWT.BOLD);
                }
                Font titleFont = new Font(titleLabel.getDisplay(), fontDatas);
                titleLabel.setFont(titleFont);
                closeLabel.setFont(titleFont);

                GridDataFactory.fillDefaults().indent(0, 0).applyTo(c);
                GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, false).indent(0, 0).applyTo(c);
                GridDataFactory.fillDefaults().align(SWT.END, SWT.TOP).grab(true, false).indent(0, 0).applyTo(closeLabel);

                closeLabel.addMouseListener(mouseListener);
                return c;
            }

            @Override
            protected Point getInitialLocation(Point initialSize) {
                Rectangle bounds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBounds();
                return new Point(bounds.width + bounds.x - this.getDefaultSize().x - OFFSET,
                    bounds.height + bounds.y - this.getDefaultSize().y - OFFSET);
            }

        }

        /**
         * A {@link Job} to handle the exporting of data management entries to local files.
         */
        private final class ExportJob extends Job {

            private static final String DOT = ".";

            private List<DMBrowserNode> browserNodesToSave;

            private File targetDirectory;

            private File targetFile;

            protected ExportJob(String title, List<DMBrowserNode> browserNodesToSave, File ordnerPath) {
                super(title);
                this.browserNodesToSave = browserNodesToSave;
                this.targetDirectory = ordnerPath;
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                // delete the nodes recursively
                monitor.beginTask(StringUtils.format("Exporting %d node(s): %s",
                    browserNodesToSave.size(),
                    browserNodesToSave.toString()).replace(BRACKET_OPEN, "").replace(BRACKET_CLOSE, ""), 2);
                monitor.worked(1);
                for (final DMBrowserNode browserNodeToSave : browserNodesToSave) {
                    DMBrowserNode workflowNode = browserNodeToSave.getNodeWithTypeWorkflow();
                    if (workflowNode.getWorkflowHostName().equals(LOCAL)
                        || isWorkflowHostReachable(workflowNode.getNodeIdentifier())) {
                        saveNode(browserNodeToSave);
                    } else {
                        return Status.CANCEL_STATUS;
                    }
                }
                monitor.worked(1);
                return Status.OK_STATUS;
            }

            protected File getTargetFile() {
                return targetFile;
            }

            private void saveNode(final DMBrowserNode browserNode) {
                if (SAVE_AS_FOLDER_NODE_TYPES.contains(browserNode.getType())) {
                    if (!targetDirectory.exists()) {
                        targetDirectory.mkdir();
                    }
                    if (!browserNode.areChildrenKnown()) {
                        contentProvider.fetchChildren(browserNode);
                    }
                    for (final DMBrowserNode child : contentProvider.getChildren(browserNode)) {
                        if (child.isEnabled()) {
                            save(child, targetDirectory);
                        }
                    }
                } else {
                    String fileName = targetDirectory.getName();
                    targetDirectory = new File(targetDirectory.getAbsolutePath().replace(File.separator + fileName, ""));
                    if (!fileName.contains(DOT)) {
                        final String[] fileEnding = browserNode.getAssociatedFilename().split(Pattern.quote(DOT));
                        if (fileEnding.length == 2) {
                            fileName = fileName + DOT + fileEnding[1];
                        }
                    }
                    targetFile = findUniqueFilename(targetDirectory, fileName);
                    if (browserNode.isEnabled()) {
                        save(browserNode.getDataReferenceId(), browserNode.getFileReferencePath(), targetFile.getName(), targetDirectory,
                            browserNode.getNodeWithTypeWorkflow().getNodeIdentifier());
                    }
                }
            }

            private void save(final DMBrowserNode browserNode, final File directory) {
                // get the current DataReference and delete it, if it is
                // not null (null DataReferences are used for
                // aggregating tree items)
                final String dataReferenceId = browserNode
                    .getDataReferenceId();
                final String fileReferencePath = browserNode.getFileReferencePath();
                String filename = browserNode.getAssociatedFilename();
                if (filename == null) {
                    filename = browserNode.getTitle();
                }
                filename = filename.replaceAll(
                    "[^-\\s\\(\\)._a-zA-Z0-9]", "_").trim();
                final File nodeFile = findUniqueFilename(directory,
                    filename);
                if (!browserNode.areChildrenKnown()) {
                    contentProvider.fetchChildren(browserNode);
                }
                if (browserNode.getNumChildren() > 0) {
                    nodeFile.mkdir();
                    // save children
                    for (final DMBrowserNode child : contentProvider.getChildren(browserNode)) {
                        // recur
                        if (child.isEnabled()) {
                            save(child, nodeFile);
                        }
                    }
                } else {
                    if (browserNode.isEnabled() && dataReferenceId != null || fileReferencePath != null) {
                        save(dataReferenceId, fileReferencePath, nodeFile.getName(), directory,
                            browserNode.getNodeWithTypeWorkflow().getNodeIdentifier());
                    }
                }
            }

            private File findUniqueFilename(final File directory,
                final String filename) {
                File result = new File(directory, filename);
                if (!result.exists()) {
                    return result;
                }
                String prefix = filename;
                String postfix = "";
                final Pattern pattern = Pattern
                    .compile("^(.*)\\.([a-zA-Z0-9]+)$");
                final Matcher matcher = pattern.matcher(filename);
                if (matcher.matches()) {
                    prefix = matcher.group(1);
                    postfix = DOT + matcher.group(2);
                }
                int i = 0;
                do {
                    ++i;
                    result = new File(directory, StringUtils.format(
                        "%s (%d)%s", prefix, i, postfix));
                } while (result.exists());
                return result;
            }

            private void save(final String dataReferenceId, final String fileReferencePath, final String filename,
                final File directory, ResolvableNodeId rceNodeIdentifier) {
                try {
                    DataManagementWorkbenchUtils.getInstance().saveReferenceToFile(dataReferenceId, fileReferencePath,
                        new File(directory, filename).getAbsolutePath(), rceNodeIdentifier);
                } catch (NullPointerException | AuthorizationException | IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }

    /**
     * An {@link Action} that opens the data associated with the selected node with the TiGL Viewer.
     */
    private final class OpenInTiglAction extends SelectionProviderAction {

        private final String[] supportedFileExtensions = new String[] { "xml", "brep", "step", "stp", "iges", "igs", "stl", "mesh" };

        private OpenInTiglAction(ISelectionProvider provider, String text) {
            super(provider, text);
        }

        @Override
        public void selectionChanged(IStructuredSelection selection) {
            Object obj = selection.getFirstElement();
            if (selection.size() == 1 && obj instanceof DMBrowserNode) {
                DMBrowserNode node = (DMBrowserNode) obj;

                if (node.isEnabled() && node.getType() == DMBrowserNodeType.DMFileResource
                    && Arrays.asList(supportedFileExtensions)
                        .contains(FilenameUtils.getExtension(node.getAssociatedFilename()))) {
                    setEnabled(true);
                    return;
                }
            }
            setEnabled(false);
        }

        @Override
        public void run() {
            if (!isEnabled()) {
                return;
            }
            ISelection selection = viewer.getSelection();
            Object object = ((IStructuredSelection) selection).getFirstElement();
            if (object instanceof DMBrowserNode) {
                DMBrowserNode node = (DMBrowserNode) object;
                // if node type is workflow or directory prevent opening in viewer
                if (node.getType() != DMBrowserNodeType.Workflow && node.getType() != DMBrowserNodeType.DMDirectoryReference) {
                    String dataReferenceId = node.getDataReferenceId();
                    String associatedFilename = node.getAssociatedFilename();
                    String fileReferencePath = node.getFileReferencePath();

                    if (associatedFilename == null) {
                        associatedFilename = "default";
                    }

                    // try to open in Viewer
                    DataManagementWorkbenchUtils.getInstance().tryOpenDataReferenceInReadonlyEditor(dataReferenceId, fileReferencePath,
                        associatedFilename, node.getNodeWithTypeWorkflow().getNodeIdentifier(), true);
                    // ok -> return
                    return;
                }
            }
        }
    }

    /**
     * An {@link Action} to delete data management entries.
     * 
     * @author Christian Weiss
     * 
     */
    private final class CustomDeleteAction extends SelectionProviderAction {

        private final List<DMBrowserNode> selectedNodes = new LinkedList<>();

        private Display display;

        private boolean hasNotFinishedWorkflows;

        private boolean isFileAction;

        private CustomDeleteAction(ISelectionProvider provider, String text, boolean isFileAction) {
            super(provider, text);
            this.isFileAction = isFileAction;
        }

        @Override
        public void selectionChanged(final IStructuredSelection selection) {
            // clear the old selection
            selectedNodes.clear();
            boolean enabled = fileDataService != null && !selection.isEmpty();
            hasNotFinishedWorkflows = false;

            if (enabled) {
                @SuppressWarnings("unchecked") final Iterator<DMBrowserNode> iter = selection.iterator();
                while (iter.hasNext()) {
                    DMBrowserNode selectedNode = iter.next();
                    if (DELETABLE_NODE_TYPES.contains(selectedNode.getType())) {
                        boolean hasfinalState =
                            selectedNode.getMetaData() != null
                                && selectedNode.getMetaData().getValue(META_DATA_WORKFLOW_FINAL_STATE) != null;
                        boolean hasDataReferences =
                            !Boolean.parseBoolean(selectedNode.getMetaData().getValue(METADATA_WORKFLOW_FILES_DELETED));
                        boolean isMarkedForDeletion =
                            Boolean.parseBoolean(selectedNode.getMetaData().getValue(METADATA_WORKFLOW_IS_MARKED_FOR_DELETION));
                        if (hasfinalState && !isMarkedForDeletion && (hasDataReferences || !isFileAction)) {
                            selectedNodes.add(selectedNode);
                        }
                        hasNotFinishedWorkflows |= !hasfinalState;
                    } else {
                        enabled = false;
                    }
                }
                enabled &= !selectedNodes.isEmpty();

                // store the Display to refresh the tree viewer in 'run'
                display = Display.getCurrent();
            }
            setEnabled(enabled);
        }

        @Override
        public void run() {
            final List<DMBrowserNode> browserNodesToDelete = new LinkedList<>(
                selectedNodes);
            String jobTitle;
            if (isFileAction) {
                jobTitle = StringUtils.format(Messages.jobTitleDeleteFiles,
                    browserNodesToDelete.size(),
                    browserNodesToDelete.toString());
            } else {
                jobTitle = StringUtils.format(Messages.jobTitleDelete,
                    browserNodesToDelete.size(),
                    browserNodesToDelete.toString());
            }

            final Job job = new Job(jobTitle) {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    for (final DMBrowserNode browserNodeToDelete : browserNodesToDelete) {
                        // get the parent node of the node to update the tree
                        // and remove the deleted child node
                        final DMBrowserNode parentNode = browserNodeToDelete
                            .getParent();
                        if (!isFileAction) {
                            boolean deleted = deleteWorkflowRun(browserNodeToDelete);
                            if (deleted && parentNode != null) {
                                parentNode.removeChild(browserNodeToDelete);
                            }
                        } else {
                            deleteFiles(browserNodeToDelete);
                            setEnabled(false);
                            display.syncExec(new Runnable() {

                                @Override
                                public void run() {
                                    refresh(browserNodeToDelete);
                                }
                            });
                        }
                    }
                    if (!isFileAction) {
                        // update the tree
                        display.syncExec(new Runnable() {

                            @Override
                            public void run() {
                                refresh();
                            }
                        });
                    }
                    // return OK as status
                    return Status.OK_STATUS;
                }

                private void deleteFiles(DMBrowserNode browserNode) {
                    contentProvider.deleteWorkflowRunFiles(browserNode);
                }

                private boolean deleteWorkflowRun(final DMBrowserNode browserNode) {
                    return contentProvider.deleteWorkflowRun(browserNode);
                }

            };
            // job is a UI task
            job.setUser(true);
            boolean schedule = true;
            final Shell shell = Display.getCurrent().getActiveShell();
            String dialogMessage;
            String dialogTitle;
            if (!isFileAction) {
                dialogTitle = Messages.dialogTitleDelete;
                if (hasNotFinishedWorkflows) {
                    dialogMessage = Messages.dialogMessageDeleteWithNotDeletableNodes;
                } else {
                    dialogMessage = Messages.dialogMessageDelete;
                }
            } else {
                dialogTitle = Messages.dialogTitleDeleteFiles;
                if (hasNotFinishedWorkflows) {
                    dialogMessage = Messages.dialogMessageDeleteFilesWithNotDeletableNodes;
                } else {
                    dialogMessage = Messages.dialogMessageDeleteFiles;
                }
            }
            if (!MessageDialog.openConfirm(shell, dialogTitle, dialogMessage)) {
                schedule = false;
            }
            if (schedule) {
                job.schedule();
            }
        }
    }

    /**
     * An {@link Action} that opens the data associated with the selected node in a read-only editor.
     */
    private final class OpenInEditorAction extends SelectionProviderAction {

        private OpenInEditorAction(ISelectionProvider provider, String text) {
            super(provider, text);
        }

        @Override
        public void selectionChanged(IStructuredSelection selection) {
            Object obj = selection.getFirstElement();
            if (selection.size() == 1 && obj instanceof DMBrowserNode) {
                DMBrowserNode node = (DMBrowserNode) obj;

                if (node.isEnabled()
                    && OPEN_IN_EDITOR_NODE_TYPES.contains(node.getType())) {
                    setEnabled(true);
                    return;
                }
            }
            setEnabled(false);
        }

        @Override
        public void run() {
            if (!isEnabled()) {
                return;
            }
            ISelection selection = viewer.getSelection();
            Object object = ((IStructuredSelection) selection).getFirstElement();
            if (object instanceof DMBrowserNode) {
                DMBrowserNode node = (DMBrowserNode) object;
                // if node type is workflow or directory prevent opening in text editor
                if (node.getType() != DMBrowserNodeType.Workflow && node.getType() != DMBrowserNodeType.DMDirectoryReference) {
                    String dataReferenceId = node.getDataReferenceId();
                    String associatedFilename = node.getAssociatedFilename();
                    String fileReferencePath = node.getFileReferencePath();

                    if (associatedFilename == null) {
                        associatedFilename = "default";
                    }

                    // try to open in editor
                    DataManagementWorkbenchUtils.getInstance().tryOpenDataReferenceInReadonlyEditor(dataReferenceId, fileReferencePath,
                        associatedFilename, node.getNodeWithTypeWorkflow().getNodeIdentifier(), false);
                    // ok -> return
                    return;
                }
            }
        }
    }

    /**
     * An {@link Action} that triggers a refresh of the selected node.
     * 
     */
    private final class RefreshNodeAction extends SelectionProviderAction {

        private final List<DMBrowserNode> selectedNodes = new LinkedList<>();

        private RefreshNodeAction(ISelectionProvider provider, String text) {
            super(provider, text);
        }

        @Override
        public void selectionChanged(final IStructuredSelection selection) {

            boolean enabled = !selection.isEmpty();
            selectedNodes.clear();
            // refresh node, if only one is selected
            if (enabled) {
                // clear the old selection
                @SuppressWarnings("unchecked") final Iterator<DMBrowserNode> iter = selection.iterator();
                while (iter.hasNext()) {
                    DMBrowserNode selectedNode = iter.next();
                    if (REFRESHABLE_NODE_TYPES.contains(selectedNode.getType())) {
                        selectedNodes.add(selectedNode);
                    } else {
                        enabled = false;
                    }

                }
                enabled &= !selectedNodes.isEmpty();
            }
            setEnabled(enabled);
        }

        @Override
        public void run() {
            for (final DMBrowserNode node : selectedNodes) {
                refresh(node);
            }
        }
    }

    /**
     * An {@link Action} that opens the timeline view for a workflow.
     * 
     */
    private final class OpenTimelineViewAction extends SelectionProviderAction {

        private DMBrowserNode nodeSelected;

        private OpenTimelineViewAction(ISelectionProvider provider, String text) {
            super(provider, text);
        }

        @Override
        public void selectionChanged(IStructuredSelection selection) {
            Object obj = selection.getFirstElement();
            if (selection.size() == 1 && obj instanceof DMBrowserNode) {
                nodeSelected = (DMBrowserNode) obj;
                if (nodeSelected.getType() == DMBrowserNodeType.Workflow
                    || nodeSelected.getType() == DMBrowserNodeType.WorkflowRunInformation
                    || nodeSelected.getType() == DMBrowserNodeType.Timeline
                    || nodeSelected.getType() == DMBrowserNodeType.Components) {
                    setEnabled(true);
                    return;
                }
            }
            setEnabled(false);
        }

        @Override
        public void run() {

            try {
                final IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
                    "de.rcenvironment.gui.Timeline", nodeSelected.getNodeWithTypeWorkflow().getWorkflowID(),
                    IWorkbenchPage.VIEW_ACTIVATE);
                ((TimelineView) view).initialize(Long.parseLong(nodeSelected.getNodeWithTypeWorkflow().getWorkflowID()),
                    nodeSelected.getNodeWithTypeWorkflow().getWorkflowControllerNode());
            } catch (PartInitException e) {
                log.error("Failed to open timeline view for workflow: " + nodeSelected.getName(), e);
            }
        }

    }

    /**
     * An {@link Action} that copies the title of the workflow node if it is of type DMBrowserNodeType.InformationText.
     * 
     */
    private final class CopyNodeTitleAction extends SelectionProviderAction {

        private DMBrowserNode nodeSelected;

        private CopyNodeTitleAction(ISelectionProvider provider, String text) {
            super(provider, text);
        }

        @Override
        public void selectionChanged(IStructuredSelection selection) {
            Object obj = selection.getFirstElement();
            if (selection.size() == 1 && obj instanceof DMBrowserNode) {
                nodeSelected = (DMBrowserNode) obj;
                if (nodeSelected.getType() == DMBrowserNodeType.InformationText) {
                    setEnabled(true);
                    return;
                }
            }
            setEnabled(false);
        }

        @Override
        public void run() {
            String nodeTitle = nodeSelected.getTitle();
            if (nodeTitle.matches(".*: .*")) {
                nodeTitle = nodeTitle.split(": ")[1];
            }

            ClipboardHelper.setContent(nodeTitle);
        }

    }

    /**
     * An {@link Action} that collapses all nodes.
     * 
     * @author Christian Weiss
     */
    private final class CollapseAllNodesAction extends Action {

        CollapseAllNodesAction(String text) {
            super(text);
        }

        @Override
        public void run() {
            viewer.collapseAll();
            visibleExpandedElements = viewer.getVisibleExpandedElements();
        }

    }

    /**
     * An {@link Action} that triggers the different sort actions.
     * 
     */
    private final class CustomSortAction extends SelectionProviderAction {

        private final List<DMBrowserNode> selectedNodes = new LinkedList<>();

        private final int sortingType;

        private CustomSortAction(ISelectionProvider provider, String text, int sortingType) {
            super(provider, text);
            this.sortingType = sortingType;
            setChecked(treeSorter.getSortingType() == sortingType);
        }

        @Override
        public void selectionChanged(final IStructuredSelection selection) {

            // clear the old selection
            selectedNodes.clear();
            boolean enabled = true;
            Object obj = selection.getFirstElement();
            if (obj instanceof DMBrowserNode) {
                DMBrowserNode node = (DMBrowserNode) obj;
                enabled = treeSorter.isSortable(node, sortingType);
                if (enabled) {
                    @SuppressWarnings("unchecked") final Iterator<DMBrowserNode> iter = selection.iterator();
                    while (iter.hasNext()) {
                        DMBrowserNode selectedNode = iter.next();
                        selectedNodes.add(selectedNode);
                    }
                    enabled &= !selectedNodes.isEmpty();
                }
            }
            setEnabled(enabled);
            setChecked((selectedNodes.isEmpty() || containsNodeTypeWorkflow(selectedNodes))
                && treeSorter.getSortingType() == sortingType);
        }

        @Override
        public void run() {
            visibleExpandedElements = viewer.getVisibleExpandedElements();
            if (selectedNodes.isEmpty() || containsNodeTypeWorkflow(selectedNodes)) {
                treeSorter.setSortingType(sortingType);
                treeSorter.enableSorting(true);
                viewer.refresh();
                setSortActionsChecked();
            } else {
                for (DMBrowserNode node : selectedNodes) {
                    if (treeSorter.isSortable(node, sortingType) && node.areChildrenKnown()) {
                        treeSorter.enableSorting(false);
                        setComparator(node);
                        setChecked(false);
                        viewer.refresh(node);
                    }
                }
            }
            viewer.setExpandedElements(visibleExpandedElements);
        }

        private boolean containsNodeTypeWorkflow(List<DMBrowserNode> nodes) {
            for (DMBrowserNode node : nodes) {
                if (node.getType().equals(DMBrowserNodeType.Workflow)) {
                    return true;
                }
            }
            return false;
        }

        private void setSortActionsChecked() {
            sortAscendingName.setChecked(treeSorter.getSortingType() == DMTreeSorter.SORT_BY_NAME_ASC);
            sortDescendingName.setChecked(treeSorter.getSortingType() == DMTreeSorter.SORT_BY_NAME_DESC);
            sortTimestampAsc.setChecked(treeSorter.getSortingType() == DMTreeSorter.SORT_BY_TIMESTAMP);
            sortTimestampDesc.setChecked(treeSorter.getSortingType() == DMTreeSorter.SORT_BY_TIMESTAMP_DESC);

        }

        private void setComparator(DMBrowserNode node) {
            switch (sortingType) {
            case DMTreeSorter.SORT_BY_TIMESTAMP:
                node.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP);
                break;
            case DMTreeSorter.SORT_BY_NAME_ASC:
                node.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_NODE_TITLE);
                break;
            case DMTreeSorter.SORT_BY_NAME_DESC:
                node.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_NODE_TITLE_DESC);
                break;
            case DMTreeSorter.SORT_BY_TIMESTAMP_DESC:
                node.sortChildren(DMBrowserNodeUtils.COMPARATOR_BY_HISTORY_TIMESTAMP_DESC);
                break;
            default:
                break;
            }
        }
    }

    /**
     * An {@link Action} to compare same data typs with each other.
     * 
     * @author Christian Weiss
     * 
     */
    private final class CustomCompareAction extends SelectionProviderAction {

        private DMBrowserNode node;

        private DMBrowserNode node2;

        protected CustomCompareAction(ISelectionProvider provider, String text) {
            super(provider, text);
        }

        @Override
        public void selectionChanged(final IStructuredSelection selection) {
            if (selection.size() == 2) {
                @SuppressWarnings("unchecked") final Iterator<DMBrowserNode> iter = selection.iterator();
                Object obj = iter.next();
                Object obj2 = iter.next();
                if (obj instanceof DMBrowserNode && obj2 instanceof DMBrowserNode) {
                    node = (DMBrowserNode) obj;
                    node2 = (DMBrowserNode) obj2;
                    boolean compareEnabled = false;
                    if (COMPARABLE_NODE_TYPES.contains(node.getType()) && COMPARABLE_NODE_TYPES.contains(node2.getType())
                        && node.getType() == node2.getType()) {
                        compareEnabled = true;
                    }
                    compareAction.setEnabled(compareEnabled);
                }
            } else {
                compareAction.setEnabled(false);
            }
        }

        @Override
        public void run() {
            if (node != null && node2 != null) {
                String dataReferenceId = node.getDataReferenceId();
                String associatedFilename = node.getAssociatedFilename();
                String dataReferenceId2 = node2.getDataReferenceId();
                String associatedFilename2 = node2.getAssociatedFilename();
                if (dataReferenceId == null || dataReferenceId2 == null) {
                    return;
                } else {

                    try {
                        final File left = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(associatedFilename);
                        final File right = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(associatedFilename2);
                        DataManagementService dataManagementService =
                            DataManagementWorkbenchUtils.getInstance().getDataManagementService();
                        dataManagementService.copyReferenceToLocalFile(
                            dataReferenceId, left,
                            node.getNodeWithTypeWorkflow().getNodeIdentifier());
                        dataManagementService.copyReferenceToLocalFile(
                            dataReferenceId2, right,
                            node.getNodeWithTypeWorkflow().getNodeIdentifier());

                        final CompareConfiguration cc = new CompareConfiguration();
                        cc.setLeftLabel(left.getName());
                        cc.setRightLabel(right.getName());
                        CompareUI.openCompareEditor(new FileCompareInput(cc, left, right));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (CommunicationException e) {
                        throw new RuntimeException(StringUtils.format(
                            "Failed to copy data reference from remote node @%s to local file: ",
                            node.getNodeWithTypeWorkflow().getNodeIdentifier())
                            + e.getMessage(), e);

                    }
                }
            }

        }
    }

    /** A {@link KeyListener} to react on pressedkey's. */
    private final class DataManagementKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent event) {
            if (event.stateMask == SWT.CTRL) {
                if (event.keyCode == 'a') {
                    // add shortcut for select all action
                    viewer.getTree().selectAll();
                    getSite().getSelectionProvider().setSelection(viewer.getSelection());
                } else if (event.keyCode == 'c' && copyAction.isEnabled()) {
                    // add shortcut for copy action
                    copyAction.run();
                } else if (event.keyCode == 't' && timelineAction.isEnabled()) {
                    // add shortcut for open timeline action
                    timelineAction.run();
                } else if (event.keyCode == SWT.F5 && refreshNodeAction.isEnabled()) {
                    // add shortcut for refresh selected action
                    refreshNodeAction.run();
                } else if (event.keyCode == 'f' && exportNodeToFileSystemAction.isEnabled()) {
                    // add shortcut for export to file system
                    exportNodeToFileSystemAction.run();
                } else if (event.keyCode == 'g' && exportNodeToProjectAction.isEnabled()) {
                    // add shortcut for export to project
                    exportNodeToProjectAction.run();
                }
            } else if (event.keyCode == SWT.DEL) {
                // add shortcut for delete action
                if (deleteNodeAction.isEnabled()) {
                    deleteNodeAction.run();
                }
            } else if (event.keyCode == SWT.F5) {
                // add shortcut for refresh all action
                if (actionRefreshAll.isEnabled()) {
                    actionRefreshAll.run();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent arg0) {}

    }

    /**
     * The constructor.
     */
    public DataManagementBrowser() {
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * @param display
     */

    private void refresh(final DMBrowserNode node) {
        visibleExpandedElements = viewer.getVisibleExpandedElements();
        DMBrowserNode toRefresh = node.getNodeWithTypeWorkflow();
        if (toRefresh == null) {
            return;
        }
        // clear children of selected node
        toRefresh.clearChildren();
        contentProvider.clear(toRefresh);
        if (viewer.getExpandedState(toRefresh)) {
            viewer.refresh(toRefresh);
        } else {
            // Called in order to update the workflow node's title (update "not terminated yet") even if node is not expanded.
            // viewer.refresh(toRefresh) doesn't do that. Actually, the children don't need to be fetched here, but I didn't find a proper
            // way to just update the node title by using the RetrieverTask of DMContentProvider. I liked to use the RetrieverTask to make
            // sure the "in progress" and error handling are performed as well so that nothing gets broken here. As a user usually expand
            // the node after refresh, fetching the children is not for nothing
            contentProvider.getChildren(toRefresh);
        }
    }

    @Override
    public void createPartControl(Composite parent) {

        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
                Object selectedNode = thisSelection.getFirstElement();
                viewer.setExpandedState(selectedNode, !viewer.getExpandedState(selectedNode));
                visibleExpandedElements = viewer.getVisibleExpandedElements();
            }
        });
        viewer.addTreeListener(new ITreeViewerListener() {

            @Override
            public void treeExpanded(TreeExpansionEvent e) {
                List<Object> elements = new ArrayList<>();
                if (visibleExpandedElements != null) {
                    elements.addAll(Arrays.asList(visibleExpandedElements));
                }
                elements.add(e.getElement());
                visibleExpandedElements = elements.toArray();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent e) {
                List<Object> elements = new ArrayList<>();
                if (visibleExpandedElements != null) {
                    elements.addAll(Arrays.asList(visibleExpandedElements));
                }
                elements.remove(e.getElement());
                visibleExpandedElements = elements.toArray();
            }
        });
        viewer.getControl().addKeyListener(new DataManagementKeyListener());

        drillDownAdapter = new DrillDownAdapter(viewer);
        try {
            contentProvider = new DMContentProvider();
            contentProvider.addContentAvailabilityHandler(this);
            viewer.setContentProvider(contentProvider);
        } catch (AuthenticationException e) {
            // FIXME
            log.error(e);
        }
        ColumnViewerToolTipSupport.enableFor(viewer);
        viewer.setLabelProvider(new DMLabelProvider());
        treeSorter = new DMTreeSorter(DMTreeSorter.SORT_BY_TIMESTAMP_DESC);
        viewer.setSorter(treeSorter);

        getSite().setSelectionProvider(viewer);

        // FIXME: re-enable?
        // Create the help context id for the viewer's control
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(),"FIXME");
        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();

        initialize();
    }

    private void initialize() {
        fileDataService = serviceRegistryAccess.getService(FileDataService.class);
        refresh();
    }

    private DMBrowserNode createRootNode() {
        DMBrowserNode rootNode = new DMBrowserNode(ROOT_NODE_TITLE);
        rootNode.setType(DMBrowserNodeType.HistoryRoot);
        return rootNode;
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                DataManagementBrowser.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(actionRefreshAll);
        manager.add(new Separator());
        manager.add(openInEditorAction);
    }

    private void fillContextMenu(IMenuManager manager) {
        // submenu for sorting
        MenuManager subMenuManager = new MenuManager(Messages.sorting);
        subMenuManager.add(sortAscendingName);
        subMenuManager.add(sortDescendingName);
        subMenuManager.add(sortTimestampAsc);
        subMenuManager.add(sortTimestampDesc);

        MenuManager subMenuManagerExport = new MenuManager("Export");
        subMenuManagerExport.add(exportNodeToFileSystemAction);
        subMenuManagerExport.add(exportNodeToProjectAction);

        manager.add(new Separator());
        manager.add(openInEditorAction);
        if (OS.isFamilyWindows()) {
            manager.add(openTiglAction);
        }
        manager.add(refreshNodeAction);
        manager.add(actionRefreshAll);

        manager.add(new Separator());
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        manager.add(new Separator());
        manager.add(subMenuManager);
        manager.add(new Separator());
        manager.add(copyAction);
        manager.add(new Separator());
        manager.add(subMenuManagerExport);
        manager.add(new Separator());
        manager.add(deleteFilesAction);
        manager.add(deleteNodeAction);
        manager.add(new Separator());
        manager.add(timelineAction);
        manager.add(new Separator());
        manager.add(compareAction);
        manager.add(new Separator());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(sortAscendingName);
        manager.add(sortDescendingName);
        manager.add(sortTimestampAsc);
        manager.add(sortTimestampDesc);
        manager.add(new Separator());
        manager.add(refreshNodeAction);
        manager.add(actionRefreshAll);
        manager.add(collapseAllNodesAction);
        manager.add(deleteNodeAction);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    private void makeActions() {
        final ISelectionProvider selectionProvider = getSite().getSelectionProvider();
        // sort Actions: Ascending Name/Timestamp, Descending Name/Timestamp
        sortAscendingName = new CustomSortAction(selectionProvider, Messages.sortUp, DMTreeSorter.SORT_BY_NAME_ASC);
        sortAscendingName.setImageDescriptor(DMBrowserImages.IMG_SORT_ALPHABETICAL_ASC);
        sortDescendingName = new CustomSortAction(selectionProvider, Messages.sortDown, DMTreeSorter.SORT_BY_NAME_DESC);
        sortDescendingName.setImageDescriptor(DMBrowserImages.IMG_SORT_ALPHABETICAL_DESC);
        sortTimestampAsc = new CustomSortAction(selectionProvider, Messages.sortTime, DMTreeSorter.SORT_BY_TIMESTAMP);
        sortTimestampAsc.setImageDescriptor(DMBrowserImages.IMG_SORT_TIMESTAMP_ASC);
        sortTimestampDesc = new CustomSortAction(selectionProvider, Messages.sortTimeDesc, DMTreeSorter.SORT_BY_TIMESTAMP_DESC);
        sortTimestampDesc.setImageDescriptor(DMBrowserImages.IMG_SORT_TIMESTAMP_DESC);

        compareAction = new CustomCompareAction(selectionProvider, Messages.compareMsg);
        compareAction.setEnabled(false);
        // refresh Actions: refresh a node or refresh all
        makeRefreshActions(selectionProvider);
        // an action to open a selected entry in a read-only editor
        openInEditorAction = new OpenInEditorAction(selectionProvider, "Open in editor (read-only)");
        openInEditorAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.OPEN_READ_ONLY_16));

        openTiglAction = new OpenInTiglAction(selectionProvider, "Open in TiGL Viewer");
        openTiglAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.TIGL_ICON));

        doubleClickAction = openInEditorAction;

        deleteNodeAction =
            new CustomDeleteAction(selectionProvider, Messages.deleteNodeActionContextMenuLabel + Messages.shortcutDelete, false);
        deleteNodeAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.DELETE_16));
        deleteNodeAction.setEnabled(false);
        deleteFilesAction = new CustomDeleteAction(selectionProvider, Messages.deleteFilesActionContextMenuLabel, true);
        deleteFilesAction.setImageDescriptor(DMBrowserImages.IMG_DESC_DELETE_FILES);
        deleteFilesAction.setEnabled(false);

        exportNodeToFileSystemAction = new CustomExportAction(selectionProvider,
            Messages.saveNodeToFilesystemActionContextMenuLabel + Messages.saveNodeToFilesystemActionShortcut, ExportType.FILESYSTEM);
        exportNodeToFileSystemAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.EXPORT_16));

        exportNodeToProjectAction = new CustomExportAction(selectionProvider,
            Messages.saveNodeToProjectActionContextMenuLabel + Messages.saveNodeToProjectActionShortcut, ExportType.PROJECT);
        exportNodeToProjectAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.EXPORT_16));

        collapseAllNodesAction = new CollapseAllNodesAction(Messages.collapseAllNodesActionContextMenuLabel);
        collapseAllNodesAction.setImageDescriptor(DMBrowserImages.IMG_DESC_COLLAPSE_ALL);

        timelineAction = new OpenTimelineViewAction(selectionProvider, "Show Timeline");
        timelineAction.setImageDescriptor(ImageDescriptor.createFromImage(DMBrowserImages.IMG_TIMELINE));
        timelineAction.setEnabled(false);

        copyAction = new CopyNodeTitleAction(selectionProvider, "Copy");
        copyAction.setImageDescriptor(ImageDescriptor.createFromImage(ImageManager.getInstance().getSharedImage(StandardImages.COPY_16)));
        copyAction.setEnabled(false);
    }

    private void makeRefreshActions(final ISelectionProvider selectionProvider) {
        actionRefreshAll = new Action(Messages.refreshAllNodesActionContextMenuLabel + Messages.shortcutRefreshAll) {

            @Override
            public void run() {
                refresh();
            }
        };
        actionRefreshAll.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.REFRESH_16));

        refreshNodeAction =
            new RefreshNodeAction(selectionProvider, Messages.refreshNodeActionContextMenuLabel + Messages.shortcutRefreshSelected);
        refreshNodeAction.setImageDescriptor(DMBrowserImages.IMG_DESC_REFRESH_NODE);
        refreshNodeAction.setEnabled(false);
    }

    private void refresh() {
        if (!viewer.getTree().isDisposed()) {
            actionRefreshAll.setEnabled(false);
            contentProvider.clear();
            viewer.getTree().setEnabled(false);
            DMBrowserNode rootNode = (DMBrowserNode) viewer.getInput();
            if (rootNode == null) {
                rootNode = createRootNode();
                viewer.setInput(rootNode);
            } else {
                rootNode.clearChildren();
            }
            viewer.getTree().setEnabled(true);
            viewer.getTree().setFocus();
            viewer.refresh();
        }
    }

    private boolean mightHaveContent(final DMBrowserNode node) {
        if (node.getDataReference() != null
            || node.getDataReferenceId() != null || node.getFileReferencePath() != null) {
            return true;
        }
        if (node.areChildrenKnown()) {
            return mightHaveContent(node.getChildren());
        } else {
            // if the child nodes are unknown, the current node *might* have
            // content
            return true;
        }
    }

    private boolean mightHaveContent(final Collection<DMBrowserNode> nodes) {
        boolean result = false;
        for (final DMBrowserNode node : nodes) {
            if (mightHaveContent(node)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private void hookDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                if (event.getSelection() instanceof TreeSelection) {
                    TreeSelection sel = (TreeSelection) event.getSelection();
                    if (sel.getFirstElement() instanceof DMBrowserNode) {
                        DMBrowserNode node = (DMBrowserNode) sel.getFirstElement();
                        if (node.isLeafNode()) {
                            doubleClickAction.run();
                        }
                    }
                }
            }
        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    /**
     * @return list with the reachable host's
     */
    private Set<InstanceNodeSessionId> registerWorkflowHostService() {
        WorkflowHostService workflowHostService = serviceRegistryAccess.getService(WorkflowHostService.class);
        return workflowHostService.getWorkflowHostNodes();
    }

    private boolean isWorkflowHostReachable(ResolvableNodeId nodeID) {
        Set<InstanceNodeSessionId> registeredNodeID = registerWorkflowHostService();
        for (InstanceNodeSessionId nodeIdentifier : registeredNodeID) {
            if (nodeID.isSameInstanceNodeAs(nodeIdentifier)) {
                return true;
            }
        }
        return false;
    }

    private void disableUnreachableNode(final ResolvableNodeId unreachableID) {
        for (TreeItem item : viewer.getTree().getItems()) {
            DMBrowserNode node = (DMBrowserNode) item.getData();
            if (node == null) {
                continue;
            }
            ResolvableNodeId nodeID = node.getNodeIdentifier();
            if (node.isEnabled() && nodeID != null && nodeID.isSameInstanceNodeAs(unreachableID)) {
                node.setTitle(StringUtils.format(NODE_TEXT_FORMAT_TITLE_PLUS_HOSTNAME, node.getTitle(),
                    "[offline]"));
                disableNode(node);
            }
        }
    }

    private void disableNode(DMBrowserNode node) {
        refreshNodeAction.setEnabled(false);
        disableNodeWithoutRefresh(node);
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.refresh(node);
        }
    }

    private void disableNodeWithoutRefresh(DMBrowserNode node) {
        node.setType(DMBrowserNodeType.Workflow_Disabled);
        node.markAsLeaf();
        node.setEnabled(false);
    }

    @Override
    public void handleContentAvailable(final DMBrowserNode node) {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                if (viewer.getTree().isDisposed()) {
                    return;
                }
                viewer.refresh(node);
                if (node.getType() != DMBrowserNodeType.HistoryRoot) {
                    DMBrowserNode workflowNode = node.getNodeWithTypeWorkflow();
                    if (workflowNode != null && !workflowNode.getWorkflowHostName().equals(LOCAL)
                        && !isWorkflowHostReachable(workflowNode.getNodeIdentifier())) {
                        disableUnreachableNode(workflowNode.getNodeIdentifier());
                    }
                }
                if (!node.areChildrenKnown()) {
                    viewer.setExpandedState(node, false);
                } else {
                    viewer.setExpandedState(node, true);
                }

                if (visibleExpandedElements != null) {
                    viewer.setExpandedElements(visibleExpandedElements);
                }
                if (node == viewer.getInput()) {
                    viewer.getTree().setEnabled(true);
                    actionRefreshAll.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void handleContentRetrievalError(final DMBrowserNode node, final Exception cause) {
        if (cause instanceof CommunicationException) {
            // Since this type of exception can occur often during normal operation (e.g. remote node is offline) and is not an error, we do
            // not print the whole stack trace.
            log.warn("Retrieving data from data management failed: " + cause.getMessage());
        } else {
            log.error("Retrieving data from data management failed", cause);
        }

        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                /*
                 * No refresh of the node as a refresh would trigger another fetch which might result in the very same error.
                 */
                if (node == viewer.getInput()) {
                    viewer.getTree().setEnabled(true);
                    actionRefreshAll.setEnabled(true);
                }
                // disable workflow without showing error
                findAndDisableRootnode(node);
            }
        });

    }

    // recursively browse parent nodes until history root is found
    private void findAndDisableRootnode(DMBrowserNode node) {
        if (node.getParent().getType() == DMBrowserNodeType.HistoryRoot) {
            disableUnreachableNode(node.getNodeIdentifier());
        } else {
            findAndDisableRootnode(node.getParent());
        }
    }

    /**
     * 
     * An Item to compare.
     * 
     * @author Sascha Zur
     */
    private class FileCompareInput extends CompareEditorInput {

        private File left;

        private File right;

        FileCompareInput(CompareConfiguration cc, File left, File right) {
            super(cc);
            this.left = left;
            this.right = right;
        }

        @Override
        protected Object prepareInput(IProgressMonitor arg0) throws InvocationTargetException, InterruptedException {
            DiffNode result = new DiffNode(Differencer.CONFLICTING);
            result.setAncestor(new CompareItem(left));
            result.setLeft(new CompareItem(left));
            result.setRight(new CompareItem(right));

            return result;
        }

    }

    /**
     * 
     * One item for the comparison.
     * 
     * @author Sascha Zur
     */
    class CompareItem implements IStreamContentAccessor, ITypedElement, IModificationDate {

        private File contents;

        CompareItem(File f) {
            this.contents = f;
        }

        @Override
        public InputStream getContents() throws CoreException {
            try {
                return new ByteArrayInputStream(FileUtils.readFileToString(contents).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public long getModificationDate() {
            return 0;
        }

        @Override
        public Image getImage() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getType() {
            return null;
        }

    }
}
