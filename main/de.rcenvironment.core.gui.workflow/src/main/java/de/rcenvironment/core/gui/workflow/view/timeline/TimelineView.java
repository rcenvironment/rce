/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.timeline;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.commons.ComponentRunInterval;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * The complete Timeline View.
 * 
 * @author Hendrik Abbenhaus
 * @author David Scholz
 * 
 */
public class TimelineView extends ViewPart implements AreaChangedListener, ResizeListener, ControlListener, SelectionListener {

    /** Identifier used to find the this view within the workbench. */
    public static final String ID = "de.rcenvironment.gui.TimeTable";

    private static final Map<String, Image> COMPONENT_ICON_CACHE = new HashMap<>();

    private Composite rootComposite = null;

    private TimelineNavigationControl navigation = null;

    private SashForm sashMiddle = null;

    private TimelineComponentList list = null;

    private TimelineComponentRow[] rows = new TimelineComponentRow[0];

    private Slider scrollBar = null;

    private final int scrollBarMaximum = 10000;

    private Action refreshAction = null;

    private Action zoomInAction = null;

    private Action zoomOutAction = null;

    private Action filterAction = null;

    private Date wfStartDate = null;

    private Date wfEndDate = null;

    private Label workflowNameLabel = null;

    private String[] allowedComponentNames = null;

    private final Color backgroundColorWhite = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

    private final int defaultMaximumZoomValue = 10000;

    private final int defaultMinimumZoomValue = 45;

    private int currentZoomValue = defaultMaximumZoomValue;

    private MetaDataService metaDataService;

    private Long workflowDmId;

    private ResolvableNodeId workflowCtrlNode;

    private boolean workflowTerminated = false;

    private Composite parentComposite;

    private boolean actualContentSet = false;

    private Label refreshLabel;

    public TimelineView() {
        metaDataService = ServiceRegistry.createAccessFor(this).getService(MetaDataService.class);
    }

    @Override
    public void createPartControl(Composite parent) {
        parentComposite = parent;
        parentComposite.setBackground(backgroundColorWhite);

        refreshLabel = new Label(parentComposite, SWT.NONE);
        refreshLabel.setText("Fetching workflow timeline...");

        rootComposite = new Composite(new Shell(), SWT.NONE);
        rootComposite.setBackground(backgroundColorWhite);
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 0;
        mainLayout.marginBottom = 0;
        mainLayout.verticalSpacing = 0;
        rootComposite.setLayout(mainLayout);
        // For itself
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        gridData.verticalSpan = 1;
        gridData.grabExcessVerticalSpace = true;

        rootComposite.setLayoutData(gridData);

        makeActions(rootComposite);
        initGUI(rootComposite);
        contributeToActionBars();

        rootComposite.setEnabled(false);
    }

    private void initGUI(Composite parent) {
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = GridData.BEGINNING;
        gridData.verticalSpan = 1;
        gridData.grabExcessVerticalSpace = false;

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;

        Composite titleComposite = new Composite(parent, SWT.None);
        titleComposite.setLayoutData(gridData);
        titleComposite.setBackground(backgroundColorWhite);
        titleComposite.setLayout(gridLayout);

        Composite workflowName = new Composite(titleComposite, SWT.None);
        workflowName.setLayoutData(gridData);
        workflowName.setBackground(backgroundColorWhite);
        workflowName.setLayout(new RowLayout());
        workflowNameLabel = new Label(workflowName, SWT.None);
        workflowNameLabel.setBackground(backgroundColorWhite);

        Composite colorLegend = new Composite(titleComposite, SWT.RIGHT_TO_LEFT);
        colorLegend.setLayoutData(gridData);
        colorLegend.setBackground(backgroundColorWhite);
        colorLegend.setLayout(new RowLayout());

        for (TimelineActivityType current : TimelineActivityType.values()) {
            if (current.getColor() != null) {
                Label label = new Label(colorLegend, SWT.None);
                label.setBackground(backgroundColorWhite);
                label.setText("   " + current.getDisplayName() + " ");
                Label colorLabel = new Label(colorLegend, SWT.None);
                colorLabel.setText("      ");
                // FIXME: the newly created color object will never be disposed
                colorLabel.setBackground(new Color(label.getDisplay(), current.getColor()));
            }
        }

        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        gridData.verticalSpan = 1;
        gridData.grabExcessVerticalSpace = true;

        SashForm master = new SashForm(parent, SWT.VERTICAL);
        master.setSashWidth(2);
        // master.setBackground(backgroundColorWhite);
        master.setLayoutData(gridData);

        initTopGUI(master);
        initBottomGUI(master);

        master.setWeights(new int[] { 5, 1 });

    }

