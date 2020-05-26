/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.view;

import java.io.File;
import java.io.Serializable;
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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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

import de.rcenvironment.components.parametricstudy.common.Dimension;
import de.rcenvironment.components.parametricstudy.common.Measure;
import de.rcenvironment.components.parametricstudy.common.StudyDataset;
import de.rcenvironment.components.parametricstudy.common.StudyStructure;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.gui.utils.common.configuration.ExcelFileExporterDialog;
import de.rcenvironment.core.gui.utils.incubator.StudyDataExportMessageHelper;
import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;
import de.rcenvironment.core.utils.common.variables.legacy.VariableType;

/**
 * The {@link Composite} displaying the data backing a chart.
 * 
 * @author Christian Weiss
 */
@SuppressWarnings("deprecation")
// This is a legacy class which will not be adapted to the new Data Types. Thus, the deprecation
// warnings are suppressed here.
public class ChartDataComposite extends Composite implements ISelectionProvider {

    private static final int DEFAULT_COLUMN_WIDTH = 100;

    private static final String SAVE_LOCATION_PROPERTIES_NODE = "de.rcenvironment.core.gui.resources.savelocations";
    
    private static final String EXPORT_TO_EXCEL_PROPERTIES_IDENTIFIER = "export_to_excel_save_location";
    
    
    /** The button to save data to an excel file. */
    private Button saveDataButton;

    /** The table viewer. */
    private TableViewer tableViewer;

    /** The table. */
    private Table table;

    /** The study datastore. */
    private StudyDatastore studyDatastore;

