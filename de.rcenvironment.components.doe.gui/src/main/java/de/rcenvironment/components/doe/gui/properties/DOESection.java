/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.gui.properties;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.doe.common.DOEAlgorithms;
import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.components.doe.common.DOEUtils;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Section for all design of experiments configurations.
 * 
 * @author Sascha Zur
 * @author Kathrin Schaffert
 */
public class DOESection extends ValidatingWorkflowNodePropertySection {

    protected static final Log LOGGER = LogFactory.getLog(DOESection.class);

    private static final String ERROR = "Error";

    private static final String NULL = "null";

    private static final String COULD_NOT_READ_TABLE_ERROR = "Could not read table. ";

    private static final int MAX_GUI_ELEMENTS;

    // Since there is a bug with linux systems which lets RCE crash, there must be a constraint that
    // is lower than the windows constraint for GUI elements.
    // The bug is an xserver bug, which occurs having an intel graphics card. It is described in
    // this thread:
    // https://forums.opensuse.org/showthread.php/389147-X-Error-of-failed-request-BadAlloc-%28insufficient-resources

    static {
        if (OS.isFamilyUnix()) {
            final int value = 2000;
            MAX_GUI_ELEMENTS = value;
        } else {
            final int value = 10000;
            MAX_GUI_ELEMENTS = value;
        }
    }

    private Combo algorithmSelection;

    private Button loadTable;

    private Button saveTable;

    private Table table;

    private Composite tableComposite;

    private Label runLabel;

    private Spinner runSpinner;

    private Label seedLabel;

    private Spinner seedSpinner;

    private Button codedValuesButton;

    private Label startLabel;

    private Text startSample;

    private Label endLabel;

    private Text endSample;

    private TableViewer viewer;

    private String[][] tableValues;

    private Label outputsWarningLabel;

    private Button clearTableButton;

