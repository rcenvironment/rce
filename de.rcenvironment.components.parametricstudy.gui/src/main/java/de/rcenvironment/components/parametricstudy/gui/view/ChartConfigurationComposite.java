/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.nebula.visualization.xygraph.dataprovider.AbstractDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.ISample;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.figures.Axis;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.PointStyle;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.TraceType;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Tree;

import de.rcenvironment.components.parametricstudy.common.Dimension;
import de.rcenvironment.components.parametricstudy.common.Measure;
import de.rcenvironment.components.parametricstudy.common.StudyDataset;
import de.rcenvironment.components.parametricstudy.gui.view.StudyDatastore.StudyDatasetAddListener;
import de.rcenvironment.core.gui.utils.common.configuration.BeanConfigurationDialog;
import de.rcenvironment.core.gui.utils.common.configuration.BeanConfigurationSourceAdapter;
import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewer;
import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerContentProvider;
import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerLabelProvider;

/**
 * The {@link Composite} displaying a {@link ChartConfiguration}.
 * 
 * @author Christian Weiss
 */
public class ChartConfigurationComposite extends Composite implements
    ISelectionProvider {

    private static final int WEIGHT = 100;

    private static final int WEIGHT_CHART = 66;

    /** The backing {@link ChartConfiguration}. */
    private final ChartConfiguration configuration = new ChartConfiguration();

    /** The data providers for the {@link Trace}s. */
    private final Set<DataProvider> dataProviders = new HashSet<DataProvider>();

    /** The {@link XYGraph}. */
    private XYGraph graph;

    /** The chart canvas. */
    private Canvas chartCanvas;

    /** The lightweight system displaying the chart. */
    private LightweightSystem lightweightSystem;

    /** The {@link StudyDatastore}. */
    private StudyDatastore studyDatastore;

    /** The tree viewer. */
    private ConfigurationViewer treeViewer;

    /** The content provider. */
    private final ConfigurationViewerContentProvider contentProvider = new ConfigurationViewerContentProvider();

    /** The tree. */
    @SuppressWarnings("unused")
    private Tree tree;

    /** The traces. */
    private final List<Trace> traces = new LinkedList<Trace>();

    /** The selection changed listeners. */
    private final List<ISelectionChangedListener> selectionChangedListeners = new LinkedList<ISelectionChangedListener>();

    /** The selection. */
    private ISelection selection;

    /**
     * Instantiates a new chart configuration composite.
     * 
     * @param parent the parent
     * @param style the style
     */
    public ChartConfigurationComposite(Composite parent, int style) {
        super(parent, style);
        BeanConfigurationSourceAdapter.initialize();
    }

    /**
     * The {@link Action} displayed in the context menu of a trace providing the option to remove
     * the trace from the chart.
     * 
     * @author Christian Weiss
     */
    private static final class RemoveTraceAction extends Action implements
        ConfigurationViewer.VisibilityAction {

        /** The trace. */
        private ChartConfiguration.Trace trace;

        /** The visible. */
        private boolean visible;

        /**
         * Instantiates a new removes the trace action.
         */
        private RemoveTraceAction() {
            super(Messages.removeTraceActionLabel);
        }

        /**
         * Sets the trace.
         * 
         * @param trace the new trace
         */
        private void setTrace(final ChartConfiguration.Trace trace) {
            this.trace = trace;
        }

        /**
         * Returns the trace.
         * 
         * @return the trace
         */
        private ChartConfiguration.Trace getTrace() {
            return trace;
        }

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewer.VisibilityAction#setVisible(boolean)
         */
        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewer.VisibilityAction#isVisible()
         */
        @Override
        public boolean isVisible() {
            return visible;
        };

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.action.Action#run()
         */
        @Override
        public void run() {
            getTrace().getChartConfiguration().removeTrace(trace);
        }

    }

    /**
     * Creates the controls.
     */
    public void createControls() {
        // layout
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.marginBottom = 5;
        setLayout(layout);
        GridData layoutData;
        // sash
        final SashForm sash = new SashForm(this, SWT.HORIZONTAL);
        layoutData = new GridData(GridData.FILL_BOTH);
        sash.setLayoutData(layoutData);
        // chart configuration
        treeViewer = new ConfigurationViewer(sash);
        final RemoveTraceAction removeTraceAction = new RemoveTraceAction();
        treeViewer.addContextMenuItem(removeTraceAction);
        // update the trace registered at the RemoveTraceAction and its
        // 'enabled'-state upon selection changes (null - no trace selected)
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                final ISelection newSelection = event.getSelection();
                if (newSelection instanceof IStructuredSelection) {
                    final IStructuredSelection structuredSelection = (IStructuredSelection) newSelection;
                    final Object element = structuredSelection
                        .getFirstElement();
                    if (element instanceof ChartConfiguration.Trace) {
                        final ChartConfiguration.Trace trace = (ChartConfiguration.Trace) element;
                        removeTraceAction.setTrace(trace);
                        removeTraceAction.setEnabled(true);
                        removeTraceAction.setVisible(true);
                    } else {
                        removeTraceAction.setTrace(null);
                        removeTraceAction.setEnabled(false);
                        removeTraceAction.setVisible(false);
                    }
                }
            }

        });
        tree = treeViewer.getTree();
        // layoutData = new GridData(GridData.FILL_BOTH);
        // tree.setLayoutData(layoutData);
        treeViewer.setAutoExpandLevel(2);
        // chart
        chartCanvas = new Canvas(sash, SWT.NONE);
        lightweightSystem = new LightweightSystem(chartCanvas);
        sash.setWeights(new int[] { WEIGHT - WEIGHT_CHART, WEIGHT_CHART });
        final Button addTraceButton = new Button(this, SWT.NONE);
        addTraceButton.setText(Messages.addTraceButtonLabel);
        addTraceButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                final BeanConfigurationDialog dialog = new BeanConfigurationDialog(
                    Display.getCurrent().getActiveShell());
                final ChartConfiguration.Trace trace = new ChartConfiguration.Trace(
                    configuration);
                dialog.setObject(trace);
                dialog.create();
                // change the title of the shall upon changes of the 'name'
                // property
                trace.addPropertyChangeListener("name",
                    new PropertyChangeListener() {

                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            try {
                                dialog.getShell().setText(trace.toString());
                            } catch (RuntimeException e) {
                                e = null;
                            }
                        }

                    });
                if (dialog.open() == IDialogConstants.OK_ID) {
                    if (trace.getXAxis() != null && trace.getYAxis() != null) {
                        configuration.addTrace(trace);
                    } else {
                        MessageBox errorDialog =
                            new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
                        errorDialog.setText(Messages.noDataErrorTitle);
                        errorDialog.setMessage(Messages.noDataError);
                        errorDialog.open();
                    }
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        for (final DataProvider dataProvider : dataProviders) {
            try {
                dataProvider.close();
            } catch (RuntimeException e) {
                e = null;
            }
        }
        dataProviders.clear();
        // remove the viewer from the content provider
        contentProvider.removeViewer(treeViewer);
    }

    /**
     * Sets the study datastore.
     * 
     * @param studyDatastore the new study datastore
     */
    public void setStudyDatastore(final StudyDatastore studyDatastore) {
        if (this.studyDatastore == studyDatastore) {
            return;
        }
        if (this.studyDatastore == null) {
            this.studyDatastore = studyDatastore;
            // link viewer to content
            treeViewer.setLabelProvider(new ConfigurationViewerLabelProvider());
            treeViewer.setContentProvider(contentProvider);
            contentProvider.addViewer(treeViewer);
            //
            initializeConfiguration();
            //
            treeViewer.setInput(configuration);
        }
    }

    /**
     * Initialize configuration.
     * 
     * @param studyDatastore the study datastore
     */
    private void initializeConfiguration() {
        configuration.setTitle(studyDatastore.getTitle());
        for (final Dimension dimension : studyDatastore.getStructure().getDimensions()) {
            final ChartConfiguration.XAxis xAxis = new ChartConfiguration.XAxis();
            xAxis.setTitle(dimension.getName());
            xAxis.setAutoScale(true);
            configuration.addXAxis(xAxis);
        }
        for (final Measure measure : studyDatastore.getStructure().getMeasures()) {
            final ChartConfiguration.YAxis yAxis = new ChartConfiguration.YAxis();
            yAxis.setTitle(measure.getName());
            yAxis.setAutoScale(true);
            configuration.addYAxis(yAxis);
        }
        // for (final StudyStructure.Dimension dimension : studyDatastore
        // .getStructure().getDimensions()) {
        // for (final StudyStructure.Measure measure : studyDatastore
        // .getStructure().getMeasures()) {
        // final ChartConfiguration.Trace trace = new ChartConfiguration.Trace(
        // configuration);
        // trace.setName(StringUtils.format("%s - %s", dimension.getName(),
        // measure.getName()));
        // trace.setXAxis(configuration.getXAxis(dimension.getName()));
        // trace.setYAxis(configuration.getYAxis(measure.getName()));
        // configuration.addTrace(trace);
        // }
        // }
        updateGraph();
        setSelection(new StructuredSelection(configuration));
    }

    /**
     * Update graph.
     */
    private void updateGraph() {
        graph = new XYGraph();
        // add a generic property change listener to pipe through changes
        configuration
            .addPropertyChangeListener(new PipedPropertyChangeListener(
                graph));
        // copy values from configuration
        graph.setTitle(configuration.getTitle());
        graph.setShowTitle(configuration.getShowTitle());
        graph.setShowLegend(configuration.isShowLegend());
        // return early, if there are no datasets
        if (studyDatastore.getDatasetCount() == 0) {
            return;
        }
        createXAxes();
        createYAxes();
        createTraces();
        configuration.addPropertyChangeListener("traces",
            new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    createTraces();
                    chartCanvas.update();
                }

            });
        lightweightSystem.setContents(graph);
    }

    /**
     * Creates the x axes.
     */
    private void createXAxes() {
        boolean first = true;
        for (final ChartConfiguration.XAxis xAxis : configuration.getXAxes()) {
            final Axis axis;
            if (first) {
                first = false;
                axis = graph.primaryXAxis;
                axis.setTitle(xAxis.getTitle());
                axis.setYAxis(false);
            } else {
                axis = new Axis(xAxis.getTitle(), false);
                graph.addAxis(axis);
            }
            // register piping property change listener
            final PipedPropertyChangeListener propertyChangeListener = new PipedPropertyChangeListener(
                axis);
            propertyChangeListener.addGetterNameMapping("logScale",
                "logScaleEnabled");
            xAxis.addPropertyChangeListener(propertyChangeListener);
            // copy values from configuation
            axis.setAutoFormat(xAxis.isAutoFormat());
            axis.setAutoScale(xAxis.isAutoScale());
        }
    }

    /**
     * Creates the y axes.
     */
    private void createYAxes() {
        boolean first = true;
        for (final ChartConfiguration.YAxis yAxis : configuration.getYAxes()) {
            final Axis axis;
            if (first) {
                first = false;
                axis = graph.primaryYAxis;
                axis.setTitle(yAxis.getTitle());
                axis.setYAxis(true);
            } else {
                axis = new Axis(yAxis.getTitle(), true);
                graph.addAxis(axis);
            }
            // register piping property change listener
            final PipedPropertyChangeListener propertyChangeListener = new PipedPropertyChangeListener(
                axis);
            propertyChangeListener.addGetterNameMapping("logScale",
                "logScaleEnabled");
            yAxis.addPropertyChangeListener(propertyChangeListener);
            // copy values from configuation
            axis.setAutoFormat(yAxis.isAutoFormat());
            axis.setAutoScale(yAxis.isAutoScale());
        }
    }

    /**
     * Creates the traces.
     */
    private void createTraces() {

        // remove existing traces
        for (final Trace trace : traces) {
            graph.removeTrace(trace);
        }
        final PropertyChangeListener axisListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                createTraces();
            }

        };
        // create new traces based on configuration
        for (final ChartConfiguration.Trace configurationTrace : configuration
            .getTraces()) {
            // register property change listeners to update the whole traces
            // (including their value stores) upon axis changes, this is
            // required to reinitialize the value stores
            configurationTrace.addPropertyChangeListener(
                ChartConfiguration.Trace.PROPERTY_X_AXIS, axisListener);
            configurationTrace.addPropertyChangeListener(
                ChartConfiguration.Trace.PROPERTY_Y_AXIS, axisListener);
            //
            final Dimension dimension = studyDatastore
                .getStructure().getDimension(
                    configurationTrace.getXAxis().getTitle());
            final Measure measure = studyDatastore
                .getStructure().getMeasure(
                    configurationTrace.getYAxis().getTitle());

            final DataProvider dataProvider = new DataProvider(dimension,
                measure);
            dataProviders.add(dataProvider);
            dataProvider.initialize();
            // FIXME: close dataProviders
            final Trace graphTrace = new Trace(configurationTrace.getName(), //
                getAxis(false, configurationTrace.getXAxis().getTitle()), //
                getAxis(true, configurationTrace.getYAxis().getTitle()), //
                dataProvider);
            // register piping property change listener
            final PipedPropertyChangeListener propertyChangeListener = new PipedPropertyChangeListener(
                graphTrace);
            propertyChangeListener.addNameMapping("type", "traceType");
            propertyChangeListener.addNameMapping("color", "traceColor");
            propertyChangeListener.addConverterMapping("color",
                new PipedPropertyChangeListener.AbstractConverter() {

                    @Override
                    public Object convert(Object object) {
                        final RGB rgb = (RGB) object;
                        final org.eclipse.swt.graphics.Color swtColor = new org.eclipse.swt.graphics.Color(
                            Display.getDefault(), rgb);
                        return swtColor;
                    }

                });
            configurationTrace
                .addPropertyChangeListener(propertyChangeListener);
            // graphTrace.addPropertyChangeListener(propertyChangeListener.reverse());
            // set the trace type
            final TraceType traceType = configurationTrace.getType();
            configurationTrace.setType(traceType);
            graphTrace.setTraceType(traceType);
            // set the trace color
            final RGB color = configurationTrace.getColor();
            if (color != null) {
                configurationTrace.setColor(color);
                graphTrace
                    .setTraceColor(new Color(Display.getDefault(), color));
            }
            // set the point style
            graphTrace.setPointStyle(PointStyle.CROSS);
            // add the trace to the locally managed list of traces, required for
            // deletion of all traces of the graph
            traces.add(graphTrace);
            // add the trace to the XYGraph instance
            graph.addTrace(graphTrace);
            // if the trace had no color, adding it will have issued an
            // arbitrary one which has to be taken over into the configuration
            if (color == null) {
                configurationTrace
                    .setColor(graphTrace.getTraceColor().getRGB());
            }
        }

    }

    /**
     * Returns the axis.
     * 
     * @param yAxis the y axis
     * @param name the name
     * @return the axis
     */
    private Axis getAxis(final boolean yAxis, final String name) {
        for (final Axis axis : graph.getAxisList()) {
            if (yAxis == axis.isYAxis() && name.equals(axis.getTitle())) {
                return axis;
            }
        }
        return null;
    }

    /**
     * The Class DataProvider.
     */
    private final class DataProvider extends AbstractDataProvider {

        /** The samples. */
        private final List<ISample> samples = new LinkedList<ISample>();

        /** The dimension. */
        private final Dimension dimension;

        /** The measure. */
        private final Measure measure;

        /** The min x. */
        private double minX;

        /** The max x. */
        private double maxX;

        /** The min y. */
        private double minY;

        /** The max y. */
        private double maxY;

        /** The listener. */
        private final StudyDatasetAddListener listener = new StudyDatasetAddListener() {

            @Override
            public void handleStudyDatasetAdd(StudyDataset dataset) {
                addDataset(dataset);
            }

        };

        /**
         * Instantiates a new data provider.
         * 
         * @param dimension the dimension
         * @param measure the measure
         */
        private DataProvider(final Dimension dimension, final Measure measure) {
            super(false);
            if (dimension == null || measure == null) {
                throw new IllegalArgumentException();
            }
            this.dimension = dimension;
            this.measure = measure;
        }

        /**
         * Initialize.
         */
        private void initialize() {
            studyDatastore.addDatasetAddListener(listener);
            for (final StudyDataset dataset : studyDatastore.getDatasets()) {
                addDataset(dataset);
            }
        }

        /**
         * Adds the dataset.
         * 
         * @param dataset the dataset
         */
        private void addDataset(StudyDataset dataset) {
            Number valueX = dataset.getValue(dimension.getName(),
                Number.class);
            Number valueY = dataset.getValue(measure.getName(),
                Number.class);
            if (valueX != null && valueY != null) {

                final double xdata = valueX.doubleValue();
                final double ydata = valueY.doubleValue();
                final Sample sample = new Sample(xdata, ydata);
                addSample(sample);
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        fireDataChange();
                    }
                });
            }
        }

        /**
         * Adds the sample.
         * 
         * @param sample the sample
         */
        private void addSample(final ISample sample) {
            // avoid duplicates
            for (final ISample storedSample : samples) {
                if (sample.getXValue() == storedSample.getXValue()) {
                    return;
                }
            }
            boolean dataChanged = false;
            if (samples.size() == 0) {
                minX = sample.getXValue();
                maxX = sample.getXValue();
                minY = sample.getYValue();
                maxY = sample.getYValue();
                dataChanged = true;
            } else {
                if (sample.getXValue() < minX) {
                    minX = sample.getXValue();
                    dataChanged = true;
                }
                if (sample.getXValue() > maxX) {
                    maxX = sample.getXValue();
                    dataChanged = true;
                }
                if (sample.getYValue() < minY) {
                    minY = sample.getYValue();
                    dataChanged = true;
                }
                if (sample.getYValue() > maxY) {
                    maxY = sample.getYValue();
                    dataChanged = true;
                }
            }
            samples.add(sample);
            Collections.sort(samples, SampleComparator.INSTANCE);
            if (dataChanged) {
                updateDataRange();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.nebula.visualization.xygraph.dataprovider.AbstractDataProvider#getSize()
         */
        @Override
        public int getSize() {
            return samples.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.nebula.visualization.xygraph.dataprovider.AbstractDataProvider#getSample(int)
         */
        @Override
        public ISample getSample(int index) {
            return samples.get(index);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.nebula.visualization.xygraph.dataprovider.AbstractDataProvider#innerUpdate()
         */
        @Override
        protected void innerUpdate() {}

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.nebula.visualization.xygraph.dataprovider.AbstractDataProvider#updateDataRange()
         */
        @Override
        protected void updateDataRange() {
            xDataMinMax = new Range(minX, maxX);
            yDataMinMax = new Range(minY, maxY);
        }

        /**
         * Close.
         */
        private void close() {
            studyDatastore.removeDatasetAddListener(listener);
        }

    }

    /**
     * The Class SampleComparator.
     */
    private static final class SampleComparator implements Comparator<ISample> {

        /** The sole instance. */
        private static final SampleComparator INSTANCE = new SampleComparator();

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(ISample sample1, ISample sample2) {
            return Double.compare(sample1.getXValue(), sample2.getXValue());
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
     */
    @Override
    public ISelection getSelection() {
        return selection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void removeSelectionChangedListener(
        ISelectionChangedListener listener) {
        selectionChangedListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void setSelection(ISelection selection) {
        this.selection = selection;
        final SelectionChangedEvent event = new SelectionChangedEvent(this,
            selection);
        for (final ISelectionChangedListener listener : selectionChangedListeners) {
            try {
                listener.selectionChanged(event);
            } catch (RuntimeException e) {
                e = null;
            }
        }
    }

}
