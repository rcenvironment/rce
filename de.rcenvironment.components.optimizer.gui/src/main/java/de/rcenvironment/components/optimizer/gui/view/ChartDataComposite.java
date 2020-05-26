/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import de.rcenvironment.components.optimizer.common.Dimension;
import de.rcenvironment.components.optimizer.common.Measure;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.common.OptimizerResultSet;
import de.rcenvironment.components.optimizer.common.ResultStructure;
import de.rcenvironment.components.optimizer.gui.properties.Messages;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.gui.utils.common.configuration.ExcelFileExporterDialog;
import de.rcenvironment.core.gui.utils.incubator.StudyDataExportMessageHelper;
import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;
import de.rcenvironment.core.utils.common.variables.legacy.VariableType;

/**
 * The {@link Composite} displaying the data backing a chart.
 * 
 * @author Sascha Zur
 */
@SuppressWarnings("deprecation")
// This is a legacy class which will not be adapted to the new Data Types. Thus, the deprecation
// warnings are suppressed here.
public class ChartDataComposite extends Composite implements ISelectionProvider {

    private static final int DEFAULT_COLUMN_WIDTH = 100;
    
    private static final String SAVE_LOCATION_PROPERTIES_NODE = "de.rcenvironment.core.gui.resources.savelocations";
    
    private static final String EXPORT_TO_EXCEL_PROPERTIES_IDENTIFIER = "export_to_excel_save_location";
    
    private ComponentExecutionInformation componentExecutionInformation;

    /** The button to save data to an excel file. */
    private Button saveDataButton;
    
    
    /** The table viewer. */
    private TableViewer tableViewer;

    /** The table. */
    private Table table;

    /** The study datastore. */
    private OptimizerDatastore resultDatastore;

