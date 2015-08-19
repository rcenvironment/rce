/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.console;

import java.util.Collection;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostSetListener;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleModelSnapshot;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowFilter;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowModelService;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.parts.ReadOnlyWorkflowNodePart.ComponentStateFigureImpl;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * The workflow console view.
 * 
 * @author Enrico Tappert
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ConsoleView extends ViewPart {

    /** Identifier used to find the this view within the workbench. */
    public static final String ID = "de.rcenvironment.gui.WorkflowComponentConsole"; //$NON-NLS-1$

    /** The index of "no selection" in a SWT combo box. */
    private static final int COMBO_NO_SELECTION = -1;

    private static ConsoleView instance;

    private static final boolean STDERR_PRESELECTED = true;

    private static final boolean STDOUT_PRESELECTED = true;

    private static final boolean META_INFO_PRESELECTED = true;

    private static final int COLUMN_WIDTH_TYPE = 40;

    private static final int COLUMN_WIDTH_TIME = 150;

    private static final int COLUMN_WIDTH_STD = 500;

    private static final int COLUMN_WIDTH_COMPONENT = 100;

    private static final int COLUMN_WIDTH_WORKFLOW = 100;

    private static final int INITIAL_SELECTION = 0;

    private static final int NO_SPACE = 0;

    private static final int WORKFLOW_WIDTH = 250;

    private static final int COMPONENT_WIDTH = WORKFLOW_WIDTH;

    private static final int TEXT_WIDTH = WORKFLOW_WIDTH;

    private static final long MODEL_QUERY_MIN_INTERVAL = 500;

    private Button checkboxStderr;

    private Button checkboxStdout;

    private Button checkboxMetaInfo;

    private Combo workflowCombo;

    private Combo componentCombo;

    private TableViewer consoleRowTableViewer;

    private ConsoleColumnSorter consoleColumnSorter;

    private Text searchTextField;

    private Font fixedWidthFont;

    private int lastKnownSequenceId = ConsoleRowModelService.INITIAL_SEQUENCE_ID;

    private final Timer modelQueryTimer = new Timer();

    private final ConsoleRowModelService consoleModel;

    private final ConsoleRowFilter rowFilter = new ConsoleRowFilter();

    private volatile boolean scrollLock = false;

    private Button deleteSearchButton;

    private Display display;

    private ServiceRegistryPublisherAccess serviceRegistryAccess;

    /**
     * A timer task that is used to periodically check the {@link ConsoleRowModelService} for modifications. This approach was chosen
     * over a callback/observer structure to realize rate limiting of GUI updates to ensure responsiveness in high CPU load situations.
     * 
     * @author Robert Mischke
     */
    private class QueryModelForChangesTask extends TimerTask {

        @Override
        public void run() {
            // Note: this task assumes that it is not invoked concurrently
            ConsoleModelSnapshot snapshot = consoleModel.getSnapshotIfModifiedSince(lastKnownSequenceId);
            if (snapshot == null) {
                return;
            }

            lastKnownSequenceId = snapshot.getSequenceId();
            // apply synchronously for rate limiting
            syncApplySnapshot(snapshot);
        }
    }

    /**
     * A Runnable that applies a given {@link ConsoleModelSnapshot} to the GUI. Intended to run from the SWT event dispatch thread.
     * 
     * @author Robert Mischke
     */
    private class ApplySnapshotRunnable implements Runnable {

        private ConsoleModelSnapshot snapshot;

        /**
         * Constructor.
         */
        public ApplySnapshotRunnable(ConsoleModelSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        /**
         * Apply the given snapshot to the UI.
         */
        @Override
        public void run() {
            if (workflowCombo.isDisposed()) {
                // do nothing if the view was disposed in the meantime
                return;
            }

            // TODO check if these combo modifications trigger unnecessary filter/model updates

            // update dropdown boxes if needed
            if (snapshot.hasWorkflowListChanged()) {
                String oldSelection = getWorkflowSelection();
                String[] newList = convertToDisplayArray(snapshot.getWorkflowList());
                workflowCombo.setItems(newList);
                // restore selection to same value, if possible
                workflowCombo.select(INITIAL_SELECTION);
                for (int i = 1; i < newList.length; i++) {
                    // note: oldSelection may be null
                    if (newList[i].equals(oldSelection)) {
                        workflowCombo.select(i);
                        break;
                    }
                }
            }
            if (snapshot.hasComponentListChanged()) {
                String oldSelection = getComponentSelection();
                String[] newList = convertToDisplayArray(snapshot.getComponentList());
                componentCombo.setItems(newList);
                // restore selection to same value, if possible
                componentCombo.select(INITIAL_SELECTION);
                for (int i = 1; i < newList.length; i++) {
                    // note: oldSelection may be null
                    if (newList[i].equals(oldSelection)) {
                        componentCombo.select(i);
                        break;
                    }
                }
            }

            if (snapshot.hasFilteredRowListChanged()) {
                consoleRowTableViewer.setInput(snapshot.getFilteredRows());
                if (!scrollLock) {
                    consoleRowTableViewer.getTable().setTopIndex(snapshot.getFilteredRows().size());
                    consoleRowTableViewer.getTable().getVerticalBar().setSelection(
                        consoleRowTableViewer.getTable().getVerticalBar().getMaximum() + snapshot.getFilteredRows().size());

                }
                // "false" parameter improves performance for de-facto immutable rows
                consoleRowTableViewer.refresh(true);
            }
        }

        private String[] convertToDisplayArray(Collection<String> input) {
            String[] result = new String[input.size() + 1];
            result[0] = Messages.all;
            int i = 1;
            for (String wf : input) {
                result[i] = wf;
                i++;
            }
            return result;
        }
    }

    public ConsoleView() {
        instance = this;
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        consoleModel = serviceRegistryAccess.getService(ConsoleRowModelService.class);

        // enqueue the polling task at a non-fixed rate for simple
        // update rate limiting in high CPU load situations
        // TODO move to an initialization callback instead of constructor
        modelQueryTimer.schedule(new QueryModelForChangesTask(), MODEL_QUERY_MIN_INTERVAL, MODEL_QUERY_MIN_INTERVAL);
    }

    public static ConsoleView getInstance() {
        return instance;
    }

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        display = parent.getShell().getDisplay();

        // TODO check font size etc. in Windows
        fixedWidthFont = new Font(parent.getDisplay(), "Courier", 10, SWT.NORMAL);

        // filter = level selection, platform selection, and search text field
        Composite filterComposite = new Composite(parent, SWT.NONE);
        filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        filterComposite.setLayout(new RowLayout());

        createLevelArrangement(filterComposite);
        createWorkflowListingArrangement(filterComposite);
        createComponentListingArrangement(filterComposite);
        createSearchArrangement(filterComposite);

        // sash = table and text display
        Composite sashComposite = new Composite(parent, SWT.NONE);
        sashComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sashComposite.setLayout(new GridLayout(1, false));

        createTableArrangement(sashComposite);

        // set text field listener
        searchTextField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent arg0) {
                rowFilter.setSearchTerm(searchTextField.getText());
                applyNewRowFilter(true);
                if (searchTextField.getText().equals("")) {
                    deleteSearchButton.setEnabled(false);
                } else {
                    deleteSearchButton.setEnabled(true);
                }
            }
        });

        // create common change/selection listener
        SelectionListener changeListener = new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // copy UI settings to filter
                rowFilter.setIncludeMetaInfo(checkboxMetaInfo.getSelection());
                rowFilter.setIncludeLifecylceEvents(checkboxMetaInfo.getSelection());
                rowFilter.setIncludeStdout(checkboxStdout.getSelection());
                rowFilter.setIncludeStderr(checkboxStderr.getSelection());
                rowFilter.setWorkflow(getWorkflowSelection());
                rowFilter.setComponent(getComponentSelection());
                // apply & trigger update
                applyNewRowFilter(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        };
        deleteSearchButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                searchTextField.setText("");
                rowFilter.setSearchTerm("");
                applyNewRowFilter(true);
                if (searchTextField.getText().equals("")) {
                    deleteSearchButton.setEnabled(false);
                } else {
                    deleteSearchButton.setEnabled(true);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

        checkboxStderr.addSelectionListener(changeListener);
        checkboxStdout.addSelectionListener(changeListener);
        checkboxMetaInfo.addSelectionListener(changeListener);
        workflowCombo.addSelectionListener(changeListener);
        componentCombo.addSelectionListener(changeListener);

        // add sorting functionality
        consoleColumnSorter = new ConsoleColumnSorter();
        consoleRowTableViewer.setSorter(consoleColumnSorter);

        // add toolbar actions (right top of view)
        for (Action action : createToolbarActions()) {
            getViewSite().getActionBars().getToolBarManager().add(action);
        }

        // set the initial filter; this also schedules an immediate update
        applyNewRowFilter(true);

        registerWorkflowHostSetListener();

        addHotkeysToTable(consoleRowTableViewer);
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * @param display
     */
    // TODO 5.0: move this into the ConsoleRowModelService
    private void registerWorkflowHostSetListener() {
        serviceRegistryAccess.registerService(WorkflowHostSetListener.class, new WorkflowHostSetListener() {

            @Override
            public void onReachableWorkflowHostsChanged(Set<NodeIdentifier> reachableWfHosts, Set<NodeIdentifier> addedWfHosts,
                Set<NodeIdentifier> removedWfHosts) {

                consoleModel.updateSubscriptions();
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (!consoleRowTableViewer.getTable().isDisposed()) {
                            consoleRowTableViewer.refresh();
                        }
                    }
                });
            }
        });
    }

    /**
     * @param true to trigger an immediate list update
     */
    private void applyNewRowFilter(boolean triggerUpdate) {
        consoleModel.setRowFilter(rowFilter);

        if (triggerUpdate) {
            triggerImmediateUpdate();
        }

        // clear selection
        consoleRowTableViewer.setSelection(new StructuredSelection());
    }

    private void triggerImmediateUpdate() {
        // add an update task immediately
        modelQueryTimer.schedule(new QueryModelForChangesTask(), 0);
    }

    @Override
    public void dispose() {
        // shut down the query timer
        modelQueryTimer.cancel();
        // dispose the font as it was created locally
        fixedWidthFont.dispose();
        super.dispose();
        if (serviceRegistryAccess != null) {
            serviceRegistryAccess.dispose();
        }
    }

    /** @return the currently selected workflow, or null if none selected. */
    private String getWorkflowSelection() {
        if (workflowCombo.getSelectionIndex() != COMBO_NO_SELECTION && workflowCombo.getSelectionIndex() != INITIAL_SELECTION) {
            return workflowCombo.getItem(workflowCombo.getSelectionIndex());
        } else {
            return null;
        }
    }

    /** @return the currently selected component, or null if none selected. */
    private String getComponentSelection() {
        if (componentCombo.getSelectionIndex() != COMBO_NO_SELECTION && componentCombo.getSelectionIndex() != INITIAL_SELECTION) {
            return componentCombo.getItem(componentCombo.getSelectionIndex());
        } else {
            return null;
        }
    }

    @Override
    public void setFocus() {
        consoleRowTableViewer.getControl().setFocus();
    }

    private void syncApplySnapshot(final ConsoleModelSnapshot snapshot) {
        if (consoleRowTableViewer != null && !consoleRowTableViewer.getTable().isDisposed()) {
            consoleRowTableViewer.getTable().getDisplay().syncExec(new ApplySnapshotRunnable(snapshot));
        }
    }

    /**
     * Creates the composite structure with level selection options.
     * 
     * @param filterComposite Parent composite for the level selection options.
     */
    private void createLevelArrangement(Composite filterComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = NO_SPACE;
        filterComposite.setLayout(rowLayout);

        // meta info checkbox
        checkboxMetaInfo = new Button(filterComposite, SWT.CHECK);
        checkboxMetaInfo.setText(Messages.metaInfo);
        checkboxMetaInfo.setSelection(META_INFO_PRESELECTED);

        // stdout checkbox
        checkboxStdout = new Button(filterComposite, SWT.CHECK);
        checkboxStdout.setText(Messages.stdout);
        checkboxStdout.setSelection(STDOUT_PRESELECTED);

        // stderr checkbox
        checkboxStderr = new Button(filterComposite, SWT.CHECK);
        checkboxStderr.setText(Messages.stderr);
        checkboxStderr.setSelection(STDERR_PRESELECTED);
    }

    /**
     * Creates the composite structure with platform selection options.
     * 
     * @param workflowComposite Parent composite for the platform selection options.
     */
    private void createWorkflowListingArrangement(Composite workflowComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = NO_SPACE;
        workflowComposite.setLayout(rowLayout);

        // workflow listing combo box
        workflowCombo = new Combo(workflowComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        workflowCombo.select(INITIAL_SELECTION);
        workflowCombo.setLayoutData(new RowData(WORKFLOW_WIDTH, SWT.DEFAULT));
    }

    /**
     * Creates the composite structure with platform selection options.
     * 
     * @param componentComposite Parent composite for the platform selection options.
     */
    private void createComponentListingArrangement(Composite componentComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = NO_SPACE;
        componentComposite.setLayout(rowLayout);

        // compoenent listing combo box
        componentCombo = new Combo(componentComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        componentCombo.select(INITIAL_SELECTION);
        componentCombo.setLayoutData(new RowData(COMPONENT_WIDTH, SWT.DEFAULT));
    }

    /**
     * Creates the composite structure with search options.
     * 
     * @param searchComposite Parent composite for the search options.
     */
    private void createSearchArrangement(Composite searchComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = 7;
        searchComposite.setLayout(rowLayout);

        // search text field
        searchTextField = new Text(searchComposite, SWT.SEARCH);
        searchTextField.setMessage(Messages.search);
        searchTextField.setSize(TEXT_WIDTH, SWT.DEFAULT);
        searchTextField.setLayoutData(new RowData(TEXT_WIDTH, SWT.DEFAULT));

        deleteSearchButton = new Button(searchComposite, SWT.BUTTON1);
        deleteSearchButton.setText(Messages.resetSearch);
        if (searchTextField.getText().equals("")) {
            deleteSearchButton.setEnabled(false);
        }
    }

    /**
     * Creates the composite structure with log display and selection options.
     * 
     * @param platformComposite Parent composite for log display and selection options.
     */
    private void createTableArrangement(Composite tableComposite) {
        tableComposite.setLayout(new GridLayout());

        // create table viewer
        consoleRowTableViewer = new TableViewer(tableComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
        consoleRowTableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // create table header and column styles
        String[] titles = new String[] {
            Messages.type,
            Messages.timestamp,
            Messages.message,
            Messages.component,
            Messages.workflow
        };
        int[] bounds = { COLUMN_WIDTH_TYPE, COLUMN_WIDTH_TIME, COLUMN_WIDTH_STD, COLUMN_WIDTH_COMPONENT, COLUMN_WIDTH_WORKFLOW };

        // for all columns
        for (int i = 0; i < bounds.length; i++) {

            final int index = i;
            final TableViewerColumn viewerColumn = new TableViewerColumn(consoleRowTableViewer, SWT.NONE);
            final TableColumn column = viewerColumn.getColumn();

            // set column properties
            column.setText(titles[i]);
            column.setWidth(bounds[i]);
            column.setResizable(true);
            column.setMoveable(true);

            column.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    consoleColumnSorter.setColumn(index);
                    int direction = consoleRowTableViewer.getTable().getSortDirection();

                    if (consoleRowTableViewer.getTable().getSortColumn() == column) {
                        if (direction == SWT.UP) {
                            direction = SWT.DOWN;
                        } else {
                            direction = SWT.UP;
                        }
                    } else {
                        direction = SWT.UP;
                    }
                    consoleRowTableViewer.getTable().setSortDirection(direction);
                    consoleRowTableViewer.getTable().setSortColumn(column);

                    consoleRowTableViewer.refresh();
                }
            });
        }

        // set table content
        consoleRowTableViewer.setContentProvider(new ConsoleContentProvider());
        ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
        consoleRowTableViewer.setLabelProvider(new DecoratedConsoleLabelProvider(new ConsoleLabelProvider(), decorator));

        // set table layout data
        final Table table = consoleRowTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(false);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // set font & color
        table.setFont(fixedWidthFont);
        table.setForeground(tableComposite.getDisplay().getSystemColor(SWT.COLOR_BLACK));

        // create copy context menu
        Menu contextMenu = new Menu(tableComposite);
        MenuItem copyItem = new MenuItem(contextMenu, SWT.PUSH);
        copyItem.setText(Messages.copy);
        copyItem.setImage(Activator.getInstance().getImageRegistry().get(Activator.IMAGE_COPY));
        copyItem.addSelectionListener(new CopyToClipboardListener(consoleRowTableViewer));
        consoleRowTableViewer.getControl().setMenu(contextMenu);
    }

    private Action[] createToolbarActions() {

        Action scrollLockAction = new Action(Messages.scrollLock, SWT.TOGGLE) {

            public void run() {
                scrollLock = !scrollLock;
            }
        };
        scrollLockAction.setImageDescriptor(ImageDescriptor.createFromURL(
            ComponentStateFigureImpl.class.getResource("/resources/icons/scrollLock.gif")));

        Action deleteAction = new Action(Messages.clear, ImageDescriptor.createFromImage(PlatformUI.getWorkbench()
            .getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE))) {

            public void run() {
                // for now, "clear" means "wipe the model"
                consoleModel.clearAll();
            }
        };

        return new Action[] { scrollLockAction, deleteAction };
    }

    private void addHotkeysToTable(final TableViewer table) {
        table.getTable().addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
                    table.getTable().selectAll();
                } else if (e.stateMask == SWT.CTRL && e.keyCode == 'c') {
                    CopyToClipboardHelper helper = new CopyToClipboardHelper(table);
                    helper.copyToClipboard();
                }
            }
        });
    }
}