    private void initTopGUI(Composite parent) {
        Composite master = new Composite(parent, SWT.NONE);
        master.setBackground(backgroundColorWhite);
        master.setLayout(new GridLayout(1, true));
        // Top:
        list = new TimelineComponentList(master);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalSpan = 1;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        list.setLayoutData(gridData);
        list.addResizeListener(this);
        list.setBackground(backgroundColorWhite);

        // bottom:
        sashMiddle = new SashForm(master, SWT.HORIZONTAL);
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalSpan = 1;
        sashMiddle.setLayoutData(gridData);

        // left:
        Composite zoomComposite = new Composite(sashMiddle, SWT.NONE);
        zoomComposite.setBackground(backgroundColorWhite);
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        zoomComposite.setLayout(layout);

        // right:
        scrollBar = new Slider(sashMiddle, SWT.HORIZONTAL);

        scrollBar.setMaximum(scrollBarMaximum); // maximum
        scrollBar.setPageIncrement(scrollBarMaximum);
        scrollBar.setIncrement(10); // jump on press left or right
        scrollBar.setThumb(scrollBar.getMaximum());
        // SWT Slider does not always disable slider if value == maximum value
        // as at start when value == maximum
        scrollBar.setEnabled(false);
        scrollBar.setLayoutData(gridData);
        scrollBar.addControlListener(this);
        scrollBar.addSelectionListener(this);

        sashMiddle.setWeights(new int[] { 1, 7 });
    }

    private void initBottomGUI(Composite parent) {
        this.navigation = new TimelineNavigationControl(parent);
        navigation.setBackground(backgroundColorWhite);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 3;
        gridData.grabExcessHorizontalSpace = true;
        this.navigation.setLayoutData(gridData);
        this.navigation.addAreaChangeListener(this);
    }

    /**
     * Sets dm id and controller node of workflow related to this view instance.
     * 
     * @param wfDmId dm id of workflow run
     * @param wfCtrlNode a generic or specific id referring to the workflow controller instance
     */
    public void initialize(Long wfDmId, ResolvableNodeId wfCtrlNode) {
        workflowDmId = wfDmId;
        workflowCtrlNode = wfCtrlNode;
        updateContent();
    }