    /** The dataset add listener. */
    private final OptimizerDatastore.OptimizerResultSetAddListener datasetAddListener =
        new OptimizerDatastore.OptimizerResultSetAddListener() {

            @Override
            public void handleStudyDatasetAdd(final OptimizerResultSet dataset) {
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (ChartDataComposite.this.isDisposed()) {
                            return;
                        }
                        ChartDataComposite.this.tableViewer.add(dataset);
                        ChartDataComposite.this.update();
                    }
                });
            }

        };

    /**
     * Instantiates a new chart data composite.
     * 
     * @param parent the parent
     * @param style the style
     * @param componentExecutionInformation
     */
    public ChartDataComposite(final Composite parent, final int style, ComponentExecutionInformation componentExecutionInformation) {
        super(parent, style);
        this.componentExecutionInformation = componentExecutionInformation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if (resultDatastore != null) {
            resultDatastore.removeDatasetAddListener(datasetAddListener);
        }
        super.dispose();
    }

    /**
     * Creates the controls.
     */
    public void createControls() {
        // layout
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.marginBottom = 5;
        setLayout(layout);
        GridData layoutData;
        // table viewer
        tableViewer = new TableViewer(this, SWT.MULTI | SWT.FULL_SELECTION);
        table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.horizontalIndent = 0;
        layoutData.verticalIndent = 0;
        layoutData.horizontalSpan = 2;
        table.setLayoutData(layoutData);
        // copy to clipboard
        final Button copyToClipboardButton = new Button(this, SWT.PUSH);
        copyToClipboardButton.setText(Messages.copyToClipboardLabel);

        layoutData = new GridData();
        copyToClipboardButton.setLayoutData(layoutData);
        // copy to clipboard button is only enabled, if a selection is made
        copyToClipboardButton.setEnabled(false);
        addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                copyToClipboardButton.setEnabled(!event.getSelection()
                    .isEmpty());
            }
        });
        copyToClipboardButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                final ISelection selection = getSelection();
                final List<String> keys = new LinkedList<String>();
                for (final TableColumn column : table.getColumns()) {
                    keys.add(column.getText());
                }
                final int keysCount = keys.size();
                final StringBuilder builder = new StringBuilder();
                if (selection != null
                    && selection instanceof IStructuredSelection) {
                    final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                    @SuppressWarnings("unchecked") final Iterator<OptimizerResultSet> iterator = structuredSelection
                        .iterator();
                    while (iterator.hasNext()) {
                        final OptimizerResultSet o = iterator.next();
                        for (int index = 0; index < keysCount; index++) {
                            final String key = keys.get(index);
                            if (o != null && o.getComponent() != null) {
                                final double value = o.getValue(key);
                                builder.append(value);
                            }
                            if (index < (keysCount - 1)) {
                                builder.append("\t");
                            }
                        }
                        if (iterator.hasNext()) {
                            builder.append("\n");
                        }
                    }
                    final String content = builder.toString();
                    ClipboardHelper.setContent(content);
                }
            }
        });
        saveDataButton = new Button(this, SWT.PUSH);
        saveDataButton.setText(Messages.excelExport);
        saveDataButton.setEnabled(false);
        saveDataButton.addSelectionListener(new ExportDataListener(this));
    }

    /**
     * 
     * 
     * @author Sascha Zur
     */
    private class ExportDataListener implements SelectionListener {
        
        private IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(SAVE_LOCATION_PROPERTIES_NODE);
        
        private final Shell cdc;

        ExportDataListener(ChartDataComposite cd) {
            cdc = cd.getShell();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {

        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            boolean success = true;
            FileDialog fd = new FileDialog(cdc, SWT.SAVE);
            fd.setText("Save");
            fd.setOverwrite(true);
            String lastSaveLocation = preferences.get(EXPORT_TO_EXCEL_PROPERTIES_IDENTIFIER, System.getProperty("user.dir"));
            fd.setFilterPath(lastSaveLocation);
            String[] filterExt = { "*.xls" };
            fd.setFilterExtensions(filterExt);
            String selected = fd.open();
            if (selected != null) {
                File excelFile = null;
                if (resultDatastore != null) {
                    
                    if (!selected.substring(selected.lastIndexOf('.') + 1).toLowerCase().equals("xls")) {
                        selected += ".xls";
                    }
                    excelFile = new File(selected); // or "e.xls"
                    TypedValue[][] values = new TypedValue[resultDatastore.getDatasetCount() + 1][];
                    
                    

                    Iterator<OptimizerResultSet> it = resultDatastore.getDatasets().iterator();
                    List<Dimension> dimensions = getSortedDimensions(resultDatastore.getStructure());
                    List<Measure> measures = getSortedMeasures(resultDatastore.getStructure());
                    int i = 0;
                    while (it.hasNext()) {

                        OptimizerResultSet next = it.next();

                        if (i == 0) {
                            values[i] = new TypedValue[next.getValues().size()];
                            values[i][0] = new TypedValue(VariableType.String, OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME);
                            for (int j = 0; j < dimensions.size(); j++) {
                                values[0][j + 1] = new TypedValue(VariableType.String, dimensions.get(j).getName());
                            }
                            for (int j = dimensions.size() + 1; j < measures.size() + dimensions.size() + 1; j++) {
                                values[0][j] = new TypedValue(VariableType.String, measures.get(j - dimensions.size() - 1).getName());
                            }
                            i++;
                        }

                        values[i] = new TypedValue[next.getValues().size()];
                        values[i][0] =
                            new TypedValue(next.getValue(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME));
                        for (int j = 0; j < dimensions.size(); j++) {
                            values[i][j + 1] = new TypedValue(next.getValue("Output: " + dimensions.get(j).getName()));
                        }
                        for (int j = dimensions.size() + 1; j < measures.size() + dimensions.size() + 1; j++) {
                            values[i][j] = new TypedValue(next.getValue(measures.get(j - dimensions.size() - 1).getName()));
                        }
                        i++;
                    }
                    success = ExcelFileExporterDialog.exportExcelFile(excelFile, values);

                } else {
                    success = false;
                }
                if (excelFile != null) {
                    StudyDataExportMessageHelper.showConfirmationOrWarningMessageDialog(success, excelFile.getPath());
                    preferences.put(EXPORT_TO_EXCEL_PROPERTIES_IDENTIFIER, excelFile.getParent());
                }
            }
        }
    }

    /**
     * Sets the study datastore.
     * 
     * @param studyDatastore the new study datastore
     */
    public void setStudyDatastore(final OptimizerDatastore studyDatastore) {
        if (this.resultDatastore == studyDatastore) {
            return;
        }
        if (this.resultDatastore == null) {
            this.resultDatastore = studyDatastore;
            final StudyDatastoreContentProvider contentProvider = new StudyDatastoreContentProvider();
            initializeStructure();
            // content
            studyDatastore.addDatasetAddListener(datasetAddListener);
            tableViewer.setContentProvider(contentProvider);
            tableViewer.setInput(studyDatastore);
            // getParent().update();
        }
    }

    /*
     * /** Initialize structure.
     */
    private void initializeStructure() {
        final ResultStructure structure = resultDatastore.getStructure();
        /**
         * A {@link ColumnLabelProvider} displaying the values of a parametric study.
         * 
         * @author Christian Weiss
         */
        class ValueLabelProvider extends ColumnLabelProvider {

            /** The key to use to lookup the values in a dataset (which is a map-like structure). */
            private final String key;

            private boolean isMeasure;

            /**
             * Instantiates a new {@link ValueLabelProvider} providing labels for the values with
             * the given key.
             * 
             * @param key
             * @param isMeasure
             */
            ValueLabelProvider(final String key, boolean isMeasure) {
                this.key = key;
                this.isMeasure = isMeasure;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
             */
            @Override
            public String getText(Object element) {
                if (!(element instanceof OptimizerResultSet)) {
                    return null;
                }
                String keyToLookup = key;
                if (isMeasure) {
                    keyToLookup = "Output: " + keyToLookup;
                }
                final double value = ((OptimizerResultSet) element).getValue(keyToLookup);
                String result;
                result = String.valueOf(value);
                return result;
            }

        }
        List<Dimension> list = getSortedDimensions(structure);
        final TableViewerColumn itColumn = new TableViewerColumn(tableViewer,
            SWT.NONE);
        itColumn.getColumn().setText(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME);
        itColumn.getColumn().setWidth(DEFAULT_COLUMN_WIDTH);
        itColumn.getColumn().setMoveable(true);
        itColumn.setLabelProvider(new ValueLabelProvider(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME, false));

        for (final Dimension dimension : list) {
            final TableViewerColumn column = new TableViewerColumn(tableViewer,
                SWT.NONE);
            column.getColumn().setText(dimension.getName());
            column.getColumn().setWidth(DEFAULT_COLUMN_WIDTH);
            column.getColumn().setMoveable(true);
            column.setLabelProvider(new ValueLabelProvider(dimension.getName(), true));
        }
        List<Measure> measureList = getSortedMeasures(structure);
        for (final Measure measure : measureList) {
            final TableViewerColumn column = new TableViewerColumn(tableViewer,
                SWT.NONE);
            column.getColumn().setText(measure.getName());
            column.getColumn().setWidth(DEFAULT_COLUMN_WIDTH);
            column.getColumn().setMoveable(true);
            column.setLabelProvider(new ValueLabelProvider(measure.getName(), false));
        }
    }

    private List<Measure> getSortedMeasures(final ResultStructure structure) {
        Collection<Measure> measureCollection = structure.getMeasures();
        List<Measure> measureList = new ArrayList<Measure>(measureCollection);
        Collections.sort(measureList, new Comparator<Measure>() {

            @Override
            public int compare(Measure o1, Measure o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return measureList;
    }

    private List<Dimension> getSortedDimensions(final ResultStructure structure) {
        Collection<Dimension> collection = structure.getDimensions();
        List<Dimension> list = new ArrayList<Dimension>(collection);
        Collections.sort(list, new Comparator<Dimension>() {

            @Override
            public int compare(Dimension o1, Dimension o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Dimension iteration = null;
        for (final Dimension dimension : list) {
            if (dimension.getName().equals(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME)) {
                iteration = dimension;
            }
        }
        list.remove(iteration);
        return list;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.widgets.Control#update()
     */
    @Override
    public void update() {
        super.update();
        if (!resultDatastore.getDatasets().isEmpty()) {
            saveDataButton.setEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        tableViewer.addSelectionChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
     */
    @Override
    public ISelection getSelection() {
        return tableViewer.getSelection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
     */
    @Override
    public void removeSelectionChangedListener(
        ISelectionChangedListener listener) {
        tableViewer.removeSelectionChangedListener(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void setSelection(ISelection selection) {
        tableViewer.setSelection(selection);
    }

    /**
     * The {@link IStructuredContentProvider} for the data table.
     */
    private final class StudyDatastoreContentProvider implements
        IStructuredContentProvider {

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        @Override
        public void dispose() {}

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
         *      java.lang.Object, java.lang.Object)
         */
        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

        /**
         * {@inheritDoc}
         * 
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements(Object inputElement) {
            if (!resultDatastore.getDatasets().isEmpty()) {
                // enabled if study data is available
                saveDataButton.setEnabled(true);
            }
            return resultDatastore.getDatasets().toArray(new OptimizerResultSet[0]);
        }

    }

}
