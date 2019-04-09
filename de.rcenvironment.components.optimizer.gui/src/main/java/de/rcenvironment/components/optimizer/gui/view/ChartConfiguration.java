/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.view;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.nebula.visualization.xygraph.figures.Trace.TraceType;
import org.eclipse.swt.graphics.RGB;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable;
import de.rcenvironment.core.utils.incubator.configuration.annotation.Configuration;

/**
 * The java bean holding the configuration of a chart to be displayed.
 * 
 * @author Christian Weiss
 */
@Configuration
public final class ChartConfiguration extends AbstractConfiguration {

    /** The default value whether or not the title of the chart is shown. */
    public static final boolean DEFAULT_SHOW_TITLE = true;

    private static final int NO_SUCH_ELEMENT_INDEX = -1;

    /** The serialVersionUID. */
    private static final long serialVersionUID = -1320466508913829555L;

    /** The x axes. */
    private final List<XAxis> xAxes = new LinkedList<XAxis>();

    /** The y axes. */
    private final List<YAxis> yAxes = new LinkedList<YAxis>();

    /** The traces. */
    private Trace[] traces = new Trace[0];

    /** The title. */
    private String title;

    /** The information whether or not to show the title. */
    private boolean showTitle = DEFAULT_SHOW_TITLE;

    /** The information whether or not to show the legend. */
    private boolean showLegend = true;

    /**
     * Returns the title.
     * 
     * @return the title
     */
    @Configurable
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     * 
     * @param title the new title
     */
    public void setTitle(final String title) {
        final String oldValue = this.title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }

    /**
     * Returns the information whether or not to show the title.
     * 
     * @return the show title
     */
    @Configurable
    public boolean getShowTitle() {
        return showTitle;
    }

    /**
     * Sets the information whether or not to show the title.
     * 
     * @param showTitle the new show title
     */
    public void setShowTitle(final boolean showTitle) {
        final boolean oldValue = this.showTitle;
        this.showTitle = showTitle;
        firePropertyChange("showTitle", oldValue, showTitle);
    }

    /**
     * Returns the information whether or not the legend shall be shown.
     * 
     * @return true, if is show legend
     */
    @Configurable
    public boolean isShowLegend() {
        return showLegend;
    }

    /**
     * Sets the information whether or not to show the legend.
     * 
     * @param showLegend the new show legend
     */
    public void setShowLegend(final boolean showLegend) {
        final boolean oldValue = this.showLegend;
        this.showLegend = showLegend;
        firePropertyChange("showLegend", oldValue, showLegend);
    }

    /**
     * Returns the x axis with the given title.
     * 
     * @param axisTitle the title
     * @return the x axis
     */
    public XAxis getXAxis(final String axisTitle) {
        for (final XAxis axis : xAxes) {
            if (axisTitle.equals(axis.getTitle())) {
                return axis;
            }
        }
        return null;
    }

    /**
     * Returns the x axes.
     * 
     * @return the x axes
     */
    @Configurable
    public XAxis[] getXAxes() {
        return xAxes.toArray(new XAxis[xAxes.size()]);
    }

    /**
     * Returns the x axis at the given index.
     * 
     * @param index the index
     * @return the x axes
     */
    public XAxis getXAxes(final int index) {
        return xAxes.get(index);
    }

    /**
     * Adds the x axis.
     * 
     * @param xaxis the xaxis
     */
    public void addXAxis(final XAxis xaxis) {
        if (xAxes.contains(xaxis) || getXAxis(xaxis.getTitle()) != null) {
            return;
        }
        this.xAxes.add(xaxis);
    }

    /**
     * Returns the y axis with the given title.
     * 
     * @param axisTitle the title
     * @return the y axis
     */
    public YAxis getYAxis(final String axisTitle) {
        for (final YAxis axis : yAxes) {
            if (axisTitle.equals(axis.getTitle())) {
                return axis;
            }
        }
        return null;
    }

    /**
     * Returns the y axes.
     * 
     * @return the y axes
     */
    @Configurable
    public YAxis[] getYAxes() {
        return yAxes.toArray(new YAxis[yAxes.size()]);
    }

    /**
     * Returns the y axis at the given index.
     * 
     * @param index the index
     * @return the y axes
     */
    public YAxis getYAxes(final int index) {
        return yAxes.get(index);
    }

    /**
     * Adds the y axis.
     * 
     * @param yaxis the yaxis
     */
    public void addYAxis(final YAxis yaxis) {
        if (yAxes.contains(yaxis) || getYAxis(yaxis.getTitle()) != null) {
            return;
        }
        this.yAxes.add(yaxis);
    }

