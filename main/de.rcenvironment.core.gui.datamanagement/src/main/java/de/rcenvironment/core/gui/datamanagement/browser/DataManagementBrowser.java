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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
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

    protected final Log log = LogFactory.getLog(getClass());

    protected DMTreeSorter treeSorter;

    private Object[] expandedNodes = null;

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

    private Action exportNodeAction;

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

    /**
     * An {@link Action} to export data management entries to local files.
     * 
     * @author Christian Weiss
     * 
     */
    private final class CustomExportAction extends SelectionProviderAction {

        private final List<DMBrowserNode> selectedNodes = new LinkedList<DMBrowserNode>();

        private Display display;

        private final List<DMBrowserNodeType> savableNodeTypes = new ArrayList<DMBrowserNodeType>();

        private final List<DMBrowserNodeType> savableNodeAsFolder = new ArrayList<DMBrowserNodeType>();

        /*
         * Set all savable DMBrowserNodeTypes.
         */
        {
            // Set all DMBrowserNodeTypes which are to save as folder.
            savableNodeAsFolder.add(DMBrowserNodeType.Timeline);
            savableNodeAsFolder.add(DMBrowserNodeType.Components);
            savableNodeAsFolder.add(DMBrowserNodeType.Component);
            savableNodeAsFolder.add(DMBrowserNodeType.HistoryObject);
            savableNodeAsFolder.add(DMBrowserNodeType.Input);
            savableNodeAsFolder.add(DMBrowserNodeType.Output);
            savableNodeAsFolder.add(DMBrowserNodeType.IntermediateInputsFolder);
            savableNodeAsFolder.add(DMBrowserNodeType.LogFolder);
            savableNodeAsFolder.add(DMBrowserNodeType.ToolInputOutputFolder);
            savableNodeAsFolder.add(DMBrowserNodeType.DMDirectoryReference);
            // Set all savable DMBrowserNodeTypes.
            savableNodeTypes.add(DMBrowserNodeType.HistoryRoot);
            savableNodeTypes.add(DMBrowserNodeType.DMFileResource);
            savableNodeTypes.add(DMBrowserNodeType.Resource);
            savableNodeTypes.add(DMBrowserNodeType.Float);
            savableNodeTypes.add(DMBrowserNodeType.Vector);
            savableNodeTypes.add(DMBrowserNodeType.ShortText);
            savableNodeTypes.add(DMBrowserNodeType.Boolean);
            savableNodeTypes.add(DMBrowserNodeType.Integer);
            savableNodeTypes.add(DMBrowserNodeType.SmallTable);
            savableNodeTypes.add(DMBrowserNodeType.Indefinite);
            savableNodeTypes.add(DMBrowserNodeType.File);
            savableNodeTypes.add(DMBrowserNodeType.CommonText);
        }

        private CustomExportAction(ISelectionProvider provider, String text) {
            super(provider, text);
        }

        @Override
        public void selectionChanged(final IStructuredSelection selection) {
            // clear the old selection
            selectedNodes.clear();
            // the 'save' action is only enabled, if a DataService is
            // connected to delegate the deletion request to and the
            // selected is not empty
            boolean enabled = fileDataService != null && !selection.isEmpty();
            if (enabled) {
                @SuppressWarnings("unchecked") final Iterator<DMBrowserNode> iter = selection.iterator();
                while (iter.hasNext()) {
                    DMBrowserNode selectedNode = iter.next();
                    DMBrowserNodeType nodeType = selectedNode.getType();
                    if (selectedNode.isEnabled() && !selectedNode.areAllChildrenDisabled()
                        && (savableNodeTypes.contains(nodeType) || savableNodeAsFolder.contains(nodeType))) {
                        selectedNodes.add(selectedNode);
                    }
                }
                // action is only enabled, if at least one node is deletable
                // according to the deletable DMBrowserNodeTypes list
                enabled &= !selectedNodes.isEmpty();
                // action is only enabled if a potential content node is
                // selected
                enabled = mightHaveContent(selectedNodes);
                // store the Display to show the DirectoryDialog in 'run'
                display = Display.getCurrent();
            }
            setEnabled(enabled);
        }

        @Override
        public void run() {
            final List<DMBrowserNode> browserNodesToSave = new LinkedList<DMBrowserNode>(selectedNodes);
            FileDialog fileDialog = new FileDialog(display.getActiveShell(), SWT.SAVE);
            fileDialog.setText("Export");
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
                                String location;
                                if (job.getTargetFile() != null) {
                                    location = StringUtils.format(Messages.exportSuccessText, browserNodesToSave.toString(),
                                        job.getTargetFile().getAbsolutePath()).replace(BRACKET_OPEN, "").replace(BRACKET_CLOSE, "");
                                } else {
                                    location = StringUtils.format(Messages.exportSuccessText,
                                        browserNodesToSave.toString(), targetDirectory.getAbsolutePath()).replace(BRACKET_OPEN, "")
                                        .replace(BRACKET_CLOSE, "");
                                }
                                MessageDialog.openInformation(display.getActiveShell(), "Export", location);
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
                if (savableNodeAsFolder.contains(browserNode.getType())) {
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
                    "[^-\\s\\(\\)._a-zA-Z0-9]", "_");
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
                } catch (NullPointerException e) {
                    log.error("");
                    // FIXME: log and warn
                    e = null;
                } catch (AuthorizationException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
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
            if (obj instanceof DMBrowserNode) {
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

        private final List<DMBrowserNode> selectedNodes = new LinkedList<DMBrowserNode>();

        private Display display;

        private final List<DMBrowserNodeType> deletableNodeTypes = new ArrayList<DMBrowserNodeType>();

        private boolean hasNotFinishedWorkflows;

        private boolean isFileAction;

        /*
         * Set all deletable DMBrowserNodeTypes.
         */
        {
            deletableNodeTypes.add(DMBrowserNodeType.Workflow);
        }

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
                    if (deletableNodeTypes.contains(selectedNode.getType())) {
                        boolean hasfinalState =
                            selectedNode.getMetaData() != null
                                && selectedNode.getMetaData().getValue(META_DATA_WORKFLOW_FINAL_STATE) != null;
                        boolean hasDataReferences =
                            !Boolean.valueOf(selectedNode.getMetaData().getValue(METADATA_WORKFLOW_FILES_DELETED));
                        boolean isMarkedForDeletion =
                            Boolean.valueOf(selectedNode.getMetaData().getValue(METADATA_WORKFLOW_IS_MARKED_FOR_DELETION));
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
            final List<DMBrowserNode> browserNodesToDelete = new LinkedList<DMBrowserNode>(
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
                            if (deleted) {
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
            if (obj instanceof DMBrowserNode) {
                DMBrowserNode node = (DMBrowserNode) obj;

                if (node.isEnabled()
                    && (node.getType() == DMBrowserNodeType.Boolean
                        || node.getType() == DMBrowserNodeType.CommonText
                        || node.getType() == DMBrowserNodeType.DMFileResource
                        || node.getType() == DMBrowserNodeType.File
                        || node.getType() == DMBrowserNodeType.Float
                        || node.getType() == DMBrowserNodeType.Integer
                        || node.getType() == DMBrowserNodeType.ShortText
                        || node.getType() == DMBrowserNodeType.SmallTable
                        || node.getType() == DMBrowserNodeType.Matrix
                        || node.getType() == DMBrowserNodeType.Vector)) {
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

        private final List<DMBrowserNode> selectedNodes = new LinkedList<DMBrowserNode>();

        private final List<DMBrowserNodeType> refreshableNodes = new ArrayList<DMBrowserNodeType>();

        /*
         * Whitelist: Nodes which can be refreshed.
         */
        {
            refreshableNodes.add(DMBrowserNodeType.Workflow);
            refreshableNodes.add(DMBrowserNodeType.Timeline);
            refreshableNodes.add(DMBrowserNodeType.Components);
            refreshableNodes.add(DMBrowserNodeType.WorkflowRunInformation);
        }

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
                    if (refreshableNodes.contains(selectedNode.getType())) {
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
                final IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().
                    showView("de.rcenvironment.gui.Timeline", nodeSelected.getNodeWithTypeWorkflow().getWorkflowID(),
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
            expandedNodes = new Object[] {};
        }

    }

    /**
     * An {@link Action} that triggers the different sort actions.
     * 
     */
    private final class CustomSortAction extends SelectionProviderAction {

        private final List<DMBrowserNode> selectedNodes = new LinkedList<DMBrowserNode>();

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

        private final List<DMBrowserNodeType> comparableNodeTypes = new ArrayList<DMBrowserNodeType>();

        private DMBrowserNode node;

        private DMBrowserNode node2;

        /*
         * Set all comparable DMBrowserNodeTypes.
         */
        {
            comparableNodeTypes.add(DMBrowserNodeType.DMFileResource);
            // add comparable node typs like double, float...
        }

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
                    if (comparableNodeTypes.contains(node.getType()) && comparableNodeTypes.contains(node2.getType())
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
                        throw new RuntimeException(e.getCause());
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
                }
            } else if (event.keyCode == SWT.DEL) {
                // add shortcut for delete action
                if (deleteNodeAction.isEnabled()) {
                    deleteNodeAction.run();
                }
            } else if (event.keyCode == SWT.F5) {
                // add shortcut for refresh all action
                actionRefreshAll.run();
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
        // clear children of selected node
        DMBrowserNode toRefresh = node.getNodeWithTypeWorkflow();
        toRefresh.clearChildren();
        contentProvider.clear(toRefresh);

        if (viewer.getExpandedState(toRefresh)) {
            expandedNodes = viewer.getVisibleExpandedElements();
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
                // TreeViewer viewer = (TreeViewer) event.getViewer();
                IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
                Object selectedNode = thisSelection.getFirstElement();
                viewer.setExpandedState(selectedNode, !viewer.getExpandedState(selectedNode));
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

        manager.add(new Separator());
        manager.add(openInEditorAction);
        if (OS.isFamilyWindows()) {
            manager.add(openTiglAction);
        }
        manager.add(refreshNodeAction);
        manager.add(actionRefreshAll);

        manager.add(new Separator());
        // manager.add(autoRefreshAction);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        manager.add(new Separator());
        manager.add(subMenuManager);
        manager.add(new Separator());
        manager.add(copyAction);
        manager.add(exportNodeAction);
        manager.add(new Separator());
        manager.add(deleteFilesAction);
        manager.add(deleteNodeAction);
        manager.add(new Separator());
        manager.add(timelineAction);
        manager.add(new Separator());
        manager.add(compareAction);
        manager.add(new Separator());
        // Other plug-ins can contribute there actions here
        // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(sortAscendingName);
        manager.add(sortDescendingName);
        manager.add(sortTimestampAsc);
        manager.add(sortTimestampDesc);
        manager.add(new Separator());
        manager.add(refreshNodeAction);
        manager.add(actionRefreshAll);
        // manager.add(autoRefreshAction);
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

        exportNodeAction = new CustomExportAction(selectionProvider, Messages.saveNodeActionContextMenuLabel);
        exportNodeAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.EXPORT_16));
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
        contentProvider.clear();
        // disable the widget
        viewer.getTree().setEnabled(false);
        // disable the action
        // FIXME: ensure re-enabling upon errors
        actionRefreshAll.setEnabled(false);
        DMBrowserNode rootNode = (DMBrowserNode) viewer.getInput();
        if (rootNode == null) {
            rootNode = createRootNode();
            viewer.setInput(rootNode);
        } else {
            rootNode.clearChildren();
        }

        expandedNodes = viewer.getVisibleExpandedElements();
        viewer.getTree().setEnabled(true);
        viewer.getTree().setFocus();
        viewer.refresh();
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
            ResolvableNodeId nodeID = node.getNodeIdentifier();
            if (node.isEnabled() && nodeID.isSameInstanceNodeAs(unreachableID)) {
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
                if (node == viewer.getInput()) {
                    viewer.getTree().setEnabled(true);
                    actionRefreshAll.setEnabled(true);
                }
                if (node.getType() != DMBrowserNodeType.HistoryRoot) {
                    DMBrowserNode workflowNode = node.getNodeWithTypeWorkflow();
                    if (workflowNode != null && !workflowNode.getWorkflowHostName().equals(LOCAL)
                        && !isWorkflowHostReachable(workflowNode.getNodeIdentifier())) {
                        disableUnreachableNode(workflowNode.getNodeIdentifier());
                    }
                }

                for (final Object expNode : expandedNodes) {
                    if (node.getChildren().contains(expNode)) {
                        viewer.expandToLevel(expNode, 1);
                    }
                }
                if (node.getChildren().isEmpty()) {
                    viewer.setExpandedState(node, false);
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

    };

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
                return null;
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
