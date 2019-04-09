/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Panel;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.model.api.ComponentSize;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.view.ComponentRuntimeView;
import de.rcenvironment.core.gui.workflow.view.properties.ComponentInstancePropertySource;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchAggregator;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchProcessor;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Read-only EditPart representing a WorkflowNode, storing additional workflow execution information.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Martin Misiak
 */
public class WorkflowRunNodePart extends ReadOnlyWorkflowNodePart {

    private static final int INTEGER_15 = 15;

    private static final int INTEGER_16 = 16;

    private static final int INTEGER_20 = 20;

    private static final int INTEGER_22 = 22;

    private static final Log LOGGER = LogFactory.getLog(WorkflowRunNodePart.class);

    // It is intended that the component state figure is updated at most every 400msec (can be
    // adapted on demand).
    // Thus, the max batch size is large - seid_do
    private static final int MAX_BATCH_SIZE = 10000;

    private static final long MAX_BATCH_LATENCY_MSEC = 400;

    private static final long UNDEFINED = -1;

    private final NotificationSubscriber stateChangeListener;

    private final BatchAggregator<ComponentState> batchAggregator;

    private ComponentStateFigure stateFigure;

    private boolean initializeStatusTriggered = false;

    private volatile long lastStateNotificationNumber = UNDEFINED;

    private volatile long lastIterationCountNotificationNumber = UNDEFINED;

    private Label runCountLabel;

    public WorkflowRunNodePart() {
        stateChangeListener = new ComponentStateChangeListener(this);
        BatchProcessor<ComponentState> batchProcessor = new BatchProcessor<ComponentState>() {

            @Override
            public void processBatch(final List<ComponentState> batch) {
                updateComponentState(batch.get(batch.size() - 1));
            }

        };
        batchAggregator = ConcurrencyUtils.getFactory().createBatchAggregator(MAX_BATCH_SIZE, MAX_BATCH_LATENCY_MSEC, batchProcessor);
    }