    /**
     * Returns the trace with the given title.
     * 
     * @param traceTitle the title
     * @return the trace
     */
    public Trace getTrace(final String traceTitle) {
        for (final Trace trace : traces) {
            if (traceTitle.equals(trace.getName())) {
                return trace;
            }
        }
        return null;
    }

    /**
     * Returns the traces.
     * 
     * @return the traces
     */
    @Configurable
    public Trace[] getTraces() {
        return traces;
    }

    /**
     * Returns the trace at the given index.
     * 
     * @param index the index
     * @return the trace
     */
    public Trace getTrace(final int index) {
        return traces[index];
    }

    /**
     * Adds the trace.
     * 
     * @param trace the trace
     */
    public void addTrace(final Trace trace) {
        final Trace[] oldTraces = traces;
        final Trace[] newTraces = new Trace[oldTraces.length + 1];
        System.arraycopy(oldTraces, 0, newTraces, 0, oldTraces.length);
        newTraces[newTraces.length - 1] = trace;
        traces = newTraces;
        fireIndexedPropertyChange("traces", traces.length - 1, null, trace);
    }

    /**
     * Removes the trace.
     * 
     * @param trace the trace
     */
    public void removeTrace(final Trace trace) {
        final int index = getTraceIndex(trace);
        if (index >= 0) {
            final Trace[] oldTraces = traces;
            final Trace[] newTraces = new Trace[oldTraces.length - 1];
            System.arraycopy(oldTraces, 0, newTraces, 0, index);
            System.arraycopy(oldTraces, index + 1, newTraces, index,
                newTraces.length - index);
            traces = newTraces;
            firePropertyChange("traces", oldTraces, traces);
        }
    }