    private void updateContent() {
        refreshAction.setEnabled(false);
        Job job = new Job("Workflow Timeline") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Fetching workflow timeline", 2);
                    monitor.worked(1);
                    WorkflowRunTimline timeline = metaDataService.getWorkflowTimeline(workflowDmId, workflowCtrlNode);
                    final String timelineAsString = getWorkflowRunTimelineAsJsonString(timeline);
                    monitor.worked(1);
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            if (!parentComposite.isDisposed()) {
                                updateContent(timelineAsString);
                                if (!actualContentSet) { // replace placeholder content with actual content
                                    refreshLabel.setParent(new Shell());
                                    refreshLabel.dispose();
                                    rootComposite.setParent(parentComposite);
                                    rootComposite.getShell().layout(true, true);
                                    zoomInAction.setEnabled(true);
                                    zoomOutAction.setEnabled(true);
                                    filterAction.setEnabled(true);
                                    actualContentSet = true;
                                }
                                rootComposite.setEnabled(true);

                                // update the scrollbars (needed if new components were contributing to the timeline)
                                list.setMinSize(list.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
                                list.layout();

                                refreshAction.setEnabled(!workflowTerminated);
                            }
                        }
                    });
                } catch (IOException | CommunicationException e) {
                    LogFactory.getLog(TimelineView.class).error("Failed to load timeline of workflow", e);
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(TimelineView.this);
                            MessageDialog.openError(getSite().getShell(), "Workflow Timeline",
                                "Failed to load the timeline. Did you get disconnected from a remote instance?\n\n"
                                    + "Please refresh the workflow data browser and try again.\n\nSee log for more details.");
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

    private String getWorkflowRunTimelineAsJsonString(WorkflowRunTimline timeline) throws IOException {
        final String timeNodeName = "Time";
        final String typeNodeName = "Type";
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("WorkflowName", timeline.getWorkflowRunName());
        rootNode.put("WorkflowStartTime", String.valueOf(timeline.getWorkflowRunInterval().getStartTime()));
        if (timeline.getWorkflowRunInterval().getEndTime() != null) {
            rootNode.put("WorkflowEndTime", String.valueOf(timeline.getWorkflowRunInterval().getEndTime()));
            workflowTerminated = true;
        } else {
            rootNode.put("WorkflowEndTime", String.valueOf(System.currentTimeMillis()));
        }
        ArrayNode componentsArray = rootNode.putArray("Components");
        Map<String, ObjectNode> componentNodes = new HashMap<>();
        Map<String, SortedMap<Long, ObjectNode>> componentEventNodes = new HashMap<>();
        for (ComponentRunInterval cri : timeline.getComponentRunIntervalsSortedByTime()) {
            if (!componentNodes.containsKey(cri.getComponentInstanceName())) {
                ObjectNode componentNode = mapper.createObjectNode();
                componentNode.put("Name", cri.getComponentInstanceName());
                componentNode.put("Id", cri.getComponentID());
                componentNode.putArray("Events");
                componentNodes.put(cri.getComponentInstanceName(), componentNode);
            }
            if (!componentEventNodes.containsKey(cri.getComponentInstanceName())) {
                componentEventNodes.put(cri.getComponentInstanceName(), new TreeMap<Long, ObjectNode>());
            }
            SortedMap<Long, ObjectNode> sortedEventNodes = componentEventNodes.get(cri.getComponentInstanceName());
            if (cri.getType() == TimelineIntervalType.COMPONENT_RUN) {
                ObjectNode eventNode = mapper.createObjectNode();
                eventNode.put(timeNodeName, String.valueOf(cri.getStartTime()));
                eventNode.put(typeNodeName, String.valueOf(cri.getType().name()));
                sortedEventNodes.put(cri.getStartTime(), eventNode);
                if (cri.getEndTime() != null) {
                    ObjectNode waitEventNode = mapper.createObjectNode();
                    waitEventNode.put(timeNodeName, String.valueOf(cri.getEndTime()));
                    waitEventNode.put(typeNodeName, "COMPONENT_WAIT");
                    sortedEventNodes.put(cri.getEndTime(), waitEventNode);
                }
            } else {
                ObjectNode eventNode = mapper.createObjectNode();
                eventNode.put(timeNodeName, String.valueOf(cri.getStartTime()));
                eventNode.put(typeNodeName, String.valueOf(cri.getType().name()));
                sortedEventNodes.put(cri.getStartTime(), eventNode);
                // second check: sanity check if tool run end and component run end are equal
                if (cri.getEndTime() != null && !sortedEventNodes.containsKey(cri.getEndTime())) {
                    ObjectNode runEventNode = mapper.createObjectNode();
                    runEventNode.put(timeNodeName, String.valueOf(cri.getEndTime()));
                    runEventNode.put(typeNodeName, TimelineIntervalType.COMPONENT_RUN.name());
                    sortedEventNodes.put(cri.getEndTime(), runEventNode);
                }
            }
        }

        for (String compInstanceName : componentNodes.keySet()) {
            ArrayNode eventsNode = (ArrayNode) componentNodes.get(compInstanceName).get("Events");
            for (ObjectNode eventNode : componentEventNodes.get(compInstanceName).values()) {
                eventsNode.add(eventNode);
            }
            componentsArray.add(componentNodes.get(compInstanceName));
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return writer.writeValueAsString(rootNode);
    }

    /**
     * Updates the Content of the View.
     * 
     * @param resource the resource
     */
    @SuppressWarnings("unchecked")
    public void updateContent(String resource) {
        // delete all!
        this.list.clear();

        // read json file

        try {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

            Map<String, Object> jsonStructureRoot = mapper.readValue(resource,
                new HashMap<String, Object>().getClass());
            String jsonWorkflowName = (String) jsonStructureRoot.get(TimelineViewConstants.JSON_WORKFLOWNAME);
            String jsonWorkflowStart = (String) jsonStructureRoot
                .get(TimelineViewConstants.JSON_WORKFLOWSTARTTIME);
            String jsonWorkflowEnd = (String) jsonStructureRoot
                .get(TimelineViewConstants.JSON_WORKFLOWENDTIME);
            List<Object> jsonComponentList = (ArrayList<Object>) jsonStructureRoot
                .get(TimelineViewConstants.JSON_COMPONENTS);
            if (jsonWorkflowName == null || jsonWorkflowName.equals("")
                || jsonWorkflowStart == null || jsonWorkflowStart.equals("")
                || jsonWorkflowEnd == null || jsonWorkflowEnd.equals("")
                || jsonComponentList == null || jsonComponentList.isEmpty()) {
                return;
            }
            workflowNameLabel.setText(jsonWorkflowName);
            workflowNameLabel.pack();
            this.wfStartDate = new Date();
            this.wfStartDate.setTime(Long.valueOf(jsonWorkflowStart)
                .longValue());
            this.wfEndDate = new Date();
            this.wfEndDate.setTime(Long.valueOf(jsonWorkflowEnd).longValue());
            setVisibleArea(wfStartDate, wfEndDate);
            // set start end endtime to list composite legend
            // list.setWorkflowStartEndTime(wfStartDate, wfEndDate);
            this.navigation.setWorflowStartEndTime(wfStartDate, wfEndDate);
            List<TimelineComponentRow> currentRows = new ArrayList<TimelineComponentRow>();

            for (Object jsonCurrentComponentObject : jsonComponentList) {
                Map<String, Object> jsonCurrentComponent = (Map<String, Object>) jsonCurrentComponentObject;
                String currentComponentName = (String) jsonCurrentComponent
                    .get(TimelineViewConstants.JSON_COMPONENT_NAME);
                String currentComponentID = (String) jsonCurrentComponent
                    .get(TimelineViewConstants.JSON_COMPONENT_ID);
                TimelineComponentRow currentRow = new TimelineComponentRow(
                    currentComponentName, currentComponentID, wfStartDate,
                    wfEndDate);

                List<Object> jsonActivityList = (ArrayList<Object>) jsonCurrentComponent
                    .get(TimelineViewConstants.JSON_COMPONENT_EVENTS);

                Collections.sort(jsonActivityList, new Comparator<Object>() {

                    /*
                     * (non-Javadoc)
                     * 
                     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
                     */
                    @Override
                    public int compare(Object o1, Object o2) {
                        Map<String, String> compare1 = (HashMap<String, String>) o1;
                        Map<String, String> compare2 = (HashMap<String, String>) o2;
                        return compare1
                            .get(TimelineViewConstants.JSON_ACTIVITYTIME)
                            .compareTo(
                                compare2.get(TimelineViewConstants.JSON_ACTIVITYTIME));
                    }
                });
                List<TimelineActivityPart> currentActivities = new ArrayList<TimelineActivityPart>();
                int run = 1;
                for (Object jsonCurrentActivity : jsonActivityList) {
                    Map<String, String> currentActivityMap = (Map<String, String>) jsonCurrentActivity;

                    Date currentActivityTime = new Date();
                    long activityTime = Long.valueOf(currentActivityMap
                        .get(TimelineViewConstants.JSON_ACTIVITYTIME));
                    currentActivityTime.setTime(activityTime);

                    String currentEventType = currentActivityMap
                        .get(TimelineViewConstants.JSON_ACTIVITYTYPE);
                    TimelineActivityType eventType = TimelineActivityType
                        .valueOfjsonName(currentEventType);
                    String comment = null;
                    if (currentActivityMap
                        .containsKey(TimelineViewConstants.JSON_COMPONENT_EVENT_INFOTEXT)) {
                        comment = currentActivityMap
                            .get(TimelineViewConstants.JSON_COMPONENT_EVENT_INFOTEXT);
                    }
                    TimelineActivityPart currentActivity = null;
                    if (eventType == TimelineActivityType.COMPONENT_RUN) {
                        currentActivity = new TimelineActivityPart(currentComponentName,
                            eventType, currentActivityTime, String.valueOf(run++), comment);
                    } else {
                        currentActivity = new TimelineActivityPart(currentComponentName,
                            eventType, currentActivityTime, comment);
                    }

                    if (currentActivities.size() == 0
                        && !currentActivityTime.equals(wfStartDate)) {
                        TimelineActivityPart currentFirstActivity = new TimelineActivityPart(currentComponentName,
                            TimelineActivityType.WAITING, wfStartDate,
                            null);
                        currentFirstActivity.setEndtime(currentActivityTime);
                        if (currentActivities.size() > 0) {
                            currentActivities.get(currentActivities.size() - 1)
                                .setEndtime(currentActivityTime);
                        }
                        currentActivities.add(currentFirstActivity);
                    }
                    if (currentActivities.size() > 0) {
                        currentActivities.get(currentActivities.size() - 1)
                            .setEndtime(currentActivityTime);
                    }
                    currentActivities.add(currentActivity);
                }
                currentRow.setActivities(currentActivities
                    .toArray(new TimelineActivityPart[currentActivities
                        .size()]));
                currentRow.setWorkflowStartTime(wfStartDate);
                currentRow.setWorkflowEndTime(wfEndDate);
                currentRows.add(currentRow);
            }
            rows = currentRows.toArray(new TimelineComponentRow[currentRows
                .size()]);

        } catch (IOException e) {
            LogFactory.getLog(TimelineView.class).error(e);
            return;
        }
        showComponentRows(this.rows);
    }

    private void setVisibleArea(Date visibleStartTime, Date visibleEndTime) {
        this.list.setTimeArea(visibleStartTime, visibleEndTime);
        // if (!mouseDown) {
        this.navigation.setVisibleArea(visibleStartTime, visibleEndTime);
        // }
    }

    private void showComponentRows(TimelineComponentRow[] newrows) {
        this.navigation.setWorflowStartEndTime(wfStartDate, wfEndDate);
        this.navigation.setTimeTableComponentRows(filter(newrows,
            this.allowedComponentNames));
        this.list.setTimeTableComponentRows(filter(newrows,
            this.allowedComponentNames));
    }

    private TimelineComponentRow[] filter(TimelineComponentRow[] oldrows,
        String[] allowed) {
        if (allowed == null) {
            return oldrows;
        }
        List<TimelineComponentRow> showRows = new ArrayList<TimelineComponentRow>();
        for (TimelineComponentRow currentRow : oldrows) {
            if (filterContains(currentRow.getName(), allowed)) {
                showRows.add(currentRow);
            }
        }
        return showRows.toArray(new TimelineComponentRow[showRows.size()]);
    }

    private boolean filterContains(String name, String[] filter) {
        for (String filterKey : filter) {
            if (filterKey.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void makeActions(final Composite parent) {

        refreshAction = new Action("Refresh") {

            @Override
            public void run() {
                updateContent();
            }

        };
        refreshAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.REFRESH_16));
        refreshAction.setEnabled(false);

        zoomInAction = new Action(Messages.zoomin) {

            @Override
            public void run() {
                int maxZoom = defaultMaximumZoomValue / 10;
                int minZoom = currentZoomValue - maxZoom;
                if (minZoom == 0) {
                    setZoom(defaultMinimumZoomValue);
                } else if (minZoom > 0) {
                    setZoom(minZoom);
                } else if (minZoom < 0) {
                    setZoom(defaultMinimumZoomValue);
                }

            }
        };
        zoomInAction.setImageDescriptor(ImageDescriptor
            .createFromURL(TimelineView.class
                .getResource("/resources/icons/zoom_in.gif")));
        zoomInAction.setEnabled(false);

        zoomOutAction = new Action(Messages.zoomout) {

            @Override
            public void run() {
                int maxZoom = defaultMaximumZoomValue / 10;
                int minZoom = defaultMaximumZoomValue - currentZoomValue;
                if (currentZoomValue + maxZoom <= defaultMaximumZoomValue) {
                    setZoom(currentZoomValue + maxZoom);
                } else if (minZoom > 0 && minZoom < maxZoom) {
                    setZoom(currentZoomValue + minZoom);
                }
            }
        };
        zoomOutAction.setImageDescriptor(ImageDescriptor
            .createFromURL(TimelineView.class
                .getResource("/resources/icons/zoom_out.gif")));
        zoomOutAction.setEnabled(false);

        filterAction = new Action("Filter") {

            @Override
            public void run() {
                if (allowedComponentNames == null) {
                    List<String> currentList = new ArrayList<String>();
                    for (TimelineComponentRow currentRow : rows) {
                        currentList.add(currentRow.getName());
                    }
                    allowedComponentNames = currentList
                        .toArray(new String[currentList.size()]);
                }
                TimelineFilterDialog dialog = new TimelineFilterDialog(
                    Display.getCurrent().getActiveShell(),
                    allowedComponentNames);
                dialog.create();
                dialog.setContent(rows);
                if (dialog.open() == 0) { // 0 = ok
                    allowedComponentNames = dialog.getFilteredNames();
                    showComponentRows(rows);
                }

            }
        };
        filterAction.setImageDescriptor(ImageDescriptor
            .createFromURL(TimelineView.class
                .getResource("/resources/icons/filter.gif")));
        filterAction.setEnabled(false);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager manager = bars.getToolBarManager();

        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(zoomOutAction);
        manager.add(zoomInAction);
        manager.add(new Separator());
        manager.add(filterAction);
        manager.add(new Separator());
    }

    /**
     * Get an component Icon by its component type Id.
     * 
     * @param identifier the id
     * @param caller the caller
     * @return the image
     */
    public static Image getImageIconFromId(String identifier, Object caller) {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry
            .createAccessFor(caller);
        DistributedComponentKnowledgeService componentKnowledgeService = serviceRegistryAccess
            .getService(DistributedComponentKnowledgeService.class);
        Collection<ComponentInstallation> installations = componentKnowledgeService
            .getCurrentComponentKnowledge().getAllInstallations();
        for (ComponentInstallation installation : installations) {
            if (installation.getInstallationId().startsWith(identifier)) {
                if (!COMPONENT_ICON_CACHE.containsKey(installation.getInstallationId())) {
                    byte[] icon = installation.getComponentRevision().getComponentInterface().getIcon16();
                    Image image = ImageDescriptor.createFromImage(new Image(Display.getCurrent(),
                        new ByteArrayInputStream(icon))).createImage();
                    COMPONENT_ICON_CACHE.put(installation.getInstallationId(), image);
                }
                return COMPONENT_ICON_CACHE.get(installation.getInstallationId());
            }
        }
        return null;
    }

    /**
     * Get a component group Name by an id.
     * 
     * @param identifier the id
     * @param caller the caller
     * @return the display name
     */
    public static String getComponentNameFromId(String identifier, Object caller) {
        if (identifier == null) {
            return null;
        }
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry
            .createAccessFor(caller);
        DistributedComponentKnowledgeService componentKnowledgeService = serviceRegistryAccess
            .getService(DistributedComponentKnowledgeService.class);
        Collection<ComponentInstallation> installations = componentKnowledgeService
            .getCurrentComponentKnowledge().getAllInstallations();
        for (ComponentInstallation installation : installations) {
            if (installation.getInstallationId().startsWith(identifier)) {
                return installation.getComponentRevision().getComponentInterface().getDisplayName();
            }
        }
        return null;
    }

    /**
     * Calculates the current zoom level.
     * 
     * @param zoom The current zoomfactor 10000 == 100%
     */
    public void setZoom(int zoom) {
        final int complete = 10000;
        Date vET = this.navigation.getVisibleEndTime();
        Date vST = this.navigation.getVisibleStartTime();
        long diffVSET = vET.getTime() - vST.getTime();
        double middleTimeStamp = vST.getTime() + (diffVSET / 2);

        long startTime = wfStartDate.getTime();
        long endTime = wfEndDate.getTime();

        double currentAddValue = (zoom * (wfEndDate.getTime() - wfStartDate.getTime())) / (2 * complete);
        Date newStartDate = new Date((long) (middleTimeStamp - currentAddValue));
        Date newEndDate = new Date((long) (middleTimeStamp + currentAddValue));

        long newStartTime = newStartDate.getTime();
        long newEndTime = newEndDate.getTime();

        // handle the newTime, if it reaches above or below the workflowTime
        if (newStartTime < startTime) {
            long overTime = startTime - newStartTime;
            newEndDate = new Date(newEndTime + overTime);
            newStartDate = wfStartDate;
        } else if (newEndTime > endTime) {
            long overTime = endTime - newEndTime;
            newStartDate = new Date(newStartTime + overTime);
            newEndDate = wfEndDate;
        }

        navigation.setVisibleArea(newStartDate, newEndDate);
        navigation.notifyAreaChangeListener();
    }

    /**
     * Converts a Pixel to a Date.
     * 
     * @param xPixel Contains the pixel which should be converted to Date
     * @param canvasSizeX Contains the size of the Canvas in the x direction or the possible maximum size of selection
     * @param currentWorkflowStartTime Contains the start time of the current workflow
     * @param currentWorkflowEndTime Contains the end time of the current worklfow
     * @return the converted Date
     */
    public static Date convertPixelToDate(int xPixel, int canvasSizeX,
        Date currentWorkflowStartTime, Date currentWorkflowEndTime) {
        long value = ((currentWorkflowEndTime.getTime() * xPixel) + (currentWorkflowStartTime
            .getTime() * (canvasSizeX - xPixel))) / canvasSizeX;
        Date date = new Date();
        date.setTime(value);
        return date;
    }

    /**
     * 
     * Converts a Date to a pixel.
     * 
     * @param date the date
     * @param canvasSizeX the canvas size
     * @param currentWorkflowStartTime the current workflowstarttime
     * @param currentWorkflowEndTime the current workflowendtime
     * @return the return
     */
    public static int convertDateToPixel(Date date, int canvasSizeX,
        Date currentWorkflowStartTime, Date currentWorkflowEndTime) {
        long value = date.getTime();
        int xPixel = (int) (((value - currentWorkflowStartTime.getTime()) * canvasSizeX) / (currentWorkflowEndTime
            .getTime() - currentWorkflowStartTime.getTime()));
        return xPixel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {

    }

    @Override
    public void resized() {
        sashMiddle.setWeights(this.list.getWeights());
    }

    @Override
    public void controlMoved(ControlEvent arg0) {

    }

    @Override
    public void controlResized(ControlEvent arg0) {
        this.list.setWeights(sashMiddle.getWeights());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {

    }

    @Override
    public void widgetSelected(SelectionEvent arg0) {
        int begin = scrollBar.getMinimum() + scrollBar.getSelection();
        int end = scrollBar.getMinimum() + scrollBar.getSelection()
            + scrollBar.getThumb();
        setVisibleArea(
            convertPixelToDate(begin, scrollBar.getMaximum(), wfStartDate,
                wfEndDate),
            convertPixelToDate(end, scrollBar.getMaximum(), wfStartDate,
                wfEndDate));
    }

    @Override
    public void selectedAreaChanged(Date selectedStartTime, Date selectedEndTime) {
        final int percent = 100;
        this.list.setTimeArea(selectedStartTime, selectedEndTime);
        long selectedTime = selectedEndTime.getTime() - selectedStartTime.getTime();
        long wfTime = wfEndDate.getTime() - wfStartDate.getTime();
        float dividedTime = ((float) selectedTime / wfTime) * percent;
        currentZoomValue = Math.round(dividedTime) * percent;
        // SWT Slider does not always disable slider if value == maximum value
        this.scrollBar.setEnabled(currentZoomValue < scrollBar.getMaximum());

        if (currentZoomValue != 0) {
            this.scrollBar.setThumb(currentZoomValue);
        } else {
            this.scrollBar.setThumb(defaultMinimumZoomValue);
        }
        int beginSelection = convertDateToPixel(selectedStartTime,
            this.scrollBar.getMaximum(), wfStartDate, wfEndDate)
            - scrollBar.getMinimum();
        this.scrollBar.setSelection(beginSelection);
    }

}
