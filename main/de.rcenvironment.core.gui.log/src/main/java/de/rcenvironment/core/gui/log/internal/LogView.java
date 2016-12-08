/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.WorkflowHostSetListener;
import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * The whole log view.
 * 
 * @author Enrico Tappert
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Oliver Seebach
 */
public class LogView extends ViewPart {

    /** Identifier used to find the this view within the workbench. */
    public static final String ID = "de.rcenvironment.rce.gui.log.view"; //$NON-NLS-1$

    private static LogView myInstance;

    private static final int TEXT_AREA_WORD_WRAP_WIDTH = 180;

    private static final boolean ERROR_PRESELECTED = true;

    private static final boolean INFO_PRESELECTED = true;

    private static final boolean WARN_PRESELECTED = true;

    private static final int COLUMN_WIDTH_BUNDLE = 250;

    private static final int COLUMN_WIDTH_PLATFORM = 250;

    private static final int COLUMN_WIDTH_LEVEL = 70;

    private static final int COLUMN_WIDTH_MESSAGE = 250;

    private static final int COLUMN_WIDTH_TIME = 140;

    private static final int NO_SPACE = 0;

    private static final int PLATFORM_WIDTH = 250;

    private static final int TEXT_WIDTH = PLATFORM_WIDTH;

    private Button myCheckboxError;

    private Button myCheckboxInfo;

    private Button myCheckboxWarn;

    private Combo myPlatformCombo;

    private LogTableFilter myListenerAndFilter;

    private LogTableColumnSorter myTableColumnSorter;

    private TableViewer myLogEntryTableViewer;

    private Text myMessageTextArea;

    private Text mySearchTextField;

    private Label searchWarning;

    private SerializableLogEntry displayedLogEntry;

    private boolean scrollLocked = false;

    private boolean adjustedSearchMessage = false;

    private Action clearAction = null;

    private Action copyAction = null;

    private Action scrollLockAction = null;