    @Override
    protected void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.sectionHeader);
        Composite mainComposite = new Composite(sectionProperties, SWT.NONE);
        mainComposite.setLayout(new GridLayout(1, false));
        mainComposite.setBackground(Display.getCurrent().getSystemColor(1));
        GridData mainData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        mainComposite.setLayoutData(mainData);

        Composite upperMainComposite = new Composite(mainComposite, SWT.NONE);
        GridLayout upperMainLayout = new GridLayout(2, false);
        upperMainLayout.marginWidth = 0;
        upperMainLayout.marginHeight = 0;
        upperMainComposite.setLayout(upperMainLayout);
        upperMainComposite.setBackground(Display.getCurrent().getSystemColor(1));
        GridData upperMainData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        upperMainComposite.setLayoutData(upperMainData);

        Composite algorithmComposite = new Composite(upperMainComposite, SWT.NONE);
        algorithmComposite.setLayout(new GridLayout(4, false));
        algorithmComposite.setBackground(Display.getCurrent().getSystemColor(1));

        algorithmSelection = new Combo(algorithmComposite, SWT.BORDER | SWT.READ_ONLY);
        algorithmSelection.setItems(DOEConstants.ALGORITMS);
        GridData selectionData = new GridData(GridData.FILL_BOTH);
        selectionData.horizontalSpan = 2;
        algorithmSelection.setLayoutData(selectionData);
        algorithmSelection.setData(CONTROL_PROPERTY_KEY, DOEConstants.KEY_METHOD);
        algorithmSelection.addSelectionListener(new AlgorithmSelectionListener());
        seedLabel = new Label(algorithmComposite, SWT.None);
        seedLabel.setText(Messages.seedLabel);
        seedSpinner = new Spinner(algorithmComposite, SWT.BORDER);
        seedSpinner.setMaximum(Integer.MAX_VALUE);
        seedSpinner.setData(CONTROL_PROPERTY_KEY, DOEConstants.KEY_SEED_NUMBER);

        Composite runComposite = new Composite(algorithmComposite, SWT.NONE);
        GridLayout runLayout = new GridLayout(2, false);
        runLayout.marginWidth = 0;
        runComposite.setLayout(runLayout);
        GridData runData = new GridData();
        runData.horizontalSpan = 4;
        runData.horizontalIndent = 0;

        runComposite.setLayoutData(runData);
        runLabel = new Label(runComposite, SWT.NONE);
        runSpinner = new Spinner(runComposite, SWT.BORDER);
        runSpinner.setData(CONTROL_PROPERTY_KEY, DOEConstants.KEY_RUN_NUMBER);
        runSpinner.setMaximum(Integer.MAX_VALUE);
        GridData spinnerData = new GridData();
        runSpinner.setLayoutData(spinnerData);

        Composite sampleComposite = new Composite(upperMainComposite, SWT.NONE);
        sampleComposite.setLayout(new GridLayout(2, false));
        sampleComposite.setBackground(Display.getCurrent().getSystemColor(1));
        GridData sampleData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        final int horizontalIndentSample = 50;
        sampleData.horizontalIndent = horizontalIndentSample;
        sampleComposite.setLayoutData(sampleData);

        startLabel = new Label(sampleComposite, SWT.NONE);
        startLabel.setText(Messages.sampleStart);
        startSample = new Text(sampleComposite, SWT.BORDER);
        GridData startData = new GridData();
        final int minWidthSamples = 30;
        startData.widthHint = minWidthSamples;
        startSample.addVerifyListener(new NumericalTextConstraintListener(startSample, NumericalTextConstraintListener.ONLY_INTEGER
            | NumericalTextConstraintListener.GREATER_OR_EQUAL_ZERO));
        startSample.setLayoutData(startData);
        startSample.setData(CONTROL_PROPERTY_KEY, DOEConstants.KEY_START_SAMPLE);
        endLabel = new Label(sampleComposite, SWT.NONE);
        endLabel.setText(Messages.sampleEnd);
        endSample = new Text(sampleComposite, SWT.BORDER);
        GridData endData = new GridData();
        endData.widthHint = minWidthSamples;
        endSample.setLayoutData(endData);
        endSample.addVerifyListener(new NumericalTextConstraintListener(endSample, NumericalTextConstraintListener.ONLY_INTEGER
            | NumericalTextConstraintListener.GREATER_OR_EQUAL_ZERO));
        endSample.setData(CONTROL_PROPERTY_KEY, DOEConstants.KEY_END_SAMPLE);

        GridLayout tableCompositeLayout = new GridLayout(3, false);
        tableCompositeLayout.marginWidth = 0;
        tableCompositeLayout.marginHeight = 0;
        tableComposite = new Composite(mainComposite, SWT.NONE);
        tableComposite.setLayout(tableCompositeLayout);
        tableComposite.setBackground(Display.getCurrent().getSystemColor(1));
        GridData tableData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        tableComposite.setLayoutData(tableData);

        addTableComposite();
        sectionProperties.setClient(mainComposite);
    }

    private void addTableComposite() {
        Composite buttonsComp = new Composite(tableComposite, SWT.NONE);
        GridLayout tableLayout = new GridLayout(5, false);
        tableLayout.marginWidth = 0;
        buttonsComp.setLayout(tableLayout);
        GridData buttonsData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
        buttonsData.horizontalSpan = 3;
        buttonsComp.setLayoutData(buttonsData);
        saveTable = new Button(buttonsComp, SWT.PUSH);
        saveTable.setText(Messages.saveTableButton);
        saveTable.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                saveTable();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        loadTable = new Button(buttonsComp, SWT.PUSH);
        loadTable.setText(Messages.loadTableButton);
        loadTable.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                loadTable();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        GridData loadData = new GridData();
        loadTable.setLayoutData(loadData);
        clearTableButton = new Button(buttonsComp, SWT.PUSH);
        clearTableButton.setText("Clear table");
        clearTableButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                setProperty(DOEConstants.KEY_TABLE, "");
                fillTableHeader();
                fillTable();
                refreshSection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        codedValuesButton = new Button(buttonsComp, SWT.CHECK);
        codedValuesButton.setText(Messages.codedValuesButton);
        codedValuesButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                fillTable();
            }
        });
        outputsWarningLabel = new Label(buttonsComp, SWT.NONE);
        outputsWarningLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        viewer = new TableViewer(buttonsComp, SWT.MULTI | SWT.H_SCROLL
            | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        int operations = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT;
        Transfer[] transferTypes = new Transfer[] { FileTransfer.getInstance() };
        viewer.addDropSupport(operations, transferTypes, new TableDropListener(viewer));
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(tableValues);
        table = viewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        final int minHeight = 200;
        data.minimumHeight = minHeight;
        data.horizontalSpan = 5;
        table.setLayoutData(data);
        table.setData(CONTROL_PROPERTY_KEY, DOEConstants.KEY_TABLE);
    }

    private void saveTable() {
        FileDialog dialog = new FileDialog(tableComposite.getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*" + DOEConstants.TABLE_FILE_EXTENTION, "*.*" });

        String path = dialog.open();
        List<String> outputs = new LinkedList<>();
        for (EndpointDescription e : getOutputs()) {
            outputs.add(e.getName());
        }
        Collections.sort(outputs);
        DOEUtils.writeTableToCSVFile(tableValues, path, outputs);
    }

    private void loadTable() {
        String path = PropertyTabGuiHelper.selectFileFromFileSystem(tableComposite.getShell(),
            new String[] { "*.csv", "*.*" }, "Open table file...");
        if (path != null) {
            loadTableFromFile(path);
        }
    }

    private boolean loadTableFromFile(String path) {
        boolean success = true;
        List<CSVRecord> records = parseFileToCSVRecordList(path);
        if (records == null) {
            return false;
        }
        if (Character.isLetter((records.get(0).get(0).charAt(0)))) {
            records.remove(0);
        }
        tableValues = new String[records.size()][];
        success = extractValuesFromRecord(records, tableValues);
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

        if (success) {
            try {
                setProperties(
                    DOEConstants.KEY_TABLE, mapper.writeValueAsString(tableValues),
                    DOEConstants.KEY_START_SAMPLE, "0",
                    DOEConstants.KEY_END_SAMPLE, Integer.toString(records.size() - 1),
                    DOEConstants.KEY_RUN_NUMBER, Integer.toString(records.size()));
            } catch (IOException e) {
                LOGGER.error(e);
                return false;
            }
            refreshSection();
        }
        return success;
    }

    private List<CSVRecord> parseFileToCSVRecordList(String path) {
        File csvData = new File(path);
        CSVParser parser;
        List<CSVRecord> records = null;
        try {
            parser = CSVParser.parse(FileUtils.readFileToString(csvData),
                CSVFormat.newFormat(';').withIgnoreSurroundingSpaces().withAllowMissingColumnNames().withRecordSeparator("\n"));
            records = parser.getRecords();
        } catch (IOException e) {
            MessageDialog
                .openInformation(this.getComposite().getShell(), ERROR, "Could not parse file.\n\nReason: " + e.getMessage());
        }
        return records;
    }

    private boolean extractValuesFromRecord(List<CSVRecord> records, String[][] values) {
        int count = 0;
        for (CSVRecord record : records) {
            int size = getOutputs().size();
            values[count] = new String[size];
            for (int i = 0; i < size; i++) {
                if (record.size() > i) {
                    String number = record.get(i);
                    if (number.equals("null")) {
                        values[count][i] = "";
                    } else {
                        Matcher matcher = Pattern.compile("(\\+|-)?\\d+(,|.)?\\d*(e|E)?(\\+|-)?\\d*").matcher(number);

                        if (count == 0 && i == 0 && matcher.find()) {
                            number = matcher.group();
                        }
                        values[count][i] = number.replaceAll("\"", " ").replace(",", ".");
                        try {
                            Double.valueOf(values[count][i]);
                        } catch (NumberFormatException e) {
                            String message = "Could not load table from file. The Table contains values that are not of type double.\n "
                                + e.getMessage();
                            MessageDialog.openInformation(this.getComposite().getShell(), ERROR, message);
                            return false;
                        }
                    }
                } else {
                    values[count][i] = "";
                }
            }
            count++;
        }
        return true;
    }

    private void fillTableHeader() {
        if (this.getOutputs() != null) {
            String[] titles = new String[this.getOutputs().size()];
            int j = 0;
            for (EndpointDescription e : this.getOutputs()) {
                titles[j++] = e.getName();
            }
            Arrays.sort(titles);
            table.setRedraw(false);
            for (TableColumn column : table.getColumns()) {
                column.dispose();
            }
            for (int i = 0; i < titles.length + 1; i++) {
                TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
                if (i == 0) {
                    column.getColumn().setText(Messages.sampleNum);
                } else {
                    column.getColumn().setText(titles[i - 1]);
                    column.setEditingSupport(new TextEditingSupport(viewer, table, i));
                }
                column.setLabelProvider(createTableColumnLabelProvider(i));
                table.getColumn(i).pack();
            }

            for (int i = 0; i < titles.length; i++) {
                table.getColumn(i).pack();
            }
            table.setRedraw(true);
        }
    }

    private ColumnLabelProvider createTableColumnLabelProvider(final int k) {
        return new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                String returnValue = "";
                if (tableValues != null) {
                    if (k == 0) {
                        for (int i = 0; i < tableValues.length; i++) {
                            if (tableValues[i] == (String[]) element) {
                                returnValue = String.valueOf(i);
                            }
                        }
                    } else {
                        if (k - 1 < ((String[]) element).length) {
                            returnValue = String.valueOf(((String[]) element)[k - 1]);
                        }
                    }
                }
                if (returnValue == null || returnValue.equals(NULL)) {
                    returnValue = "";
                }
                return returnValue;
            }
        };
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        refreshDOESection();
    }

    private void refreshDOESection() {
        fillTableHeader();
        algorithmSelection.setText(getProperty(DOEConstants.KEY_METHOD));
        fillTable();
        updateActivationAndWarningMessages();
        if (startSample.getText() != null && !startSample.getText().isEmpty() && getProperty(DOEConstants.KEY_START_SAMPLE) != null
            && !startSample.getText().equals(getProperty(DOEConstants.KEY_START_SAMPLE))) {
            startSample.setText(getProperty(DOEConstants.KEY_START_SAMPLE));
        }
        if (endSample.getText() != null && !endSample.getText().isEmpty() && getProperty(DOEConstants.KEY_END_SAMPLE) != null
            && !endSample.getText().equals(getProperty(DOEConstants.KEY_END_SAMPLE))) {
            endSample.setText(getProperty(DOEConstants.KEY_END_SAMPLE));
        }
    }

    @Override
    protected void beforeTearingDownModelBinding() {
        super.beforeTearingDownModelBinding();
        outputsWarningLabel.setText("");
    }

    /**
     * Fill the design table.
     */
    private void fillTable() {

        table.clearAll();

        if (getOutputs() == null) {
            return;
        }
        int varCount = getOutputs().size();
        if (varCount < 2 && !(algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
            || algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)
            || algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_MONTE_CARLO))) {
            return;
        }

        tableValues = createTableValues();
        if (tableValues.length == 0) {
            return;
        }
        if (tableValues.length * varCount > MAX_GUI_ELEMENTS) {
            if (outputsWarningLabel != null) {
                outputsWarningLabel.setVisible(true);
                outputsWarningLabel.setText(Messages.tooMuchElements);
                outputsWarningLabel.pack();
            }
            return;
        }

        viewer.setInput(tableValues);

        if (codedValuesButton.getSelection() && !getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
            updateCodedValues();
        }

        viewer.refresh();
        for (TableColumn c : table.getColumns()) {
            c.pack();
        }
    }

    private void updateCodedValues() {
        String[] endpointNames = new String[this.getOutputs().size()];
        int j = 0;
        for (EndpointDescription e : this.getOutputs()) {
            endpointNames[j++] = e.getName();
        }
        Arrays.sort(endpointNames);

        for (int i = 0; i < tableValues.length; i++) {
            TableItem current = null;
            if (table.getItemCount() == 0 || table.getItemCount() <= i) {
                current = new TableItem(table, SWT.NONE);
            } else {
                current = table.getItem(i);
            }
            current.setText(0, String.valueOf(i));

            for (int k = 0; k < tableValues[i].length; k++) { // NOSONAR because if tableValues were null here, this code wouldn't have been
                                                              // reached
                EndpointDescription currentEndpoint = null;
                for (EndpointDescription e : getOutputs()) {
                    if (e.getName().equals(endpointNames[k])) {
                        currentEndpoint = e;
                    }
                }
                if (currentEndpoint != null && tableValues != null && tableValues[i] != null && tableValues[i][k] != null) {
                    Double low = Double.valueOf(currentEndpoint.getMetaDataValue(DOEConstants.META_KEY_LOWER));
                    Double up = Double.valueOf(currentEndpoint.getMetaDataValue(DOEConstants.META_KEY_UPPER));
                    tableValues[i][k] = String.valueOf(DOEAlgorithms.convertValue(low, up,
                        Double.parseDouble(tableValues[i][k])));
                }
            }
        }
    }

    private String[][] createTableValues() {
        String algorithm = algorithmSelection.getText();
        if (algorithm.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)) {
            return createTableValuesForCustomTableInput();
        }

        int varCount = getOutputs().size();
        if (varCount == 0) {
            return new String[0][0];
        }
        final int runCount = getRunCount();
        final int seedCount = getSeedCount();

        Double[][] tableValuesDouble = null;
        switch (algorithm) {
        case DOEConstants.DOE_ALGORITHM_FULLFACT:
            if (runSpinner.getSelection() >= 2) {
                tableValuesDouble = DOEAlgorithms.populateTableFullFactorial(varCount, runCount);
            }
            break;
        case DOEConstants.DOE_ALGORITHM_LHC:
            tableValuesDouble = DOEAlgorithms.populateTableLatinHypercube(varCount, runCount, seedCount);
            break;
        case DOEConstants.DOE_ALGORITHM_MONTE_CARLO:
            tableValuesDouble = DOEAlgorithms.populateTableMonteCarlo(varCount, runCount, seedCount);
            break;
        case DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE:
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            try {
                String tableString = getProperty(DOEConstants.KEY_TABLE);
                if (tableString == null || tableString.isEmpty()) {
                    break;
                }

                Double[][] currentTableValues = mapper.readValue(getProperty(DOEConstants.KEY_TABLE), Double[][].class);
                if (currentTableValues == null) {
                    break;
                }

                int columnNum = this.getOutputs().size();
                if (currentTableValues[0].length != columnNum || currentTableValues.length != runCount) {
                    tableValuesDouble = new Double[runCount][columnNum];
                    int rows = Math.min(runCount, currentTableValues.length);
                    int columns = Math.min(columnNum, currentTableValues[0].length);
                    for (int i = 0; i < rows; i++) {
                        tableValuesDouble[i] = Arrays.copyOf(Arrays.copyOfRange(currentTableValues[i], 0, columns), columnNum);
                    }
                } else {
                    tableValuesDouble = currentTableValues;
                }
            } catch (IOException e) {
                LOGGER.error(COULD_NOT_READ_TABLE_ERROR, e);
            }
            break;
        default:
            break;
        }
        if (!node.getInputDescriptionsManager().isValidEndpointName(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME)) {
            node.getInputDescriptionsManager().removeDynamicEndpointDescription(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME);
        }

        return writeTableValuesAsStringArray(varCount, tableValuesDouble);
    }

    private String[][] writeTableValuesAsStringArray(int varCount, Double[][] tableValuesDouble) {
        if (tableValuesDouble != null && tableValuesDouble.length > 0) {
            String[][] returnValues = new String[tableValuesDouble.length][tableValuesDouble[0].length];
            for (int i = 0; i < tableValuesDouble.length; i++) {
                if (tableValuesDouble[i] != null) {
                    returnValues[i] = new String[tableValuesDouble[i].length];
                    for (int j = 0; j < tableValuesDouble[i].length; j++) {
                        returnValues[i][j] = String.valueOf(tableValuesDouble[i][j]);
                    }
                } else {
                    returnValues[i] = new String[varCount];
                    Arrays.fill(returnValues[i], "");
                }
            }
            return returnValues;
        } else if (!DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE.equals(algorithmSelection.getText())) {
            return new String[0][0];
        } else {
            return new String[getRunCount()][varCount];
        }
    }

    private String[][] createTableValuesForCustomTableInput() {

        if (node.getInputDescriptionsManager().isValidEndpointName(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME)) {
            Map<String, String> metaData = new HashMap<>();
            node.getInputDescriptionsManager().addDynamicEndpointDescription(DOEConstants.CUSTOM_TABLE_ENDPOINT_ID,
                DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME, DataType.Matrix,
                metaData);
        }

        return new String[0][0];
    }

    private int getRunCount() {
        if (runSpinner == null) {
            throw new NullPointerException("runSpinner should not be null");
        }
        return runSpinner.getSelection();
    }

    private int getSeedCount() {
        if (seedSpinner == null) {
            throw new NullPointerException("seedSpinner should not be null");
        }
        return seedSpinner.getSelection();
    }

    private void updateActivationAndWarningMessages() {
        boolean isCustomDesignTable = algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE);
        loadTable.setEnabled(isCustomDesignTable);
        clearTableButton.setEnabled(isCustomDesignTable);
        startLabel.setVisible(isCustomDesignTable);
        startSample.setVisible(isCustomDesignTable);
        endLabel.setVisible(isCustomDesignTable);
        endSample.setVisible(isCustomDesignTable);
        runLabel.setEnabled(true);
        runSpinner.setEnabled(true);
        seedSpinner.setEnabled(false);
        seedLabel.setEnabled(false);
        codedValuesButton.setEnabled(true);
        table.setEnabled(true);
        saveTable.setEnabled(true);

        outputsWarningLabel.setText("");
        outputsWarningLabel.setVisible(false);

        if (algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)) {
            activateGUIElements(false);
        } else if (algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_FULLFACT)) {
            runLabel.setText(Messages.numLevelsLabel);
        } else if (algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_LHC)
            || algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_MONTE_CARLO)) {
            runLabel.setText(Messages.desiredRunsLabel);
            seedSpinner.setEnabled(true);
            seedLabel.setEnabled(true);
        } else {
            runLabel.setText(Messages.desiredRunsLabel);
            runLabel.setEnabled(true);
            runSpinner.setEnabled(true);
            codedValuesButton.setEnabled(false);
        }

        if (getOutputs().isEmpty() && !(algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_FULLFACT)
            || algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_LHC))) {
            outputsWarningLabel.setVisible(true);
            outputsWarningLabel.setText(Messages.minOneOutput);
            activateGUIElements(false);
        } else if (getOutputs().size() < 2 && (algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_FULLFACT)
            || algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_LHC))) {
            outputsWarningLabel.setVisible(true);
            outputsWarningLabel.setText(Messages.minTwoOutputs);
            activateGUIElements(false);
        } else if (algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
            checkCustomTable();
        }

        if (outputsWarningLabel != null && !outputsWarningLabel.isVisible() && tableValues != null
            && (tableValues.length != 0 && (tableValues.length * getOutputs().size() > MAX_GUI_ELEMENTS || tableValues.length == 0))
            && !algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)) {
            outputsWarningLabel.setVisible(true);
            outputsWarningLabel.setText(Messages.tooMuchElements);
        }

        if (outputsWarningLabel != null) {
            if (outputsWarningLabel.getText().isEmpty()) {
                outputsWarningLabel.setVisible(false);
            }
            outputsWarningLabel.pack();
        }

        runLabel.getParent().pack();
    }

    private void checkCustomTable() {
        if ((getProperty(DOEConstants.KEY_TABLE) == null || getProperty(DOEConstants.KEY_TABLE).isEmpty())) {
            outputsWarningLabel.setVisible(true);
            outputsWarningLabel.setText(Messages.noTableLong);
        } else if (getProperty(DOEConstants.KEY_TABLE) != null) {
            checkForEmptyCells();
        }
    }

    private void checkForEmptyCells() {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            Double[][] tableValuesDouble = mapper.readValue(getProperty(DOEConstants.KEY_TABLE), Double[][].class);
            int runNumber = Integer.parseInt(getProperty(DOEConstants.KEY_RUN_NUMBER));
            for (int i = 0; (i <= runNumber && (tableValuesDouble != null) && i < tableValuesDouble.length); i++) {
                for (int j = 0; j < tableValuesDouble[i].length; j++) {
                    if (tableValuesDouble[i][j] == null) {
                        outputsWarningLabel.setVisible(true);
                        outputsWarningLabel.setText(Messages.noTableLong);
                    }
                }
                if (outputsWarningLabel.isVisible()) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error(e.getStackTrace());
        }
    }

    private void activateGUIElements(boolean enabled) {
        saveTable.setEnabled(enabled);
        table.setEnabled(enabled);
        loadTable.setEnabled(enabled);
        clearTableButton.setEnabled(enabled);
        codedValuesButton.setEnabled(enabled);
        runSpinner.setEnabled(enabled);
        seedSpinner.setEnabled(enabled);
        table.clearAll();
        table.redraw();
    }

    /**
     * 
     * @author Sascha Zur
     */
    private class AlgorithmSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_FULLFACT) && runSpinner.getSelection() < 2) {
                setProperties(DOEConstants.KEY_METHOD, algorithmSelection.getText(), DOEConstants.KEY_RUN_NUMBER, "2");
            } else {
                setProperty(DOEConstants.KEY_METHOD, algorithmSelection.getText());
            }
        }
    }

    /**
     * Adds editing a cell in the table.
     * 
     * @author Sascha Zur
     */
    private class TextEditingSupport extends EditingSupport {

        private final TextCellEditor editor;

        private final int columnNumber;

        private final ColumnViewer viewer;

        TextEditingSupport(ColumnViewer viewer, Table table, int columnNumber) {
            super(viewer);
            this.editor = new TextCellEditor(table);
            editor.setValidator((Object arg0) -> {
                try {
                    Double.parseDouble((String) arg0);
                } catch (NumberFormatException e) {
                    if (((String) arg0).isEmpty()) {
                        return null;
                    }
                    return "No";
                }
                return null;
            });
            this.columnNumber = columnNumber;
            this.viewer = viewer;
        }

        @Override
        protected boolean canEdit(Object arg0) {
            return getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE);
        }

        @Override
        protected CellEditor getCellEditor(Object arg0) {
            return editor;
        }

        @Override
        protected Object getValue(Object arg0) {
            Object returnValue = ((String[]) arg0)[columnNumber - 1];
            if (returnValue == null || returnValue.equals(NULL)) {
                returnValue = "";
            }
            return returnValue;
        }

        @Override
        protected void setValue(Object arg0, Object arg1) {
            ((String[]) arg0)[columnNumber - 1] = (String) arg1;
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            String[][] eliminateNull = ((String[][]) viewer.getInput());
            for (int i = 0; i < eliminateNull.length; i++) {
                for (int j = 0; j < eliminateNull[i].length; j++) {
                    if (eliminateNull[i][j] == null || eliminateNull[i][j].equals(NULL)) {
                        eliminateNull[i][j] = "";
                    }
                }
            }
            try {
                setProperty(DOEConstants.KEY_TABLE, mapper.writeValueAsString(viewer.getInput()));
            } catch (IOException e) {
                LOGGER.error(e);
            }
            viewer.refresh();
        }
    }

    /**
     * Drop listener for the table, so .csv files can be dropped on it and will be loaded.
     * 
     * @author Sascha Zur
     */
    public class TableDropListener extends ViewerDropAdapter {

        protected TableDropListener(Viewer viewer) {
            super(viewer);
        }

        @Override
        public boolean performDrop(Object arg0) {
            String[] fileDrops = (String[]) arg0;
            if (fileDrops.length != 1 || !fileDrops[0].endsWith(DOEConstants.TABLE_FILE_EXTENTION)) {
                return false;
            } else {
                String fileLocation = fileDrops[0];
                boolean success = loadTableFromFile(fileLocation);
                if (success) {
                    algorithmSelection.select(algorithmSelection.indexOf(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE));
                    setProperty(DOEConstants.KEY_METHOD, DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE);
                    updateActivationAndWarningMessages();
                    fillTable();
                }
            }
            return true;
        }

        @Override
        public boolean validateDrop(Object arg0, int arg1, TransferData arg2) {
            return true;
        }

    }

    @Override
    protected Set<EndpointDescription> getOutputs() {
        Set<EndpointDescription> outputs = new HashSet<>(super.getOutputs());

        Iterator<EndpointDescription> outputsIterator = outputs.iterator();
        while (outputsIterator.hasNext()) {
            EndpointDescription next = outputsIterator.next();
            if (LoopComponentConstants.ENDPOINT_ID_TO_FORWARD.equals(next.getDynamicEndpointIdentifier())
                || LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE.equals(next.getName())
                || DOEConstants.OUTPUT_NAME_NUMBER_OF_SAMPLES.equals(next.getName())) {
                outputsIterator.remove();
            }
        }
        return outputs;
    }

    @Override
    protected DOESectionController createController() {
        return new DOESectionController();
    }

    protected class DOESectionController extends DefaultController {

        @Override
        public void modifyText(ModifyEvent event) {

            final Object source = event.getSource();
            if (source instanceof Control) {
                final Control control = (Control) source;
                final String property = (String) control.getData(CONTROL_PROPERTY_KEY);

                if (property.equals(DOEConstants.KEY_START_SAMPLE)) {
                    setStartSample();
                }
                if (property.equals(DOEConstants.KEY_END_SAMPLE)) {
                    setEndSample();
                }
            }
        }

        private void setEndSample() {
            String startSampleString = getProperty(DOEConstants.KEY_START_SAMPLE);
            String endSampleString = endSample.getText();
            String currentEndSample = getProperty(DOEConstants.KEY_END_SAMPLE);
            int runCount = runSpinner.getSelection();
            if (startSampleString != null && !startSampleString.isEmpty() && endSampleString != null && !endSampleString.isEmpty()
                && !currentEndSample.equals(endSampleString) && runCount > 0) {
                if (Integer.parseInt(endSampleString) >= runCount) {
                    if (!getProperty(DOEConstants.KEY_END_SAMPLE).equals(String.valueOf(runCount - 1))) {
                        setProperty(DOEConstants.KEY_END_SAMPLE, String.valueOf(runCount - 1));
                    } else {
                        endSample.setText(String.valueOf(runCount - 1));
                    }
                } else if (Integer.parseInt(endSampleString) < Integer.parseInt(startSampleString)) {
                    if (!getProperty(DOEConstants.KEY_END_SAMPLE).equals(startSampleString)) {
                        setProperty(DOEConstants.KEY_END_SAMPLE, startSampleString);
                    } else {
                        endSample.setText(startSampleString);
                    }

                } else {
                    setProperty(DOEConstants.KEY_END_SAMPLE, endSampleString);
                }
            }
        }

        private void setStartSample() {
            String startSampleString = startSample.getText();
            String currentStartSampleString = getProperty(DOEConstants.KEY_START_SAMPLE);
            String endSampleString = getProperty(DOEConstants.KEY_END_SAMPLE);
            if (startSampleString != null && !startSampleString.isEmpty() && !currentStartSampleString.equals(startSampleString)
                && endSampleString != null
                && !endSampleString.isEmpty()) {
                if (Integer.parseInt(startSampleString) > Integer.parseInt(endSampleString)) {
                    if (!getProperty(DOEConstants.KEY_START_SAMPLE).equals(endSampleString)) {
                        setProperty(DOEConstants.KEY_START_SAMPLE, endSampleString);
                    } else {
                        startSample.setText(endSampleString);
                    }
                } else if (Integer.parseInt(startSampleString) <= Integer.parseInt(endSampleString)) {
                    setProperty(DOEConstants.KEY_START_SAMPLE, startSampleString);
                }
            }
        }
    }

    @Override
    protected DOESectionUpdater createUpdater() {
        return new DOESectionUpdater();
    }

    /**
     * DOE Section {@link DefaultUpdater} implementation of the handler to update the UI.
     * 
     * @author Kathrin Schaffert
     *
     */
    protected class DOESectionUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
            super.updateControl(control, propertyName, newValue, oldValue);
            if ((control instanceof Combo && oldValue != null) || control instanceof Table) {
                refreshDOESection();
            }
            if (control instanceof Spinner && oldValue != null) {
                fillTable();
                if (algorithmSelection.getText().equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
                    ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
                    try {
                        setPropertyNotUndoable(DOEConstants.KEY_TABLE, mapper.writeValueAsString(viewer.getInput()));
                    } catch (IOException e) {
                        LOGGER.error(e);
                    }
                }
                if (propertyName.equals(DOEConstants.KEY_RUN_NUMBER)) {
                    updateStartAndEndSample();
                }
            }
        }

        private void updateStartAndEndSample() {
            int startSampleInt = Integer.parseInt(startSample.getText());
            int endSampleInt = Integer.parseInt(endSample.getText());
            int runCount = runSpinner.getSelection();
            if (endSampleInt >= runCount) {
                if (runCount - 1 < startSampleInt) {
                    startSample.setText(String.valueOf(runCount - 1));
                }
                endSample.setText(String.valueOf(runCount - 1));
            }
        }
    }
}