    @Override
    public void refresh() {
        super.refresh();
        synchronized (batchAggregator) {
            if (!initializeStatusTriggered) {
                Job job = new Job(StringUtils.format(Messages.initializingComponentState, ((WorkflowNode) getModel()).getName())) {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        initializeStatus();
                        return Status.OK_STATUS;
                    }
                };
                job.schedule();
                // currently, there is no kind of retry, thus, submitting the job calling
                // initializeStatus() is seen as initialization was
                // performed not matter it was successful or not
                initializeStatusTriggered = true;
            }
        }
    }

    @Override
    protected String generateTooltipText() {
        return generateTooltipTextBase((WorkflowNode) getModel());
    }

    @Override
    protected IFigure createFigure() {
        // get the plain figure from the parent implementation
        final IFigure figure = super.createBaseFigure();
        // enhance the figure with an activity display element
        stateFigure = new ComponentStateFigureImpl();
        createExecutionCountLabel();
        ComponentInterface ci =
            ((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentInterface();
        if (ci.getShape() == ComponentShape.CIRCLE) {
            stateFigure.setBounds(new Rectangle(1, 1, INTEGER_22, INTEGER_22));
            final int x = 19;
            final int executionLabelWidth = 15;
            runCountLabel.setBounds(new Rectangle(x, 2, executionLabelWidth, INTEGER_15));
            informationFigure.setBounds(new Rectangle(INTEGER_22, INTEGER_20, INTEGER_16, INTEGER_16));
        } else if (ci.getSize() == ComponentSize.SMALL) {
            final int xy = -2;
            stateFigure.setBounds(new Rectangle(xy, xy, INTEGER_22, INTEGER_22));
            final int x = 16;
            final int y = -1;
            final int executionLabelWidth = 21;
            runCountLabel.setBounds(new Rectangle(x, y, executionLabelWidth, INTEGER_15));
            informationFigure.setBounds(new Rectangle(INTEGER_22, INTEGER_22, INTEGER_16, INTEGER_16));
        } else {
            final int x = 22;
            final int executionLabelWidth = 50;
            runCountLabel.setBounds(new Rectangle(x, 1, executionLabelWidth, INTEGER_15));
            stateFigure.setBounds(new Rectangle(0, 0, INTEGER_22, INTEGER_22));
        }
        figure.add(stateFigure);
        figure.add(runCountLabel);
        figure.add(informationFigure);

        return figure;
    }

    private void createExecutionCountLabel() {
        runCountLabel = new Label("-");
        runCountLabel.setTextPlacement(PositionConstants.NORTH_EAST);
        runCountLabel.setTextAlignment(PositionConstants.RIGHT);
        runCountLabel.setLabelAlignment(PositionConstants.RIGHT);
        runCountLabel.setVisible(true);
        runCountLabel.setOpaque(false);
        runCountLabel.setToolTip(new Label("Runs: -"));
    }

    @Override
    public void performRequest(Request req) {
        if (req.getType().equals(RequestConstants.REQ_OPEN)) {

            Job job = new Job("Opening view") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask("Retrieving workflow information", 2);
                        monitor.worked(1);
                        monitor.worked(2);
                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                openDefaultView();
                            }
                        });
                        return Status.OK_STATUS;
                    } finally {
                        monitor.done();
                    }
                };
            };
            job.setUser(true);
            job.schedule();
        }
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IPropertySource.class) {
            return new ComponentInstancePropertySource(getWorkflowExecutionInformation(),
                ((WorkflowNode) getModel()).getIdentifierAsObject());
        }
        return super.getAdapter(type);
    }

    private WorkflowExecutionInformation getWorkflowExecutionInformation() {
        return (WorkflowExecutionInformation) ((WorkflowExecutionInformationPart) ((WorkflowPart) getParent()).getParent()).getModel();
    }

    private ComponentExecutionInformation getComponentExecutionInformation() {
        return getWorkflowExecutionInformation().getComponentExecutionInformation(((WorkflowNode) getModel()).getIdentifierAsObject());
    }

    private void initializeStatus() {
        try {
            final ComponentExecutionInformation compExeInfo = getComponentExecutionInformation();
            final String stateNotifId = ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeInfo.getExecutionIdentifier();
            final String iterationCountNotifId = ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX
                + compExeInfo.getExecutionIdentifier();
            final LogicalNodeId ctrlerNode = getWorkflowExecutionInformation().getNodeId();
            final DistributedNotificationService notificationService =
                ServiceRegistry.createAccessFor(this).getService(DistributedNotificationService.class);
            notificationService.subscribe(stateNotifId, stateChangeListener, ctrlerNode);
            final List<Notification> stateNotifications = notificationService.getNotifications(stateNotifId, ctrlerNode).get(stateNotifId);
            if (stateNotifications != null && stateNotifications.size() > 0) {
                handleStateNotification(getLastNonDisposedStateNotification(stateNotifications));
            }
            notificationService.subscribe(iterationCountNotifId, stateChangeListener, ctrlerNode);
            final List<Notification> iterationCountNotifs = notificationService.getNotifications(iterationCountNotifId,
                ctrlerNode).get(iterationCountNotifId);
            if (iterationCountNotifs != null && iterationCountNotifs.size() > 0) {
                handleExecutionCountNotification(iterationCountNotifs.get(iterationCountNotifs.size() - 1));
            }
        } catch (NullPointerException e) {
            // TODO review: can this still occur after 7.0.0 error handling changes?
            LOGGER.error("Could not initialize status.", e);
        } catch (RemoteOperationException e) {
            LOGGER.error("Failed to register workflow state change listeners: " + e.getMessage());
        }
    }

    private Notification getLastNonDisposedStateNotification(List<Notification> stateNotifications) {
        for (int i = 1; i <= stateNotifications.size(); i++) {
            Notification notification = stateNotifications.get(stateNotifications.size() - i);
            ComponentState state = ComponentState.valueOf((String) notification.getBody());
            if (state != ComponentState.DISPOSED && state != ComponentState.DISPOSING) {
                return notification;
            }
        }
        return stateNotifications.get(stateNotifications.size() - 1);
    }

    private void openDefaultView() {
        final ComponentExecutionInformation cid = getComponentExecutionInformation();

        IExtensionRegistry extReg = Platform.getExtensionRegistry();
        IConfigurationElement[] confElements =
            extReg.getConfigurationElementsFor("de.rcenvironment.core.gui.workflow.monitoring"); //$NON-NLS-1$
        IConfigurationElement[] viewConfElements =
            extReg.getConfigurationElementsFor("org.eclipse.ui.views"); //$NON-NLS-1$

        boolean foundRuntimeView = false;

        for (final IConfigurationElement confElement : confElements) {
            if (cid.getComponentIdentifier().startsWith(confElement.getAttribute("component"))
                && confElement.getAttribute("default") != null
                && Boolean.TRUE.toString().matches(confElement.getAttribute("default"))) { //$NON-NLS-1$
                for (final IConfigurationElement viewConfElement : viewConfElements) {
                    if (viewConfElement.getAttribute("id").equals(confElement.getAttribute("view"))) {
                        try {
                            final IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
                                viewConfElement.getAttribute("class"),
                                cid.getExecutionIdentifier(), IWorkbenchPage.VIEW_VISIBLE); // $NON-NLS-1$
                            ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                                @Override
                                @TaskDescription("Initialize component runtime view data")
                                public void run() {
                                    ((ComponentRuntimeView) view).initializeData(cid);
                                    Display.getDefault().asyncExec(new Runnable() {

                                        @Override
                                        public void run() {
                                            ((ComponentRuntimeView) view).initializeView();
                                        }
                                    });
                                }
                            });

                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(view);
                            foundRuntimeView = true;
                            break;
                        } catch (PartInitException e) {
                            throw new RuntimeException(e);
                        } catch (InvalidRegistryObjectException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        if (!foundRuntimeView) {
            // If not existent: open properties view
            try {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
                    "org.eclipse.ui.views.PropertySheet"); //$NON-NLS-1$
            } catch (PartInitException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected synchronized void handleStateNotification(Notification notification) {
        long notificationNumber = notification.getHeader().getNumber();
        if (notificationNumber < lastStateNotificationNumber) {
            return;
        }
        try {
            final ComponentState state = ComponentState.valueOf((String) notification.getBody());
            if (state != null && state != ComponentState.DISPOSING && state != ComponentState.DISPOSED) {
                batchAggregator.enqueue(state);
                lastStateNotificationNumber = notificationNumber;
            }
        } catch (IllegalArgumentException e) {
            e = null;
            // notification is ignored because notification doesn't contain ComponentState, e.g. if
            // last notification is sent before publisher is removed
        }
    }

    protected void updateComponentState(final ComponentState state) {
        if (stateFigure != null) {

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    stateFigure.setState(state);
                    stateFigure.setToolTip(new Label("State: " + state.getDisplayName()));
                }
            });
        }
    }

    protected synchronized void handleExecutionCountNotification(final Notification notification) {
        long notificationNumber = notification.getHeader().getNumber();
        if (notificationNumber < lastIterationCountNotificationNumber) {
            return;
        }
        if (notification.getBody() instanceof String) {
            final String[] counts = StringUtils.splitAndUnescape((String) notification.getBody());
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (counts.length == 1) {
                        runCountLabel.setText(counts[0]);
                        runCountLabel.setToolTip(new Label("Runs: " + counts[0]));
                    } else {
                        runCountLabel.setText(String.valueOf(counts.length) + "/"
                            + String.valueOf(Integer.parseInt(counts[counts.length - 1]) - Integer.parseInt(counts[counts.length - 2])));
                        int innerLoopCount = 1;
                        int previousCount = 0;
                        String countString = "";
                        for (String count : counts) {
                            int countInt = Integer.parseInt(count);
                            countString += innerLoopCount + "/" + (countInt - previousCount) + "\n";
                            innerLoopCount++;
                            previousCount = countInt;
                        }
                        runCountLabel.setToolTip(new Label("Nested loop run/component runs\n"
                            + countString
                            + "Total component runs: " + counts[counts.length - 1]));
                    }
                }
            });
            lastIterationCountNotificationNumber = notificationNumber;
        }
    }

    /**
     * Indicates the state of {@link Component}s via a figure.
     * 
     * @author Chrisitian Weiss
     */
    public interface ComponentStateFigure extends IFigure {

        /**
         * @param state of {@link Component}.
         */
        void setState(ComponentState state);

        /**
         * @return state of {@link Component}.
         */
        ComponentState getState();

    }

    /**
     * Implementation of {@link ComponentStateFigure}.
     * 
     * @author Christian Weiss
     */
    public final class ComponentStateFigureImpl extends Panel implements ComponentStateFigure {

        private final ImageFigure innerImageFigure = new ImageFigure(Activator.getInstance().getImageRegistry()
            .get(ComponentState.UNKNOWN.name()));
        {
            innerImageFigure.setOpaque(false);
            add(innerImageFigure);
            setVisible(true);
            setOpaque(false);
        }

        private ComponentState state;

        @Override
        public void setBounds(final Rectangle rect) {
            super.setBounds(rect);
            final Rectangle innerRectangleBounds = new Rectangle(rect.x + 3, rect.y + 3, rect.width - 6, rect.height - 6);
            innerImageFigure.setBounds(innerRectangleBounds);
        }

        @Override
        public void setState(ComponentState state) {
            if (this.state == state) {
                return;
            }
            this.state = state;

            Image stateImage = Activator.getInstance().getImageRegistry().get(state.name());
            if (stateImage != null) {
                innerImageFigure.setImage(stateImage);
            } else {
                innerImageFigure.setImage(Activator.getInstance().getImageRegistry().get(ComponentState.UNKNOWN.name()));
            }
            innerImageFigure.setOpaque(false);
            setVisible(true);
            setOpaque(false);

            refresh();
        }

        @Override
        protected void paintFigure(Graphics graphics) {}

        @Override
        public ComponentState getState() {
            return state;
        }

    }

}
