/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.WorkflowHostSetListener;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleModelSnapshot;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowFilter;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowModelService;
import de.rcenvironment.core.gui.resources.api.FontManager;
import de.rcenvironment.core.gui.resources.api.StandardFonts;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.parts.WorkflowRunNodePart.ComponentStateFigureImpl;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * The workflow console view.
 * 
 * @author Enrico Tappert
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class ConsoleView extends ViewPart {

    /** Identifier used to find the this view within the workbench. */
    public static final String ID = "de.rcenvironment.gui.WorkflowComponentConsole"; //$NON-NLS-1$

    /** The index of "no selection" in a SWT combo box. */
    private static final int COMBO_NO_SELECTION = -1;

    private static ConsoleView instance;

    private static final boolean STDERR_PRESELECTED = true;

    private static final boolean STDOUT_PRESELECTED = true;

    private static final boolean COMP_LOG_PRESELECTED = true;

    private static final int COLUMN_WIDTH_TYPE = 70;

    private static final int COLUMN_WIDTH_TIME = 150;

    private static final int COLUMN_WIDTH_STD = 500;

    private static final int COLUMN_WIDTH_COMPONENT = 100;

    private static final int COLUMN_WIDTH_WORKFLOW = 400;

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

    private int lastKnownSequenceId = ConsoleRowModelService.INITIAL_SEQUENCE_ID;

    private final Timer modelQueryTimer = new Timer();

    private final ConsoleRowModelService consoleModel;

    private final ConsoleRowFilter rowFilter = new ConsoleRowFilter();

    private volatile boolean scrollLock = false;

    private Button deleteSearchButton;

    private Display display;

    private ServiceRegistryPublisherAccess serviceRegistryAccess;

    private MenuItem copyLineItem;

    private MenuItem copyMessageItem;

    /**
     * A timer task that is used to periodically check the {@link ConsoleRowModelService} for modifications. This approach was chosen over a
     * callback/observer structure to realize rate limiting of GUI updates to ensure responsiveness in high CPU load situations.
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
        ApplySnapshotRunnable(ConsoleModelSnapshot snapshot) {
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
                String[] newList = convertToDisplayArray(snapshot.getWorkflowList(), "Workflows");
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
                String[] newList = convertToDisplayArray(snapshot.getComponentList(), "Components");
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

        private String[] convertToDisplayArray(Collection<String> input, String firstEntry) {
            String[] result = new String[input.size() + 1];
            result[0] = StringUtils.format("[All %s]", firstEntry);
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
        display = parent.getShell().getDisplay();

        parent.setLayout(new GridLayout(3, false));

        createWorkflowListingArrangement(parent);
        createComponentListingArrangement(parent);
        createSearchArrangement(parent);
        createLevelArrangement(parent);
        createTableArrangement(parent);

        // Triggers if the copy functions in context menu are enabled
        consoleRowTableViewer.getTable().addMenuDetectListener(new MenuDetectListener() {

            @Override
            public void menuDetected(MenuDetectEvent event) {
                copyLineItem.setEnabled(!consoleRowTableViewer.getSelection().isEmpty());
                copyMessageItem.setEnabled(!consoleRowTableViewer.getSelection().isEmpty());
            }
        });

        // set text field listener
        searchTextField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent arg0) {
                rowFilter.setSearchTerm(searchTextField.getText());
                applyNewRowFilter(true);
                deleteSearchButton.setEnabled(!searchTextField.getText().equals(""));
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
            public void onReachableWorkflowHostsChanged(Set<InstanceNodeSessionId> reachableWfHosts,
                Set<InstanceNodeSessionId> addedWfHosts,
                Set<InstanceNodeSessionId> removedWfHosts) {

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
    private void createLevelArrangement(Composite parentComposite) {
        Composite filterComposite = new Composite(parentComposite, SWT.NONE);
        filterComposite.setLayout(new GridLayout(4, false));
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 3;
        filterComposite.setLayoutData(gd);

        Label label = new Label(filterComposite, SWT.NONE);
        label.setText("Message Types: ");

        // meta info checkbox
        checkboxMetaInfo = new Button(filterComposite, SWT.CHECK);
        checkboxMetaInfo.setText(Messages.compLog);
        checkboxMetaInfo.setSelection(COMP_LOG_PRESELECTED);

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
    private void createWorkflowListingArrangement(Composite parentComposite) {
        Composite workflowComposite = new Composite(parentComposite, SWT.NONE);
        workflowComposite.setLayout(new GridLayout());

        // workflow listing combo box
        workflowCombo = new Combo(workflowComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        workflowCombo.select(INITIAL_SELECTION);
        GridData gd = new GridData();
        gd.widthHint = WORKFLOW_WIDTH;
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        workflowCombo.setLayoutData(gd);
    }

    /**
     * Creates the composite structure with platform selection options.
     * 
     * @param componentComposite Parent composite for the platform selection options.
     */
    private void createComponentListingArrangement(Composite parentComposite) {
        Composite componentComposite = new Composite(parentComposite, SWT.NONE);
        componentComposite.setLayout(new GridLayout());

        // compoenent listing combo box
        componentCombo = new Combo(componentComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        componentCombo.select(INITIAL_SELECTION);
        GridData gd = new GridData();
        gd.widthHint = COMPONENT_WIDTH;
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        componentCombo.setLayoutData(gd);
    }

    /**
     * Creates the composite structure with search options.
     * 
     * @param searchComposite Parent composite for the search options.
     */
    private void createSearchArrangement(Composite parentComposite) {
        Composite searchComposite = new Composite(parentComposite, SWT.NONE);
        searchComposite.setLayout(new GridLayout(2, false));

        // search text field
        searchTextField = new Text(searchComposite, SWT.SEARCH);
        searchTextField.setMessage(Messages.search);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
        gd.widthHint = TEXT_WIDTH;
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        searchTextField.setLayoutData(gd);
        searchTextField.setSize(TEXT_WIDTH, SWT.DEFAULT);

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
    private void createTableArrangement(Composite parent) {

        Composite tableComposite = new Composite(parent, SWT.None);
        tableComposite.setLayout(new GridLayout());
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.horizontalSpan = 3;
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        tableComposite.setLayoutData(gd);

        // create table viewer
        consoleRowTableViewer = new TableViewer(tableComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
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
        table.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));
        table.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));

        // create copy context menu
        Menu contextMenu = new Menu(parent);
        copyLineItem = new MenuItem(contextMenu, SWT.PUSH);
        copyMessageItem = new MenuItem(contextMenu, SWT.PUSH);
        copyMessageItem.setText(Messages.copyMessage + "\tCtrl+Alt+C");
        copyMessageItem.setImage(Activator.getInstance().getImageRegistry().get(Activator.IMAGE_COPY));
        copyMessageItem.addSelectionListener(new CopyToClipboardListener(consoleRowTableViewer));
        copyLineItem.setText(Messages.copyLine + "\tCtrl+C");
        copyLineItem.setImage(Activator.getInstance().getImageRegistry().get(Activator.IMAGE_COPY));
        copyLineItem.addSelectionListener(new CopyToClipboardListener(consoleRowTableViewer));
        consoleRowTableViewer.getControl().setMenu(contextMenu);

    }

    private Action[] createToolbarActions() {

        Action scrollLockAction = new Action(Messages.scrollLock, SWT.TOGGLE) {

            @Override
            public void run() {
                scrollLock = !scrollLock;
            }
        };
        scrollLockAction.setImageDescriptor(ImageDescriptor.createFromURL(
            ComponentStateFigureImpl.class.getResource("/resources/icons/scrollLock.gif")));

        Action deleteAction = new Action(Messages.clear, ImageDescriptor.createFromImage(PlatformUI.getWorkbench()
            .getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE))) {

            @Override
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
                } else if ((e.stateMask == SWT.CTRL && e.keyCode == 'c') && copyLineItem.isEnabled()) {
                    CopyToClipboardHelper helper = new CopyToClipboardHelper(table);
                    helper.copyToClipboard(CopyToClipboardHelper.COPY_LINE);
                } else if ((e.stateMask == (SWT.CTRL + SWT.ALT) && (e.keyCode == 'c')) && copyLineItem.isEnabled()) {
                    CopyToClipboardHelper helper = new CopyToClipboardHelper(table);
                    helper.copyToClipboard(CopyToClipboardHelper.COPY_MESSAGE);
                }
            }
        });
    }

}