    private final LogModel.Listener listener = new LogModel.Listener() {

        @Override
        public void handleLogEntryAdded(final SerializableLogEntry logEntry) {
            if (!myLogEntryTableViewer.getTable().isDisposed()
                && !myLogEntryTableViewer.getTable().getDisplay().isDisposed()) {
                myLogEntryTableViewer.getTable().getDisplay().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (!myLogEntryTableViewer.getTable().isDisposed()) {
                            if (logEntry.getPlatformIdentifer().equals(getPlatform())) {
                                myLogEntryTableViewer.add(logEntry);
                                if (!LogView.this.isScrollLocked()) {
                                    myLogEntryTableViewer.reveal(logEntry);
                                }
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void handleLogEntryRemoved(final SerializableLogEntry logEntry) {
            /*
             * Log entries are also removed, because otherwise upon a resort the log entries would just vanish ... removing them thus
             * implements the principle of least astonishment.
             */
            myLogEntryTableViewer.getTable().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (!myLogEntryTableViewer.getTable().isDisposed()) {
                        if (logEntry.getPlatformIdentifer().equals(getPlatform())) {
                            myLogEntryTableViewer.remove(logEntry);
                        }
                    }
                }
            });
            /**
             * If (the message of) the deleted log entry is displayed, clear the displayed message.
             */
            if (logEntry.equals(getDisplayedLogEntry())) {
                myLogEntryTableViewer.getTable().getDisplay().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (!myMessageTextArea.isDisposed()) {
                            displayLogEntry(null);
                        }
                    }
                });
            }
        }
    };

    private ServiceRegistryPublisherAccess serviceRegistryAccess;

    private Display display;

    public LogView() {
        myInstance = this;
    }

    public static LogView getInstance() {
        return myInstance;
    }

    @Override
    public void createPartControl(final Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        // filter = level selection, platform selection, and search text field
        Composite filterComposite = new Composite(parent, SWT.NONE);
        filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        filterComposite.setLayout(new RowLayout());

        createLevelArrangement(filterComposite);
        createPlatformListingArrangement(filterComposite);
        createSearchArrangement(filterComposite);

        // sash = table and text display
        Composite sashComposite = new Composite(parent, SWT.NONE);
        sashComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sashComposite.setLayout(new GridLayout(1, false));

        SashForm sashForm = new SashForm(sashComposite, SWT.VERTICAL | SWT.SMOOTH);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createTableArrangement(sashForm);
        createTableDynamics();
        createTextAreaArrangement(sashForm);

        sashForm.setWeights(new int[] { 3, 1 });
        sashForm.setSashWidth(7);

        // add sorting functionality
        myTableColumnSorter = new LogTableColumnSorter();
        myLogEntryTableViewer.setSorter(myTableColumnSorter);

        // add change listeners and filter mechanism
        myListenerAndFilter = new LogTableFilter(this, myLogEntryTableViewer);

        mySearchTextField.addKeyListener(myListenerAndFilter);

        myCheckboxError.addSelectionListener(myListenerAndFilter);
        myCheckboxInfo.addSelectionListener(myListenerAndFilter);
        myCheckboxWarn.addSelectionListener(myListenerAndFilter);
        myPlatformCombo.addSelectionListener(myListenerAndFilter);

        myLogEntryTableViewer.addFilter(myListenerAndFilter);

        initActions();

        // add toolbar actions (right top of view)
        for (Action action : createToolbarActions()) {
            getViewSite().getActionBars().getToolBarManager().add(action);
        }

        getSite().setSelectionProvider(myLogEntryTableViewer);

        hookContextMenu();

        // disable copy action when table or selection is empty
        myLogEntryTableViewer.getTable().addMenuDetectListener(new MenuDetectListener() {

            @Override
            public void menuDetected(MenuDetectEvent arg0) {
                if (myLogEntryTableViewer.getSelection().isEmpty() || myLogEntryTableViewer.getTable().getItemCount() == 0) {
                    copyAction.setEnabled(false);
                } else {
                    copyAction.setEnabled(true);
                }
            }
        });

        // store display reference for access by topology listener
        display = parent.getShell().getDisplay();
        registerWorkflowHostSetListener();
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * @param display
     */
    private void registerWorkflowHostSetListener() {

        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        serviceRegistryAccess.registerService(WorkflowHostSetListener.class, new WorkflowHostSetListener() {

            @Override
            public void onReachableWorkflowHostsChanged(Set<InstanceNodeSessionId> reachableWfHosts,
                Set<InstanceNodeSessionId> addedWfHosts, Set<InstanceNodeSessionId> removedWfHosts) {

                final List<InstanceNodeSessionId> nodeIds = LogModel.getInstance().updateListOfLogSources();
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        refreshPlatformCombo(nodeIds);
                    }
                });
            }
        });

    }

    /**
     * Triggers asynchronous refresh (worker thread calls UI thread) of the view only for the given platform.
     * 
     * @param platform Current selected platform.
     */
    private void asyncRefresh(final InstanceNodeSessionId platform) {
        myLogEntryTableViewer.getTable().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                if (!myLogEntryTableViewer.getTable().isDisposed()) {
                    try {
                        if (platform.equals(getPlatform())) {
                            displayLogEntry(null);
                            myLogEntryTableViewer.getTable().clearAll();
                            myLogEntryTableViewer.refresh();
                        }
                    } catch (SWTException e) {
                        // re-throw the exception if the disposal state is NOT the error
                        if (!myLogEntryTableViewer.getTable().isDisposed()) {
                            throw e;
                        }
                    }
                }
            }
        });
    }

    public boolean getErrorSelection() {
        return myCheckboxError.getSelection();
    }

    public boolean getInfoSelection() {
        return myCheckboxInfo.getSelection();
    }

    public boolean getWarnSelection() {
        return myCheckboxWarn.getSelection();
    }

    public InstanceNodeSessionId getPlatform() {
        return (InstanceNodeSessionId) myPlatformCombo.getData(myPlatformCombo.getItem(myPlatformCombo.getSelectionIndex()));
    }

    /**
     * Method is deleting not allowed searchMessages.
     * 
     * @return Returns the searchMessage
     * @author Mark Geiger
     */
    public String getSearchText() {

        boolean setVisible = false;
        boolean containsRow = false;

        String ret = "";

        if (adjustedSearchMessage) {
            adjustedSearchMessage = false;
            setVisible = true;
        }
        do {
            containsRow = false;
            int inRow = 0;
            int rowStartIdx = 0;
            ret = mySearchTextField.getText().toString();
            for (int i = 0; i < ret.length(); i++) {
                if (ret.charAt(i) == '*') {
                    if (inRow == 0) {
                        rowStartIdx = i;
                    }
                    inRow++;
                } else {
                    inRow = 0;
                }

                if (inRow > 1) {
                    setVisible = true;
                    containsRow = true;
                    // cut of '*******'
                    mySearchTextField.setText(
                        mySearchTextField.getText().toString().substring(0, rowStartIdx)
                            + mySearchTextField.getText().toString().substring(i, ret.length()));

                    mySearchTextField.setSelection(rowStartIdx + 1);
                    adjustedSearchMessage = true;
                    break;
                }
            }
        } while (containsRow);
        searchWarning.setVisible(setVisible);
        return ret;
    }

    @Override
    public void setFocus() {
        myLogEntryTableViewer.getControl().setFocus();
    }

    /**
     * 
     * Create the composite structure with level selection options.
     * 
     * @param filterComposite Parent composite for the level selection options.
     */
    private void createLevelArrangement(Composite filterComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = NO_SPACE;
        filterComposite.setLayout(rowLayout);

        // ERROR checkbox
        myCheckboxError = new Button(filterComposite, SWT.CHECK);
        myCheckboxError.setText(Messages.error);
        myCheckboxError.setSelection(ERROR_PRESELECTED);

        // WARN checkbox
        myCheckboxWarn = new Button(filterComposite, SWT.CHECK);
        myCheckboxWarn.setText(Messages.warn);
        myCheckboxWarn.setSelection(WARN_PRESELECTED);

        // INFO checkbox
        myCheckboxInfo = new Button(filterComposite, SWT.CHECK);
        myCheckboxInfo.setText(Messages.info);
        myCheckboxInfo.setSelection(INFO_PRESELECTED);

    }

    /**
     * 
     * Create the composite structure with platform selection options.
     * 
     * @param platformComposite Parent composite for the platform selection options.
     */
    private void createPlatformListingArrangement(Composite platformComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = NO_SPACE;
        platformComposite.setLayout(rowLayout);

        // platform listing combo box
        myPlatformCombo = new Combo(platformComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (InstanceNodeSessionId nodeId : LogModel.getInstance().updateListOfLogSources()) {
            myPlatformCombo.add(nodeId.getAssociatedDisplayName());
            myPlatformCombo.setData(nodeId.getAssociatedDisplayName(), nodeId);
        }

        myPlatformCombo.select(0);
        myPlatformCombo.setLayoutData(new RowData(PLATFORM_WIDTH, SWT.DEFAULT));

        LogModel.getInstance().setSelectedLogSource((InstanceNodeSessionId) myPlatformCombo.getData(
            myPlatformCombo.getItem(myPlatformCombo.getSelectionIndex())));
    }

    /**
     * 
     * Create the composite structure with search options.
     * 
     * @param searchComposite Parent composite for the search options.
     */
    private void createSearchArrangement(Composite searchComposite) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = 7;
        rowLayout.center = true;
        rowLayout.fill = true;
        searchComposite.setLayout(rowLayout);

        // search text field
        mySearchTextField = new Text(searchComposite, SWT.SEARCH);
        mySearchTextField.setMessage(Messages.search);
        mySearchTextField.setSize(TEXT_WIDTH, SWT.DEFAULT);
        mySearchTextField.setLayoutData(new RowData(TEXT_WIDTH, SWT.DEFAULT));

        Composite warningComp = new Composite(searchComposite, 0);
        warningComp.setLayout(rowLayout);

        searchWarning = new Label(warningComp, SWT.CENTER | SWT.BORDER);
        searchWarning.setVisible(false);

        searchWarning.setBackground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_GERALDINE));
        searchWarning.setFont(new Font(null, new FontData("TimesNewRoman", 8, 1)));
        searchWarning.setText(" Only one ' * ' in a row allowed ");
    }

    /**
     * 
     * Create the composite structure with log display and selection options.
     * 
     * @param platformComposite Parent composite for log display and selection options.
     */
    private void createTableArrangement(Composite tableComposite) {
        tableComposite.setLayout(new GridLayout());

        // create table viewer
        myLogEntryTableViewer = new TableViewer(tableComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
        myLogEntryTableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // create table header and column styles
        String[] columnTitles = new String[] {
            Messages.level,
            Messages.message,
            Messages.bundle,
            Messages.platform,
            Messages.timestamp };
        int[] bounds = { COLUMN_WIDTH_LEVEL, COLUMN_WIDTH_MESSAGE, COLUMN_WIDTH_BUNDLE, COLUMN_WIDTH_PLATFORM, COLUMN_WIDTH_TIME };

        for (int i = 0; i < columnTitles.length; i++) {
            // for all columns

            final int index = i;
            final TableViewerColumn viewerColumn = new TableViewerColumn(myLogEntryTableViewer, SWT.NONE);
            final TableColumn column = viewerColumn.getColumn();

            // set column properties
            column.setText(columnTitles[i]);
            column.setWidth(bounds[i]);
            column.setResizable(true);
            column.setMoveable(true);

            column.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    myTableColumnSorter.setColumn(index);
                    int direction = myLogEntryTableViewer.getTable().getSortDirection();

                    if (myLogEntryTableViewer.getTable().getSortColumn() == column) {
                        if (direction == SWT.UP) {
                            direction = SWT.DOWN;
                        } else {
                            direction = SWT.UP;
                        }
                    } else {
                        direction = SWT.UP;
                    }
                    myLogEntryTableViewer.getTable().setSortDirection(direction);
                    myLogEntryTableViewer.getTable().setSortColumn(column);

                    myLogEntryTableViewer.getTable().clearAll();
                    myLogEntryTableViewer.refresh();
                }
            });
        }

        // set table content
        myLogEntryTableViewer.setContentProvider(new LogContentProvider());
        myLogEntryTableViewer.setLabelProvider(new LogLabelProvider());
        myLogEntryTableViewer.setInput(LogModel.getInstance());

        // set table layout data
        final Table table = myLogEntryTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // for the selection of a table item
        table.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                TableItem[] selection = table.getSelection();
                if (selection.length > 0) {
                    if (selection[0].getData() instanceof SerializableLogEntry) {
                        SerializableLogEntry log = (SerializableLogEntry) selection[0].getData();
                        displayLogEntry(log);
                    }
                }
            }
        });

        myLogEntryTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection().isEmpty()) {
                    displayLogEntry(null);
                }
            }
        });
    }

    private void hookContextMenu() {
        MenuManager menuManager = new MenuManager();
        menuManager.add(clearAction);
        menuManager.add(copyAction);
        Menu menu = menuManager.createContextMenu(myLogEntryTableViewer.getTable());
        myLogEntryTableViewer.getTable().setMenu(menu);
        getSite().registerContextMenu(menuManager, myLogEntryTableViewer);
        getSite().setSelectionProvider(myLogEntryTableViewer);
    }

    private void createTableDynamics() {
        LogModel.getInstance().addListener(listener);
    }

    private SerializableLogEntry getDisplayedLogEntry() {
        return displayedLogEntry;
    }

    protected void displayLogEntry(final SerializableLogEntry logEntry) {
        StringBuffer buffer = new StringBuffer();
        if (logEntry != null) {
            buffer.append(logEntry.getMessage().replaceAll(SerializableLogEntry.RCE_SEPARATOR, "\n"));
            if (logEntry.getException() != null) {
                if (buffer.length() == 0) {
                    buffer.append("(no message)");
                }
                if (logEntry.getException() != null && !logEntry.getException().isEmpty()) {
                    // note: there were line breaks added here before, but that doesn't make sense with compacted exceptions anymore
                    if (buffer.charAt(buffer.length() - 1) == ':') {
                        // avoid double colons
                        buffer.append(" ");
                    } else {
                        buffer.append(": ");
                    }
                    buffer.append(logEntry.getException());
                }
            }
        }
        String text = buffer.toString();
        text = WordUtils.wrap(text, TEXT_AREA_WORD_WRAP_WIDTH, "\n", false);
        myMessageTextArea.setText(text);
        displayedLogEntry = logEntry;
    }

    @Override
    public void dispose() {
        LogModel.getInstance().removeListener(listener);
        super.dispose();
        if (serviceRegistryAccess != null) {
            serviceRegistryAccess.dispose();
        }

    }

    /**
     * 
     * Create the composite structure for expansive text displaying.
     * 
     * @param textAreaComposite Parent composite for expansive text displaying.
     */
    private void createTextAreaArrangement(Composite textAreaComposite) {
        textAreaComposite.setLayout(new GridLayout(1, false));

        myMessageTextArea = new Text(textAreaComposite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        myMessageTextArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        myMessageTextArea.setEditable(false);
    }

    private Action[] createToolbarActions() {
        return new Action[] {
            scrollLockAction, clearAction
        };
    }

    protected void clear() {
        LogModel.getInstance().clear();
        asyncRefresh(getPlatform());
    }

    protected void setScrollLocked(final boolean scrollLocked) {
        this.scrollLocked = scrollLocked;
    }

    public boolean isScrollLocked() {
        return scrollLocked;
    }

    protected ImageDescriptor getScrollLockImageDescriptor() {
        final ImageDescriptor result;
        if (isScrollLocked()) {
            result = ImageManager.getInstance().getImageDescriptor(StandardImages.SCROLLOCK_ENABLED);
        } else {
            result = ImageManager.getInstance().getImageDescriptor(StandardImages.SCROLLOCK_DISABLED);
        }
        return result;
    }

    private void refreshPlatformCombo(List<InstanceNodeSessionId> nodeIds) {
        myPlatformCombo.removeAll();
        for (InstanceNodeSessionId nodeId : nodeIds) {
            myPlatformCombo.add(nodeId.getAssociatedDisplayName());
            myPlatformCombo.setData(nodeId.getAssociatedDisplayName(), nodeId);
        }

        LogModel logModel = LogModel.getInstance();
        logModel.updateListOfLogSources();
        // select platform (previously selected)
        InstanceNodeSessionId currentPlatform = logModel.getCurrentLogSource();
        if (currentPlatform != null) {
            String[] items = myPlatformCombo.getItems();
            for (int i = 0; i < items.length; i++) {
                if (myPlatformCombo.getData(items[i]).equals(currentPlatform)) {
                    myPlatformCombo.select(i);
                    return;
                }
            }
        }
        myPlatformCombo.select(0);
        logModel.setSelectedLogSource((InstanceNodeSessionId) myPlatformCombo.getData(myPlatformCombo.getItem(0)));
        asyncRefresh(logModel.getCurrentLogSource());
    }

    private void initActions() {

        clearAction = new Action(Messages.clear, ImageDescriptor.createFromImage(
            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE))) {

            @Override
            public void run() {
                clear();
            }
        };

        copyAction = new Action(Messages.copy, ImageDescriptor.createFromImage(
            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY))) {

            @Override
            public void run() {
                ISelection selection = myLogEntryTableViewer.getSelection();
                List<SerializableLogEntry> logEntries = new ArrayList<SerializableLogEntry>();
                if (selection != null && selection instanceof IStructuredSelection) {
                    IStructuredSelection sel = (IStructuredSelection) selection;
                    for (@SuppressWarnings("unchecked") Iterator<SerializableLogEntry> iterator = sel.iterator(); iterator.hasNext();) {
                        SerializableLogEntry logEntry = iterator.next();
                        logEntries.add(logEntry);
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (SerializableLogEntry logEntry : logEntries) {
                    sb.append(logEntry.toString() + System.getProperty("line.separator")); //$NON-NLS-1$
                }
                if (sb.length() > 0) {
                    ClipboardHelper.setContent(sb.toString());
                }
            }
        };

        scrollLockAction = new Action(Messages.scrollLock, SWT.TOGGLE) {

            {
                setImageDescriptor(getScrollLockImageDescriptor());
            }

            @Override
            public void run() {
                setScrollLocked(!isScrollLocked());
                setImageDescriptor(getScrollLockImageDescriptor());
            }
        };
    }
}
