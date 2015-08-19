/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.wizards.toolintegration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.cpacs.component.integration.CpacsToolIntegrationConstants;

/**
 * Wizard page for cpacs specific tool integration configuration.
 * 
 * @author Jan Flink
 */
public class CpacsConfigurationPage extends ToolIntegrationWizardPage {

    private static final int TITLE_HEIGHT = 25;

    private static final String EMPTY_STRING = "";

    private static final String COLON = ":";

    private static final int INDEX_NULL = -1;

    protected final Log logger = LogFactory.getLog(getClass());

    private Map<String, Object> configurationMap;

    private Button alwaysRunCheckbox;

    private Button toolSpecificInputCheckbox;

    private Label labelIncomingCpacsEndpoint;

    private Combo fileEndpointCombo;

    private Label labelInputMappingFile;

    private Label separator1;

    private Text mappingInputFilename;

    private Button mappingInputFileChooser;

    private Label labelOutputMappingFile;

    private Text mappingOutputFilename;

    private Button mappingOutputFileChooser;

    private Label separator2;

    private Label labelCpacsResultFilename;

    private Text cpacsResultFilename;

    private Label labelToolSpecificInput;

    private Label labelToolSpecificMapping;

    private Text toolSpecificInputdataFilename;

    private Text toolSpecificMappingFilename;

    private Button toolSpecificInputFileChooser;

    private Button toolSpecificMappingFileChooser;

    private Label labelToolInputFilename;

    private Label labelToolOutputFilename;

    private Text toolInputFilename;

    private Text toolOutputFilename;

    private final List<RelativeXMLFilePathChooserButtonListener> fileChooseListener =
        new ArrayList<RelativeXMLFilePathChooserButtonListener>();

    private Label labelOutgoingCpacsEndpoint;

    private Combo fileOutgoingEndpointCombo;