    /** The dataset add listener. */
    private final StudyDatastore.StudyDatasetAddListener datasetAddListener = new StudyDatastore.StudyDatasetAddListener() {

        @Override
        public void handleStudyDatasetAdd(final StudyDataset dataset) {
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
     */
    public ChartDataComposite(final Composite parent, final int style) {
        super(parent, style);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if (studyDatastore != null) {
            studyDatastore.removeDatasetAddListener(datasetAddListener);
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
                copyToClipboard();
            }

        });

        table.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == 'c') {
                    copyToClipboard();
                } else if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
                    table.selectAll();
                }
            }
        });
        saveDataButton = new Button(this, SWT.PUSH);
        saveDataButton.setText(Messages.excelExport);
        saveDataButton.setEnabled(false);
        saveDataButton.addSelectionListener(new MySelectionListener(this));
    }

    private void copyToClipboard() {
        
        final ISelection selection = getSelection();
        // keys will contain the column header strings
        final List<String> keys = new LinkedList<String>();
        for (final TableColumn column : table.getColumns()) {
            keys.add(column.getText());
        }
        final int keysCount = keys.size();
        final StringBuilder builder = new StringBuilder();
        if (selection != null
            && selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            @SuppressWarnings("unchecked") final Iterator<StudyDataset> iterator = structuredSelection
                .iterator();
            while (iterator.hasNext()) {
                final StudyDataset o = iterator.next();
                for (int index = 0; index < keysCount; index++) {
                    final String key = keys.get(index);
                    final Serializable value = o.getValue(key);
                    if (value != null) {
                        builder.append(value);
                    }
                    // add a tab to separate several values in a single row
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
            final StudyDatastoreContentProvider contentProvider = new StudyDatastoreContentProvider();
            initializeStructure();
            // content
            studyDatastore.addDatasetAddListener(datasetAddListener);
            tableViewer.setContentProvider(contentProvider);
            tableViewer.setInput(studyDatastore);
        }
    }

    /**
     * 
     * 
     * @author zur_sa
     */
    private class MySelectionListener implements SelectionListener {

        private IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(SAVE_LOCATION_PROPERTIES_NODE);
        
        private final Shell cdc;

        MySelectionListener(ChartDataComposite cd) {
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
                excelFile = new File(selected); // or "e.xls"
                if (studyDatastore != null) {
                    if (!selected.substring(selected.lastIndexOf('.') + 1).toLowerCase().equals("xls")) {
                        selected += ".xls";
                    }
                    TypedValue[][] values = new TypedValue[studyDatastore.getDatasetCount() + 1][];

                    Iterator<StudyDataset> it = studyDatastore.getDatasets().iterator();
                    int i = 0;
                    List<Dimension> dimensions = getSortedDimensions(studyDatastore.getStructure());
                    List<Measure> measures = getSortedMeasures(studyDatastore.getStructure());
                    while (it.hasNext()) {

                        StudyDataset next = it.next();

                        if (i == 0) {
                            values[i] = new TypedValue[dimensions.size() + measures.size()];
                            for (int j = 0; j < dimensions.size(); j++) {
                                values[0][j] = new TypedValue(VariableType.String, dimensions.get(j).getName());
                            }
                            for (int j = dimensions.size(); j < measures.size() + dimensions.size(); j++) {
                                values[0][j] = new TypedValue(VariableType.String, measures.get(j - dimensions.size()).getName());
                            }
                            i++;
                        }

                        values[i] = new TypedValue[dimensions.size() + measures.size()];
                        for (int j = 0; j < dimensions.size(); j++) {
                            values[i][j] = new TypedValue(next.getValue(dimensions.get(j).getName()));
                        }
                        for (int j = dimensions.size(); j < measures.size() + dimensions.size(); j++) {
                            values[i][j] = new TypedValue(next.getValue(measures.get(j - dimensions.size()).getName()));
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
    
    
    private List<Measure> getSortedMeasures(final StudyStructure structure) {
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

    private List<Dimension> getSortedDimensions(final StudyStructure structure) {
        Collection<Dimension> collection = structure.getDimensions();
        List<Dimension> list = new ArrayList<Dimension>(collection);
        Collections.sort(list, new Comparator<Dimension>() {

            @Override
            public int compare(Dimension o1, Dimension o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return list;
    }

    /**
     * Initialize structure.
     */
    private void initializeStructure() {
        final StudyStructure structure = studyDatastore.getStructure();
        /**
         * A {@link ColumnLabelProvider} displaying the values of a parametric study.
         * 
         * @author Christian Weiss
         */
        class ValueLabelProvider extends ColumnLabelProvider {

            /** The key to use to lookup the values in a dataset (which is a map-like structure). */
            private final String key;

            /**
             * Instantiates a new {@link ValueLabelProvider} providing labels for the values with
             * the given key.
             * 
             * @param key
             */
            ValueLabelProvider(final String key) {
                this.key = key;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
             */
            @Override
            public String getText(Object element) {
                if (!(element instanceof StudyDataset)) {
                    return null;
                }
                final Serializable value = ((StudyDataset) element)
                    .getValue(key);
                String result;
                if (value == null) {
                    result = "";
                } else {
                    result = value.toString();
                }
                return result;
            }

        }

        
        for (final Dimension dimension : getSortedDimensions(structure)) {
            final TableViewerColumn column = new TableViewerColumn(tableViewer,
                SWT.NONE);
            column.getColumn().setText(dimension.getName());
            column.getColumn().setImage(Activator.getImage("cube.dimension"));
            column.getColumn().setWidth(DEFAULT_COLUMN_WIDTH);
            column.getColumn().setMoveable(true);
            column.setLabelProvider(new ValueLabelProvider(dimension.getName()));
        }
        
        for (final Measure measure : getSortedMeasures(structure)) {
            final TableViewerColumn column = new TableViewerColumn(tableViewer,
                SWT.NONE);
            column.getColumn().setText(measure.getName());
            column.getColumn().setImage(Activator.getImage("cube.measure"));
            column.getColumn().setWidth(DEFAULT_COLUMN_WIDTH);
            column.getColumn().setMoveable(true);
            column.setLabelProvider(new ValueLabelProvider(measure.getName()));
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.widgets.Control#update()
     */
    @Override
    public void update() {
        super.update();
        if (table.getItem(0).getText().equals("")) {
            table.remove(0);
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
            if (!studyDatastore.getDatasets().isEmpty()) {
                // enabled if study data is available
                saveDataButton.setEnabled(true);
            }
            return studyDatastore.getDatasets().toArray(new StudyDataset[0]);
        }
    }

}