    /**
     * Returns the trace index.
     * 
     * @param trace the trace
     * @return the trace index
     */
    private int getTraceIndex(final Trace trace) {
        for (int index = 0; index < traces.length; ++index) {
            if (traces[index].equals(trace)) {
                return index;
            }
        }
        return NO_SUCH_ELEMENT_INDEX;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.rce.components.parametricstudy.gui.view.AbstractConfiguration#validate()
     */
    @Override
    public String validate() {
        String result = null;
        if (title == null || title.isEmpty() //
            || xAxes.size() == 0 //
            || yAxes.size() == 0 //
            || traces.length == 0) {
            result = "chart configuration is not valid";
        }
        if (result != null) {
            final List<AbstractConfiguration> elements = new LinkedList<AbstractConfiguration>();
            elements.addAll(xAxes);
            elements.addAll(yAxes);
            elements.addAll(Arrays.asList(traces));
            for (final Object element : elements) {
                if (!(element instanceof AbstractConfiguration)) {
                    continue;
                }
                result = ((AbstractConfiguration) element).validate();
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * The common base class for axes, holding the common configuration attributes.
     * 
     * @author Christian Weiss
     */
    @Configuration
    public abstract static class Axis extends AbstractConfiguration {

        /** The serialVersionUID. */
        private static final long serialVersionUID = -5175175964625896094L;

        /** The title. */
        private String title;

        /** The information whether or not the axis is visible. */
        private boolean visible = true;

        /** The information whether or not the axis shall use auto formating. */
        private boolean autoFormat = true;

        /** The information whether or not the axis shall use auto scaling. */
        private boolean autoScale = true;

        /** The information whether or not the axis shall use log scaling. */
        private boolean logScale = false;

        /**
         * Returns the title.
         * 
         * @return the title
         */
        @Configurable
        public String getTitle() {
            return title;
        }

        /**
         * Sets the title.
         * 
         * @param title the new title
         */
        public void setTitle(String title) {
            final String oldValue = this.title;
            this.title = title;
            firePropertyChange("title", oldValue, title);
        }

        /**
         * Checks if is visible.
         * 
         * @return true, if is visible
         */
        @Configurable
        public boolean isVisible() {
            return visible;
        }

        /**
         * Sets the visible.
         * 
         * @param visible the new visible
         */
        public void setVisible(boolean visible) {
            final boolean oldValue = this.visible;
            this.visible = visible;
            firePropertyChange("visible", oldValue, visible);
        }

        /**
         * Checks if is auto format.
         * 
         * @return true, if is auto format
         */
        @Configurable
        public boolean isAutoFormat() {
            return autoFormat;
        }

        /**
         * Sets the auto format.
         * 
         * @param autoFormat the new auto format
         */
        public void setAutoFormat(boolean autoFormat) {
            final boolean oldValue = this.autoFormat;
            this.autoFormat = autoFormat;
            firePropertyChange("autoFormat", oldValue, autoFormat);
        }

        /**
         * Checks if is auto scale.
         * 
         * @return true, if is auto scale
         */
        @Configurable
        public boolean isAutoScale() {
            return autoScale;
        }

        /**
         * Sets the auto scale.
         * 
         * @param autoScale the new auto scale
         */
        public void setAutoScale(boolean autoScale) {
            final boolean oldValue = this.autoScale;
            this.autoScale = autoScale;
            firePropertyChange("autoScale", oldValue, autoScale);
        }

        /**
         * Checks if is log scale.
         * 
         * @return true, if is log scale
         */
        @Configurable
        public boolean isLogScale() {
            return logScale;
        }

        /**
         * Sets the log scale.
         * 
         * @param logScale the new log scale
         */
        public void setLogScale(boolean logScale) {
            final boolean oldValue = this.logScale;
            this.logScale = logScale;
            firePropertyChange("logScale", oldValue, logScale);
        }

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.rce.components.parametricstudy.gui.view.AbstractConfiguration#validate()
         */
        @Override
        public String validate() {
            if (title == null || title.isEmpty()) {
                return "axis configuration is not valid";
            }
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return StringUtils.format("%s", title);
        }

    }

    /**
     * The class holding the configuration for an axis on the x (horizontal) dimension.
     * 
     * @author Chritian Weiss
     */
    public static final class XAxis extends Axis {

        /** The serialVersionUID. */
        private static final long serialVersionUID = -8467430361175681746L;

    }

    /**
     * The class holding the configuration for an axis on the y (vertical) dimension.
     */
    public static final class YAxis extends Axis {

        /** The serialVersionUID. */
        private static final long serialVersionUID = 6839843465063469066L;

    }

    /**
     * The class holding the configuration for a trace.
     */
    @Configuration
    public static final class Trace extends AbstractConfiguration {

        /** The Constant PROPERTY_VISIBLE. */
        public static final String PROPERTY_VISIBLE = "visible";

        /** The Constant PROPERTY_Y_AXIS. */
        public static final String PROPERTY_Y_AXIS = "yAxis";

        /** The Constant PROPERTY_X_AXIS. */
        public static final String PROPERTY_X_AXIS = "xAxis";

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = -6828180563766171017L;

        /**
         * The Enum Type.
         */
        public enum Type {

            /** Solid Line. */
            SOLID_LINE("Solid Line"),
            /** Dash Line. */
            DASH_LINE("Dash Line"),
            /** Point. */
            POINT("Point"),
            /** Bar. */
            BAR("Bar"), //
            /** Area. */
            AREA("Area"),
            /** Step Vertically. */
            STEP_VERTICALLY("Step Vertically"),
            /** Step Horizontally. */
            STEP_HORIZONTALLY("Step Horizontally");

            /** The title. */
            private final String title;

            /**
             * Instantiates a new type.
             * 
             * @param title the title
             */
            Type(final String title) {
                this.title = title;
            }

            /**
             * {@inheritDoc}
             * 
             * @see java.lang.Enum#toString()
             */
            @Override
            public String toString() {
                return title;
            }

        }

        /** The chart configuration. */
        private final ChartConfiguration chartConfiguration;

        /** The type. */
        private TraceType type = TraceType.SOLID_LINE;

        /** The color. */
        private RGB color;

        /** The name. */
        private String name = "";

        /** The x axis. */
        private XAxis xAxis;

        /** The y axis. */
        private YAxis yAxis;

        /** The visible. */
        private boolean visible = true;

        /**
         * Instantiates a new trace.
         * 
         * @param chartConfiguration the chart configuration
         */
        public Trace(final ChartConfiguration chartConfiguration) {
            this.chartConfiguration = chartConfiguration;
        }

        /**
         * Returns the chart configuration.
         * 
         * @return the chart configuration
         */
        public ChartConfiguration getChartConfiguration() {
            return chartConfiguration;
        }

        /**
         * Returns the type.
         * 
         * @return the type
         */
        @Configurable
        public TraceType getType() {
            return type;
        }

        /**
         * Sets the type.
         * 
         * @param type the new type
         */
        public void setType(TraceType type) {
            if (type == null) {
                throw new IllegalArgumentException();
            }
            final TraceType oldValue = this.type;
            this.type = type;
            firePropertyChange("type", oldValue, type);
        }

        /**
         * Returns the color.
         * 
         * @return the color
         */
        @Configurable
        public RGB getColor() {
            return color;
        }

        /**
         * Sets the color.
         * 
         * @param color the new color
         */
        public void setColor(RGB color) {
            if (color == null) {
                throw new IllegalArgumentException();
            }
            final RGB oldValue = this.color;
            this.color = color;
            firePropertyChange("color", oldValue, color);
        }

        /**
         * Returns the name.
         * 
         * @return the name
         */
        @Configurable
        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         * 
         * @param name the new name
         */
        public void setName(String name) {
            final String oldValue = this.name;
            this.name = name;
            firePropertyChange("name", oldValue, name);
        }

        /**
         * The Class XAxisSelectionProvider.
         */
        public static final class XAxisSelectionProvider implements
            Configurable.ValueProvider {

            /** The trace. */
            private Trace trace;

            /**
             * {@inheritDoc}
             * 
             * @see de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.ValueProvider#setObject(java.lang.Object)
             */
            @Override
            public void setObject(Object object) {
                trace = (Trace) object;
            }

            /**
             * {@inheritDoc}
             * 
             * @see de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.ValueProvider#getValues()
             */
            @Override
            public Object[] getValues() {
                return trace.getChartConfiguration().getXAxes();
            }

        }

        /**
         * Returns the x axis.
         * 
         * @return the x axis
         */
        @Configurable(valueProvider = XAxisSelectionProvider.class)
        public XAxis getXAxis() {
            return xAxis;
        }

        /**
         * Sets the x axis.
         * 
         * @param xAxisIn the new x axis
         */
        public void setXAxis(XAxis xAxisIn) {
            final XAxis oldValue = this.xAxis;
            this.xAxis = xAxisIn;
            firePropertyChange(PROPERTY_X_AXIS, oldValue, xAxisIn);
        }

        /**
         * The Class YAxisSelectionProvider.
         */
        public static final class YAxisSelectionProvider implements
            Configurable.ValueProvider {

            /** The trace. */
            private Trace trace;

            /**
             * {@inheritDoc}
             * 
             * @see de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.ValueProvider#setObject(java.lang.Object)
             */
            @Override
            public void setObject(Object object) {
                trace = (Trace) object;
            }

            /**
             * {@inheritDoc}
             * 
             * @see de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.ValueProvider#getValues()
             */
            @Override
            public Object[] getValues() {
                return trace.getChartConfiguration().getYAxes();
            }

        }

        /**
         * Returns the y axis.
         * 
         * @return the y axis
         */
        @Configurable(valueProvider = YAxisSelectionProvider.class)
        public YAxis getYAxis() {
            return yAxis;
        }

        /**
         * Sets the y axis.
         * 
         * @param yAxisIn the new y axis
         */
        public void setYAxis(YAxis yAxisIn) {
            final YAxis oldValue = this.yAxis;
            this.yAxis = yAxisIn;
            firePropertyChange(PROPERTY_Y_AXIS, oldValue, yAxisIn);
        }

        /**
         * Checks if is visible.
         * 
         * @return true, if is visible
         */
        @Configurable
        public boolean isVisible() {
            return visible;
        }

        /**
         * Sets the visible.
         * 
         * @param visible the new visible
         */
        public void setVisible(boolean visible) {
            final boolean oldValue = this.visible;
            if (oldValue == visible) {
                return;
            }
            this.visible = visible;
            firePropertyChange(PROPERTY_VISIBLE, oldValue, visible);
            // check if axes can be hidden or must be shown as a result
            if (visible) {
                getXAxis().setVisible(visible);
                getYAxis().setVisible(visible);
            } else {
                boolean xAxisUsed = false;
                boolean yAxisUsed = false;
                for (final Trace trace : getChartConfiguration().getTraces()) {
                    if (!trace.isVisible()) {
                        continue;
                    }
                    if (trace.getXAxis() == getXAxis()) {
                        xAxisUsed = true;
                    }
                    if (trace.getYAxis() == getYAxis()) {
                        yAxisUsed = true;
                    }
                }
                if (!xAxisUsed && getXAxis() != null) {
                    getXAxis().setVisible(visible);
                }
                if (!yAxisUsed && getYAxis() != null) {
                    getYAxis().setVisible(visible);
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see de.rcenvironment.rce.components.parametricstudy.gui.view.AbstractConfiguration#validate()
         */
        @Override
        public String validate() {
            if (type == null //
                || name == null || name.isEmpty() //
                || xAxis == null //
                || yAxis == null) {
                return "something is wrong";
            }
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getName();
        }

    }

}