    protected CpacsConfigurationPage(String pageName, Map<String, Object> configurationMap) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.cpacsPageDescription);
        this.configurationMap = configurationMap;
        initDynamicEndpointConfiguration();
    }

    @SuppressWarnings("unchecked")
    private void initDynamicEndpointConfiguration() {
        if (configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE) != null
            && configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE).equals(
                CpacsToolIntegrationConstants.CPACS_TOOL_INTEGRATION_CONTEXT_TYPE)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, List<Map<String, Object>>> inputs =
                    mapper.readValue(getClass().getResource("/resources/inputTemplate.json"),
                        new HashMap<String, List<Map<String, Object>>>().getClass());
                Map<String, List<Map<String, Object>>> outputs =
                    mapper.readValue(getClass().getResource("/resources/outputTemplate.json"),
                        new HashMap<String, List<Map<String, Object>>>().getClass());
                configurationMap.put("dynamicInputs", inputs.get("dynamicInputs"));
                configurationMap.put("dynamicOutputs", outputs.get("dynamicOutputs"));
            } catch (IOException e) {
                logger.error("Could not read templates for dynamic inputs/outputs. " + e);
            }
        }
    }

    // TODO this is a workaround --> implement a general mechanism to set default values in common
    // tool integration
    private void setDefaultConfigurations() {
        if (configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE) != null
            && configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE).equals(
                CpacsToolIntegrationConstants.CPACS_TOOL_INTEGRATION_CONTEXT_TYPE)) {
            configurationMap.put(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR, Boolean.TRUE);
        }
    }

    @Override
    public void setConfigMap(Map<String, Object> newConfigurationMap) {
        this.configurationMap = newConfigurationMap;
        initDynamicEndpointConfiguration();
        setDefaultConfigurations();
        updateSelectionListeners();
    }

    private void updateSelectionListeners() {
        for (RelativeXMLFilePathChooserButtonListener l : fileChooseListener) {
            l.updateConfiguration(configurationMap);
        }
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout(3, true);
        container.setLayout(layout);
        GridData containerData =
            new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        container.setLayoutData(containerData);

        Label mappingTitle = new Label(container, SWT.NONE);
        GridData gridDataMappingTitle = new GridData(SWT.LEFT, SWT.TOP, false, false, 3, 1);
        gridDataMappingTitle.heightHint = TITLE_HEIGHT;
        mappingTitle.setLayoutData(gridDataMappingTitle);
        mappingTitle.setText(Messages.mappingTitle);

        createIncomingCpacsEndpointConfig(container);
        new Label(container, SWT.NONE);

        createInputMappingConfig(container);

        createToolInputConfig(container);
        new Label(container, SWT.NONE);

        createToolOutputConfig(container);
        new Label(container, SWT.NONE);

        createToolOutputMappingConfig(container);

        createCpacsResultConfig(container);

        createOutgoingCpacsEndpointConfig(container);

        separator1 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        createToolSpecificInputConfig(container);

        separator2 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        createAlwaysRunConfig(container);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        setControl(container);
        updatePageComplete();
    }

    private void createAlwaysRunConfig(Composite container) {
        alwaysRunCheckbox = new Button(container, SWT.CHECK);
        alwaysRunCheckbox.setText(Messages.alwaysRunCheckbox);
        alwaysRunCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(CpacsToolIntegrationConstants.KEY_ALWAYS_RUN, (!alwaysRunCheckbox.getSelection()));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
    }

    private void enableToolspecific(boolean enable) {
        labelToolSpecificInput.setEnabled(enable);
        toolSpecificInputdataFilename.setEnabled(enable);
        labelToolSpecificMapping.setEnabled(enable);
        toolSpecificMappingFilename.setEnabled(enable);
        toolSpecificInputFileChooser.setEnabled(enable);
        toolSpecificMappingFileChooser.setEnabled(enable);
        if (!enable) {
            toolSpecificInputdataFilename.setText(EMPTY_STRING);
            toolSpecificMappingFilename.setText(EMPTY_STRING);
        }
    }

    private void createToolSpecificInputConfig(Composite container) {
        toolSpecificInputCheckbox = new Button(container, SWT.CHECK);
        toolSpecificInputCheckbox.setText(Messages.toolSpecificInputCheckbox);
        toolSpecificInputCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE).equals(
                    CpacsToolIntegrationConstants.CPACS_TOOL_INTEGRATION_CONTEXT_TYPE)) {
                    configurationMap.put(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT,
                        (toolSpecificInputCheckbox.getSelection()));
                }
                enableToolspecific(toolSpecificInputCheckbox.getSelection());
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        labelToolSpecificInput = new Label(container, SWT.NONE);
        labelToolSpecificInput.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelToolSpecificInput.setText(Messages.labelToolSpecificInput + COLON);
        labelToolSpecificInput.setEnabled(false);

        toolSpecificInputdataFilename = new Text(container, SWT.BORDER);
        toolSpecificInputdataFilename.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        toolSpecificInputdataFilename.setEnabled(false);
        toolSpecificInputdataFilename.addModifyListener(new TextAreaModifyListener(
            CpacsToolIntegrationConstants.KEY_TOOLSPECIFICINPUTDATA_FILENAME));

        toolSpecificInputFileChooser = new Button(container, SWT.NONE);
        toolSpecificInputFileChooser.setText(Messages.fileChooser);
        RelativeXMLFilePathChooserButtonListener toolSpecificInputFileChooserListener =
            new RelativeXMLFilePathChooserButtonListener(Messages.labelToolInputFilename,
                toolSpecificInputdataFilename,
                getShell(), configurationMap);
        toolSpecificInputFileChooser.addSelectionListener(toolSpecificInputFileChooserListener);
        fileChooseListener.add(toolSpecificInputFileChooserListener);
        toolSpecificInputFileChooser.setEnabled(false);

        labelToolSpecificMapping = new Label(container, SWT.NONE);
        labelToolSpecificMapping.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelToolSpecificMapping.setText(Messages.labelToolSpecificMapping + COLON);
        labelToolSpecificMapping.setEnabled(false);

        toolSpecificMappingFilename = new Text(container, SWT.BORDER);
        toolSpecificMappingFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        toolSpecificMappingFilename.setEnabled(false);
        toolSpecificMappingFilename.addModifyListener(new TextAreaModifyListener(
            CpacsToolIntegrationConstants.KEY_TOOLSPECIFICMAPPING_FILENAME));

        toolSpecificMappingFileChooser = new Button(container, SWT.NONE);
        toolSpecificMappingFileChooser.setText(Messages.fileChooser);
        RelativeXMLFilePathChooserButtonListener toolSpecificMappingFileChooserListener =
            new RelativeXMLFilePathChooserButtonListener(Messages.labelToolSpecificMapping,
                toolSpecificMappingFilename,
                getShell(), configurationMap);
        toolSpecificMappingFileChooser.addSelectionListener(toolSpecificMappingFileChooserListener);
        fileChooseListener.add(toolSpecificMappingFileChooserListener);
        toolSpecificMappingFileChooser.setEnabled(false);
    }

    private void createCpacsResultConfig(Composite container) {
        labelCpacsResultFilename = new Label(container, SWT.NONE);
        labelCpacsResultFilename.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelCpacsResultFilename.setText(Messages.labelCpacsResultFilename + COLON);

        cpacsResultFilename = new Text(container, SWT.BORDER);
        cpacsResultFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cpacsResultFilename.addModifyListener(new TextAreaModifyListener(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME));
        new Label(container, SWT.NONE);
    }

    private void createToolOutputMappingConfig(Composite container) {
        labelOutputMappingFile = new Label(container, SWT.NONE);
        labelOutputMappingFile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelOutputMappingFile.setText(Messages.labelOutputMappingFile + COLON);

        mappingOutputFilename = new Text(container, SWT.BORDER);
        mappingOutputFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mappingOutputFilename.addModifyListener(new TextAreaModifyListener(CpacsToolIntegrationConstants.KEY_MAPPING_OUTPUT_FILENAME));

        mappingOutputFileChooser = new Button(container, SWT.NONE);
        mappingOutputFileChooser.setText(Messages.fileChooser);
        RelativeXMLFilePathChooserButtonListener mappingOutputFileChooserListener =
            new RelativeXMLFilePathChooserButtonListener(Messages.labelOutputMappingFile,
                mappingOutputFilename, getShell(),
                configurationMap);
        mappingOutputFileChooser.addSelectionListener(mappingOutputFileChooserListener);
        fileChooseListener.add(mappingOutputFileChooserListener);
    }

    private void createToolOutputConfig(Composite container) {
        labelToolOutputFilename = new Label(container, SWT.NONE);
        labelToolOutputFilename.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelToolOutputFilename.setText(Messages.labelToolOutputFilename + COLON);

        toolOutputFilename = new Text(container, SWT.BORDER);
        toolOutputFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        toolOutputFilename.addModifyListener(new TextAreaModifyListener(CpacsToolIntegrationConstants.KEY_TOOL_OUTPUT_FILENAME));
    }

    private void createToolInputConfig(Composite container) {
        labelToolInputFilename = new Label(container, SWT.NONE);
        labelToolInputFilename.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelToolInputFilename.setText(Messages.labelToolInputFilename + COLON);

        toolInputFilename = new Text(container, SWT.BORDER);
        toolInputFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        toolInputFilename.addModifyListener(new TextAreaModifyListener(CpacsToolIntegrationConstants.KEY_TOOL_INPUT_FILENAME));
    }

    private void createInputMappingConfig(Composite container) {
        labelInputMappingFile = new Label(container, SWT.NONE);
        labelInputMappingFile.setText(Messages.labelInputMappingFile + COLON);
        labelInputMappingFile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

        mappingInputFilename = new Text(container, SWT.BORDER);
        mappingInputFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mappingInputFilename.addModifyListener(new TextAreaModifyListener(CpacsToolIntegrationConstants.KEY_MAPPING_INPUT_FILENAME));

        mappingInputFileChooser = new Button(container, SWT.NONE);
        mappingInputFileChooser.setText(Messages.fileChooser);
        RelativeXMLFilePathChooserButtonListener mappingInputFileChooserListener =
            new RelativeXMLFilePathChooserButtonListener(Messages.labelInputMappingFile,
                mappingInputFilename, getShell(),
                configurationMap);
        mappingInputFileChooser.addSelectionListener(mappingInputFileChooserListener);
        fileChooseListener.add(mappingInputFileChooserListener);
    }

    private void createIncomingCpacsEndpointConfig(Composite container) {
        labelIncomingCpacsEndpoint = new Label(container, SWT.NONE);
        labelIncomingCpacsEndpoint.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelIncomingCpacsEndpoint.setText(Messages.labelIncomingCpacsEndpoint + COLON);

        fileEndpointCombo = new Combo(container, SWT.READ_ONLY);
        fileEndpointCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fileEndpointCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                configurationMap.put(CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME,
                    fileEndpointCombo.getItem(fileEndpointCombo.getSelectionIndex()));
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
    }

    private void createOutgoingCpacsEndpointConfig(Composite container) {
        labelOutgoingCpacsEndpoint = new Label(container, SWT.NONE);
        labelOutgoingCpacsEndpoint.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        labelOutgoingCpacsEndpoint.setText(Messages.labelOutgoingCpacsEndpoint + COLON);

        fileOutgoingEndpointCombo = new Combo(container, SWT.READ_ONLY);
        fileOutgoingEndpointCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        fileOutgoingEndpointCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                configurationMap.put(CpacsToolIntegrationConstants.KEY_CPACS_OUTGOING_ENDPOINTNAME,
                    fileOutgoingEndpointCombo.getItem(fileOutgoingEndpointCombo.getSelectionIndex()));
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
    }

    @Override
    public void updatePage() {
        if (getControl() != null) {
            updateComboBoxes(fileEndpointCombo, ToolIntegrationConstants.KEY_ENDPOINT_INPUTS);
            updateComboBoxes(fileOutgoingEndpointCombo, ToolIntegrationConstants.KEY_ENDPOINT_OUTPUTS);

            if (fileEndpointCombo.getSelectionIndex() < 0
                && configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME) != null
                && configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME) != EMPTY_STRING) {
                configurationMap.put(CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME, EMPTY_STRING);
            }

            if ((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_INPUT_FILENAME) != null) {
                mappingInputFilename.setText((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_INPUT_FILENAME));
            } else {
                mappingInputFilename.setText(EMPTY_STRING);
            }

            if ((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_INPUT_FILENAME) != null) {
                toolInputFilename.setText((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_INPUT_FILENAME));
            } else {
                toolInputFilename.setText(EMPTY_STRING);
            }

            if ((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_OUTPUT_FILENAME) != null) {
                toolOutputFilename.setText((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_OUTPUT_FILENAME));
            } else {
                toolOutputFilename.setText(EMPTY_STRING);
            }

            if ((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_OUTPUT_FILENAME) != null) {
                mappingOutputFilename.setText((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_OUTPUT_FILENAME));
            } else {
                mappingOutputFilename.setText(EMPTY_STRING);
            }

            if ((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME) != null) {
                cpacsResultFilename.setText((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME));
            } else {
                cpacsResultFilename.setText(EMPTY_STRING);
            }

            if ((Boolean) configurationMap.get(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT) == null) {
                if (configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE).equals(
                    CpacsToolIntegrationConstants.CPACS_TOOL_INTEGRATION_CONTEXT_TYPE)) {
                    configurationMap.put(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT, false);
                }
            }
            if ((Boolean) configurationMap.get(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT) != null) {
                toolSpecificInputCheckbox.setSelection((Boolean) configurationMap
                    .get(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT));
            }
            enableToolspecific(toolSpecificInputCheckbox.getSelection());

            if ((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICINPUTDATA_FILENAME) != null) {
                toolSpecificInputdataFilename.setText((String) configurationMap
                    .get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICINPUTDATA_FILENAME));
            } else {
                toolSpecificInputdataFilename.setText(EMPTY_STRING);
            }

            if ((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICMAPPING_FILENAME) != null) {
                toolSpecificMappingFilename.setText((String) configurationMap
                    .get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICMAPPING_FILENAME));
            } else {
                toolSpecificMappingFilename.setText(EMPTY_STRING);
            }

            if ((Boolean) configurationMap.get(CpacsToolIntegrationConstants.KEY_ALWAYS_RUN) == null) {
                configurationMap.put(CpacsToolIntegrationConstants.KEY_ALWAYS_RUN, true);
            }
            alwaysRunCheckbox.setSelection(!(Boolean) configurationMap.get(CpacsToolIntegrationConstants.KEY_ALWAYS_RUN));

            updatePageComplete();
        }

    }

    private void updateComboBoxes(Combo combo, String direction) {
        String type;
        if (direction.equals(ToolIntegrationConstants.KEY_ENDPOINT_OUTPUTS)) {
            type = CpacsToolIntegrationConstants.KEY_CPACS_OUTGOING_ENDPOINTNAME;
        } else {
            type = CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME;
        }

        addAllFileEndpoints(combo, direction, type);

        if (combo.getItemCount() > 0 && combo.getSelectionIndex() >= 0 && ((String) configurationMap.get(type) == null
            || ((String) configurationMap.get(type)).equals(EMPTY_STRING))) {
            configurationMap.put(type,
                combo.getItem(combo.getSelectionIndex()));
        }

        String cpacsEndpoint = (String) configurationMap.get(type);
        if (cpacsEndpoint != null && combo.indexOf(cpacsEndpoint) != INDEX_NULL) {
            combo.select(combo.indexOf(cpacsEndpoint));
        } else if (cpacsEndpoint != null && combo.indexOf(cpacsEndpoint) == INDEX_NULL) {
            configurationMap.put(type, EMPTY_STRING);
        }
    }

    private void addAllFileEndpoints(Combo combo, String direction, String type) {
        combo.removeAll();
        @SuppressWarnings("unchecked") List<Map<String, String>> endpointList =
            (List<Map<String, String>>) configurationMap.get(direction);
        if (endpointList != null) {
            for (Map<String, String> endpoint : endpointList) {
                // add only if data type equals {@link DataType.FileReference}
                if (DataType.valueOf(endpoint.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE)) == DataType.FileReference) {
                    combo.add(endpoint.get(ToolIntegrationConstants.KEY_ENDPOINT_NAME));
                }
            }
            if (combo.getItemCount() > 0 && configurationMap.get(type) == null) {
                combo.select(0);
            }
        }
    }

    private void updatePageComplete() {

        boolean configsNotNull = (Boolean) configurationMap.get(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT) != null
            && (Boolean) configurationMap.get(CpacsToolIntegrationConstants.KEY_ALWAYS_RUN) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICMAPPING_FILENAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICINPUTDATA_FILENAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_INPUT_FILENAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_INPUT_FILENAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_OUTPUT_FILENAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_OUTPUT_FILENAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME) != null
            && (String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_OUTGOING_ENDPOINTNAME) != null;

        boolean generalConfigNotEmpty = configsNotNull
            && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_INITIAL_ENDPOINTNAME)).equals(EMPTY_STRING)
            && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_OUTGOING_ENDPOINTNAME)).equals(EMPTY_STRING)
            && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_INPUT_FILENAME)).equals(EMPTY_STRING)
            && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_INPUT_FILENAME)).equals(EMPTY_STRING)
            && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOL_OUTPUT_FILENAME)).equals(EMPTY_STRING)
            && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_MAPPING_OUTPUT_FILENAME)).equals(EMPTY_STRING)
            && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_CPACS_RESULT_FILENAME)).equals(EMPTY_STRING);

        boolean toolSpecificInputConfigNotEmpty = configsNotNull
            && (!(Boolean) configurationMap.get(CpacsToolIntegrationConstants.KEY_HAS_TOOLSPECIFIC_INPUT)
            || (!((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICMAPPING_FILENAME)).equals(EMPTY_STRING))
                && !((String) configurationMap.get(CpacsToolIntegrationConstants.KEY_TOOLSPECIFICINPUTDATA_FILENAME)).equals(EMPTY_STRING));

        if (fileEndpointCombo.getItemCount() == 0) {
            setMessage(Messages.errorNoInput, DialogPage.ERROR);
        } else {
            setMessage(null, DialogPage.NONE);
        }
        if (fileOutgoingEndpointCombo.getItemCount() == 0 && getMessage() == null) {
            setMessage(Messages.errorNoOutput, DialogPage.ERROR);
        }

        if (generalConfigNotEmpty && toolSpecificInputConfigNotEmpty) {
            setPageComplete(true);
            setMessage(null, DialogPage.NONE);
        } else {
            setPageComplete(false);
            if (getMessage() == null) {
                setMessage(Messages.configurationValueMissing, DialogPage.ERROR);
            }
        }

    }

    /**
     * Listener for the script text areas for saving the content to the correct key.
     * 
     * @author Jan Flink
     */
    private class TextAreaModifyListener implements ModifyListener {

        private final String key;

        public TextAreaModifyListener(String key) {
            this.key = key;
        }

        @Override
        public void modifyText(ModifyEvent arg0) {
            if (configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE).equals(
                CpacsToolIntegrationConstants.CPACS_TOOL_INTEGRATION_CONTEXT_TYPE)) {
                configurationMap.put(key, ((Text) arg0.getSource()).getText());
            }
            updatePageComplete();
        }
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp("de.rcenvironment.cpacs.gui.wizard.toolintegration.integration_cpacs");
    }
}
