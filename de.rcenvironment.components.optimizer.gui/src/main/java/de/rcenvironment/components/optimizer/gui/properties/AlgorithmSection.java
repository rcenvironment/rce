/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.common.OptimizerFileLoader;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * "Properties" view tab for choosing Algorithm.
 * 
 * @author Sascha Zur
 * @author Kathrin Schaffert
 */
public abstract class AlgorithmSection extends ValidatingWorkflowNodePropertySection implements PropertyChangeListener {

    private static final String COMMA = ",";

    /**
     * Selections.
     */
    private static final String COULD_NOT_PARSE_METHOD_FILE = "Could not parse method file";

    private boolean listenerRegistered = false;

    private boolean displayOnlyDiscreteMethods;

    private Combo comboMainAlgorithmSelection;

    private final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private Map<String, MethodDescription> methodDescriptions;

    private Combo comboSecondAlgorithmSelection;

    private Combo comboThirdAlgorithmSelection;

    private Composite firstAlgo;

    private Composite secondAlgo;

    private Composite thirdAlgo;

    private String[] allMethods;

    private String[] discreteMethods;

    private Label pythonLabel;

    private Label noteLabel;

    private Button dakotaPathButton;

    public AlgorithmSection() {
        loadMethods();
    }

    private void loadMethods() {
        try {
            methodDescriptions = OptimizerFileLoader.getAllMethodDescriptions(getAlgorithmFolder());
        } catch (JsonParseException e) {
            logger.error("Could not parse method file ", e);
        } catch (JsonMappingException e) {
            logger.error("Could not map method file ", e);
        } catch (IOException e) {
            logger.error("Could not load method file ", e);
        }

        Set<String> keySet = new HashSet<>();
        keySet.addAll(methodDescriptions.keySet());

        allMethods = new String[keySet.size()];
        keySet.toArray(allMethods);
        Arrays.sort(allMethods);

        List<String> list =
            keySet.stream()
                .filter(key -> methodDescriptions.get(key).getOptimizerPackage().equals(OptimizerComponentConstants.OPT_PACKAGE_DAKOTA))
                .filter(key -> methodDescriptions.get(key).getSupportsDiscreteOptimization().equals("false"))
                .collect(Collectors.toList());

        keySet.removeAll(list);
        discreteMethods = new String[keySet.size()];
        discreteMethods = keySet.toArray(new String[0]);
        Arrays.sort(discreteMethods);
    }

    protected abstract String getAlgorithmFolder();

    @Override
    public void createCompositeContent(final Composite parent, final TabbedPropertySheetPage tabbedPropertySheetPage) {
        super.createCompositeContent(parent, tabbedPropertySheetPage);
        final Section sectionAlgorithm = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent, getWidgetFactory(),
            Messages.algorithm);
        final Composite algorithmConfigurationParent = getWidgetFactory().createComposite(sectionAlgorithm);
        sectionAlgorithm.setClient(algorithmConfigurationParent);
        algorithmConfigurationParent.setLayout(new GridLayout(1, false));

        firstAlgo = new Composite(algorithmConfigurationParent, SWT.NONE);
        firstAlgo.setLayout(new GridLayout(2, false));
        new Label(firstAlgo, SWT.NONE).setText("Main algorithm");
        new Label(firstAlgo, SWT.NONE);
        comboMainAlgorithmSelection = new Combo(firstAlgo, SWT.BORDER | SWT.READ_ONLY);
        comboMainAlgorithmSelection.addListener(SWT.Selection, new SelectAlgorithmListener());
        comboMainAlgorithmSelection.setData(CONTROL_PROPERTY_KEY, OptimizerComponentConstants.ALGORITHMS);
        Button buttonProperties = new Button(firstAlgo, SWT.PUSH);
        buttonProperties.setText(Messages.algorithmProperties);
        SelectionAdapter buttonListener = new MethodSelectionAdapter(parent, comboMainAlgorithmSelection);
        buttonProperties.addSelectionListener(buttonListener);

        secondAlgo = new Composite(algorithmConfigurationParent, SWT.NONE);
        secondAlgo.setLayout(new GridLayout(2, false));
        secondAlgo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        new Label(secondAlgo, SWT.NONE).setText("Second algorithm");
        new Label(secondAlgo, SWT.NONE);
        comboSecondAlgorithmSelection = new Combo(secondAlgo, SWT.BORDER | SWT.READ_ONLY);
        comboSecondAlgorithmSelection.setItems(allMethods);
        comboSecondAlgorithmSelection.addListener(SWT.Selection, new SelectAlgorithmListener());
        comboSecondAlgorithmSelection.setData(CONTROL_PROPERTY_KEY, OptimizerComponentConstants.ALGORITHMS);
        Button buttonSecondProperties = new Button(secondAlgo, SWT.PUSH);
        buttonSecondProperties.setText(Messages.algorithmProperties);
        SelectionAdapter buttonListenerSecond = new MethodSelectionAdapter(parent, comboSecondAlgorithmSelection);
        buttonSecondProperties.addSelectionListener(buttonListenerSecond);
        secondAlgo.setVisible(false);

        thirdAlgo = new Composite(algorithmConfigurationParent, SWT.NONE);
        thirdAlgo.setLayout(new GridLayout(2, false));

        new Label(thirdAlgo, SWT.NONE).setText("Third algorithm");
        new Label(thirdAlgo, SWT.NONE);
        comboThirdAlgorithmSelection = new Combo(thirdAlgo, SWT.BORDER | SWT.READ_ONLY);
        comboThirdAlgorithmSelection.setItems(allMethods);
        comboThirdAlgorithmSelection.addListener(SWT.Selection, new SelectAlgorithmListener());
        comboThirdAlgorithmSelection.setData(CONTROL_PROPERTY_KEY, OptimizerComponentConstants.ALGORITHMS);
        Button buttonThirdProperties = new Button(thirdAlgo, SWT.PUSH);
        buttonThirdProperties.setText(Messages.algorithmProperties);
        SelectionAdapter buttonListenerThird = new MethodSelectionAdapter(parent, comboThirdAlgorithmSelection);
        buttonThirdProperties.addSelectionListener(buttonListenerThird);
        thirdAlgo.setVisible(false);

        pythonLabel = new Label(firstAlgo, SWT.NONE);
        pythonLabel.setText(Messages.pythonForMethodInstalled);
        GridData labelData = new GridData();
        labelData.horizontalSpan = 2;
        pythonLabel.setLayoutData(labelData);

        noteLabel = new Label(firstAlgo, SWT.NONE);
        noteLabel.setText(Messages.noteDiscreteMethods);
        noteLabel.setLayoutData(labelData);

        dakotaPathButton = new Button(firstAlgo, SWT.CHECK);
        dakotaPathButton.setText("Use custom dakota binary (selected at workflow start)");
        dakotaPathButton.setData(CONTROL_PROPERTY_KEY, OptimizerComponentConstants.USE_CUSTOM_DAKOTA_PATH);
        GridData pathData = new GridData();
        pathData.horizontalSpan = 2;
        dakotaPathButton.setLayoutData(pathData);

        // useRestartFileButton = new Button(firstAlgo, SWT.CHECK);
        // useRestartFileButton.setText("Use precalculated values for optimization (select file at workflow start)");
        // useRestartFileButton.setData(CONTROL_PROPERTY_KEY, OptimizerComponentConstants.USE_RESTART_FILE);
    }

    @Override
    protected void afterInitializingModelBindingWithValidation() {
        super.afterInitializingModelBindingWithValidation();
        setMethodConfigurationsIfNotExisting();
        setDisplayOnlyDiscreteMethods();
        updateMethods();
    }

    private void setDisplayOnlyDiscreteMethods() {
        if (getConfiguration().getOutputDescriptionsManager().getDynamicEndpointDescriptions().isEmpty()) {
            displayOnlyDiscreteMethods = false;
        } else {
            displayOnlyDiscreteMethods = getConfiguration().getOutputDescriptionsManager().getDynamicEndpointDescriptions().stream()
                .map(EndpointDescription.class::cast)
                .filter(x -> x.getDynamicEndpointIdentifier().equals(OptimizerComponentConstants.ID_DESIGN))
                .allMatch(x -> Boolean.parseBoolean(x.getMetaDataValue(OptimizerComponentConstants.META_IS_DISCRETE)));
        }
    }

    public void setMethodConfigurationsIfNotExisting() {
        String configString = getConfiguration().getConfigurationDescription()
            .getConfigurationValue(OptimizerComponentConstants.METHODCONFIGURATIONS);
        if (configString == null || configString.isEmpty()) {
            Map<String, MethodDescription> allConfiguredDescriptions = null;
            try {
                allConfiguredDescriptions = OptimizerFileLoader.getAllMethodDescriptions(getAlgorithmFolder());
                if (allConfiguredDescriptions != null) {
                    setPropertyNotUndoable(OptimizerComponentConstants.METHODCONFIGURATIONS,
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allConfiguredDescriptions));
                }
            } catch (JsonParseException e) {
                logger.error(e);
            } catch (JsonMappingException e) {
                logger.error(e);
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);
        if (node != null && !listenerRegistered) {
            node.addPropertyChangeListener(this);
            listenerRegistered = true;
        }
    }

    @Override
    protected void beforeTearingDownModelBinding() {
        if (node != null) {
            node.removePropertyChangeListener(this);
        }
        listenerRegistered = false;
        super.beforeTearingDownModelBinding();
    }

    /**
     * Refreshes the {@link AlgorithmSection} UI.
     */
    public void refreshAlgorithmSection() {
        String algorithm =
            getConfiguration().getConfigurationDescription()
                .getConfigurationValue(OptimizerComponentConstants.ALGORITHMS);
        if (comboMainAlgorithmSelection.isDisposed()) {
            return;
        }
        // LHS is not a optimizer method but a DoE. Although it is needed for the Surrogate Based
        // optimizer method
        if (comboMainAlgorithmSelection != null
            && Arrays.asList(comboMainAlgorithmSelection.getItems()).contains(OptimizerComponentConstants.DAKOTA_LHS)
            && !getAlgorithmFolder().equals("/doe")) {
            comboMainAlgorithmSelection.remove(OptimizerComponentConstants.DAKOTA_LHS);
        }
        if (algorithm != null) {
            final String[] selectedAlgorithm = getConfiguration().getConfigurationDescription()
                .getConfigurationValue(OptimizerComponentConstants.ALGORITHMS).split(COMMA);
            if (selectedAlgorithm != null && selectedAlgorithm.length > 0 && selectedAlgorithm[0] != null) {
                boolean ok = false;
                for (String str : comboMainAlgorithmSelection.getItems()) {
                    if (str.equals(selectedAlgorithm[0])) {
                        comboMainAlgorithmSelection.select(comboMainAlgorithmSelection.indexOf(selectedAlgorithm[0]));
                        ok = true;
                    }
                }
                if (!ok) {
                    comboMainAlgorithmSelection.deselectAll();
                    secondAlgo.setVisible(false);
                    thirdAlgo.setVisible(false);
                    setProperty(OptimizerComponentConstants.ALGORITHMS, "");
                }
            }
            refreshShownAlgorithms();
            if (secondAlgo.isVisible() && selectedAlgorithm != null && selectedAlgorithm.length > 1 && selectedAlgorithm[1] != null) {
                boolean ok = false;
                for (String str : comboSecondAlgorithmSelection.getItems()) {
                    if (str.equals(selectedAlgorithm[1])) {
                        comboSecondAlgorithmSelection.select(comboSecondAlgorithmSelection.indexOf(selectedAlgorithm[1]));
                        ok = true;
                    }
                }
                if (!ok) {
                    comboSecondAlgorithmSelection.select(0);
                }
            }
            if (thirdAlgo.isVisible() && selectedAlgorithm != null && selectedAlgorithm.length > 2 && selectedAlgorithm[2] != null) {
                boolean ok = false;
                for (String str : comboThirdAlgorithmSelection.getItems()) {
                    if (str.equals(selectedAlgorithm[2])) {
                        comboThirdAlgorithmSelection.select(comboThirdAlgorithmSelection.indexOf(selectedAlgorithm[2]));
                        ok = true;
                    }
                }
                if (!ok) {
                    comboThirdAlgorithmSelection.select(0);
                }
            }
            String configString = getConfiguration().getConfigurationDescription()
                .getConfigurationValue(OptimizerComponentConstants.METHODCONFIGURATIONS);
            if (configString == null || configString.equals("")) {
                try {
                    setProperty(OptimizerComponentConstants.METHODCONFIGURATIONS,
                        mapper.writer().writeValueAsString(methodDescriptions));
                } catch (JsonParseException e) {
                    logger.error(COULD_NOT_PARSE_METHOD_FILE, e);
                } catch (JsonMappingException e) {
                    logger.error(COULD_NOT_PARSE_METHOD_FILE, e);
                } catch (IOException e) {
                    logger.error(COULD_NOT_PARSE_METHOD_FILE, e);
                }
            }
        } else {
            secondAlgo.setVisible(false);
            thirdAlgo.setVisible(false);
            comboMainAlgorithmSelection.setText("");
        }
    }

    private void refreshShownAlgorithms() {
        try {
            String configString = getConfiguration().getConfigurationDescription()
                .getConfigurationValue(OptimizerComponentConstants.METHODCONFIGURATIONS);
            comboMainAlgorithmSelection.setText("");
            @SuppressWarnings("unchecked") Map<String, MethodDescription> allConfiguredDescriptions =
                mapper.readValue(configString, new HashMap<String, MethodDescription>().getClass());
            MethodDescription mainAlgorithm = mapper.convertValue(
                allConfiguredDescriptions.get(comboMainAlgorithmSelection.getText()), MethodDescription.class);
            if (mainAlgorithm != null && mainAlgorithm.getOptimizerPackage() != null) {
                pythonLabel.setVisible(true);
                noteLabel.setVisible(true);
                if (mainAlgorithm.getOptimizerPackage().equalsIgnoreCase(OptimizerComponentConstants.OPT_PACKAGE_DAKOTA)) {
                    pythonLabel.setText(Messages.dakotaOSHint);
                    dakotaPathButton.setVisible(true);
                } else {
                    dakotaPathButton.setVisible(false);
                    noteLabel.setVisible(false);
                    pythonLabel.setText(Messages.pythonForMethodInstalled);
                }
                pythonLabel.getParent().pack();
                if (mainAlgorithm.getOptimizerPackage().equals(OptimizerComponentConstants.OPT_PACKAGE_DAKOTA)
                    && mainAlgorithm.getMethodName().contains(OptimizerComponentConstants.DAKOTA_SGB)) {
                    comboSecondAlgorithmSelection.removeAll();
                    comboThirdAlgorithmSelection.removeAll();
                    // if SGB is chosen, only Dakota LHS is available for the second algorithm
                    comboSecondAlgorithmSelection.add(OptimizerComponentConstants.DAKOTA_LHS);
                    comboThirdAlgorithmSelection.setItems(
                        mainAlgorithm.getSpecificSettings().get(OptimizerComponentConstants.APPROX_METHOD_KEY)
                            .get(OptimizerComponentConstants.DEFAULT_VALUE_KEY).split(COMMA));
                    secondAlgo.setVisible(true);
                    thirdAlgo.setVisible(true);
                    comboSecondAlgorithmSelection.select(comboSecondAlgorithmSelection.indexOf(OptimizerComponentConstants.DAKOTA_LHS));
                    comboThirdAlgorithmSelection.select(0);

                } else {

                    if (mainAlgorithm.getFollowingMethods() != null && mainAlgorithm.getFollowingMethods().equals("1")) {
                        comboSecondAlgorithmSelection.removeAll();
                        filterMethodsList(mainAlgorithm, comboSecondAlgorithmSelection);
                        secondAlgo.setVisible(true);
                        thirdAlgo.setVisible(false);
                        comboSecondAlgorithmSelection.select(0);
                    } else if (mainAlgorithm.getFollowingMethods() != null && mainAlgorithm.getFollowingMethods().equals("2")) {
                        comboSecondAlgorithmSelection.removeAll();
                        comboThirdAlgorithmSelection.removeAll();
                        filterMethodsList(mainAlgorithm, comboSecondAlgorithmSelection);
                        filterMethodsList(mainAlgorithm, comboThirdAlgorithmSelection);
                        secondAlgo.setVisible(true);
                        thirdAlgo.setVisible(true);
                        comboSecondAlgorithmSelection.select(0);
                        comboThirdAlgorithmSelection.select(0);

                    } else {
                        secondAlgo.setVisible(false);
                        thirdAlgo.setVisible(false);
                    }

                }
            } else {
                comboMainAlgorithmSelection.setText("");
                pythonLabel.setVisible(false);
                noteLabel.setVisible(false);
            }
        } catch (JsonParseException e1) {
            logger.error(COULD_NOT_PARSE_METHOD_FILE, e1);
        } catch (JsonMappingException e1) {
            logger.error(COULD_NOT_PARSE_METHOD_FILE, e1);
        } catch (IOException e1) {
            logger.error(COULD_NOT_PARSE_METHOD_FILE, e1);
        }
    }

    private void filterMethodsList(MethodDescription mainAlgorithm, Combo combo) {
        List<String> result = new LinkedList<String>();
        result.add("");
        for (String currentAlgorithm : methodDescriptions.keySet()) {
            if (methodDescriptions.get(currentAlgorithm).getOptimizerPackage().equalsIgnoreCase(mainAlgorithm.getOptimizerPackage())
                && !currentAlgorithm.equalsIgnoreCase(mainAlgorithm.getMethodName())) {
                result.add(currentAlgorithm);
            }
        }
        String[] methodList = new String[result.size()];
        result.toArray(methodList);
        Arrays.sort(methodList);
        combo.setItems(methodList);
    }

    protected void updateMethods() {
        if (displayOnlyDiscreteMethods) {
            comboMainAlgorithmSelection.setItems(discreteMethods);
        } else {
            comboMainAlgorithmSelection.setItems(allMethods);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals(EndpointDescriptionsManager.PROPERTY_ENDPOINT)) {

            List<EndpointDescription> endpointList = (((EndpointDescriptionsManager) evt.getSource()).getDynamicEndpointDescriptions())
                .stream().map(EndpointDescription.class::cast)
                .filter(x -> x.getDynamicEndpointIdentifier().equals(OptimizerComponentConstants.ID_DESIGN)).collect(Collectors.toList());

            if (endpointList.isEmpty()) {
                return;
            }
            boolean isDiscrete = endpointList.stream()
                .allMatch(x -> Boolean.parseBoolean(x.getMetaDataValue(OptimizerComponentConstants.META_IS_DISCRETE)));

            if (displayOnlyDiscreteMethods != isDiscrete) {
                displayOnlyDiscreteMethods = isDiscrete;
                updateMethods();
                refreshAlgorithmSection();
            }
        }
    }

    /**
     * Selectionadapter for the dakota algorithms.
     * 
     * @author Sascha Zur
     */
    private class MethodSelectionAdapter extends SelectionAdapter {

        private final Composite parent;

        private final Combo attachedCombo;

        MethodSelectionAdapter(Composite parent, Combo attachedCombo) {
            this.parent = parent;
            this.attachedCombo = attachedCombo;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void widgetSelected(SelectionEvent e) {
            String configString = getConfiguration().getConfigurationDescription()
                .getConfigurationValue(OptimizerComponentConstants.METHODCONFIGURATIONS);
            try {

                Map<String, MethodDescription> allConfiguredDescriptions = null;
                if (configString == null || configString.isEmpty()) {
                    allConfiguredDescriptions = OptimizerFileLoader.getAllMethodDescriptions(getAlgorithmFolder());
                } else {
                    allConfiguredDescriptions = mapper.readValue(configString, new HashMap<String, MethodDescription>().getClass());
                }
                // Because the ObjectMapper doesn't create the MethodDescription objects but
                // HashMaps in the previous step,
                // the description to manipulate must be converted into the correct class.
                if (allConfiguredDescriptions != null) {
                    allConfiguredDescriptions.put(attachedCombo.getText(),
                        mapper.convertValue(allConfiguredDescriptions.get(attachedCombo.getText()), MethodDescription.class));
                    if (attachedCombo.getText().equals("")) {
                        MessageBox dialog =
                            new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.OK);
                        dialog.setText("No Algorithm");
                        dialog.setMessage("No algorithm selected!");
                        dialog.open();
                    } else {
                        MethodPropertiesDialogGenerator dialog = new MethodPropertiesDialogGenerator(
                            parent.getShell(), allConfiguredDescriptions.get(attachedCombo.getText()));
                        if (dialog.open() == Dialog.OK) {
                            setProperty(OptimizerComponentConstants.METHODCONFIGURATIONS,
                                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allConfiguredDescriptions));
                        }
                    }

                }
            } catch (JsonParseException e1) {
                logger.error(COULD_NOT_PARSE_METHOD_FILE, e1);
            } catch (JsonMappingException e1) {
                logger.error("Could not map method file", e1);
            } catch (IOException e1) {
                logger.error("Could not load method file", e1);
            }
        }

    }

    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);
    }

    /**
     * Listener for the Algorithm Combobox.
     * 
     * @author Sascha Zur
     */
    private class SelectAlgorithmListener implements Listener {

        @SuppressWarnings("unchecked")
        @Override
        public void handleEvent(final Event event) {
            String key = comboMainAlgorithmSelection.getItem(comboMainAlgorithmSelection.getSelectionIndex());
            if (event.widget.equals(comboMainAlgorithmSelection)) {
                refreshShownAlgorithms();
                String configString = getConfiguration().getConfigurationDescription()
                    .getConfigurationValue(OptimizerComponentConstants.METHODCONFIGURATIONS);
                try {

                    Map<String, MethodDescription> allConfiguredDescriptions = null;
                    if (configString == null || configString.isEmpty()) {
                        allConfiguredDescriptions = OptimizerFileLoader.getAllMethodDescriptions(getAlgorithmFolder());
                    } else {
                        allConfiguredDescriptions = mapper.readValue(configString, new HashMap<String, MethodDescription>().getClass());
                    }
                    if (!allConfiguredDescriptions.containsKey(key)) {
                        MethodDescription desc = OptimizerFileLoader.loadMethod(key);
                        if (desc != null) {
                            allConfiguredDescriptions.put(key, desc);
                            setProperty(OptimizerComponentConstants.METHODCONFIGURATIONS,
                                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allConfiguredDescriptions));
                        }
                    }
                } catch (JsonParseException e1) {
                    logger.error(COULD_NOT_PARSE_METHOD_FILE, e1);
                } catch (JsonMappingException e1) {
                    logger.error("Could not map method file", e1);
                } catch (IOException e1) {
                    logger.error("Could not load method file", e1);
                }

            }
            if (secondAlgo.isVisible() && comboSecondAlgorithmSelection.getSelectionIndex() >= 0
                && !comboSecondAlgorithmSelection.getItem(comboSecondAlgorithmSelection.getSelectionIndex()).equals("")) {
                key += COMMA + comboSecondAlgorithmSelection.getItem(comboSecondAlgorithmSelection.getSelectionIndex());
            }
            if (thirdAlgo.isVisible() && comboThirdAlgorithmSelection.getSelectionIndex() >= 0) {
                key += COMMA + comboThirdAlgorithmSelection.getItem(comboThirdAlgorithmSelection.getSelectionIndex());
            }

            setProperties(OptimizerComponentConstants.ALGORITHMS, key, OptimizerComponentConstants.OPTIMIZER_PACKAGE,
                methodDescriptions.get(comboMainAlgorithmSelection.getText()).getOptimizerPackage());
        }
    }

    @Override
    protected AlgorithmSectionUpdater createUpdater() {
        return new AlgorithmSectionUpdater();
    }

    /**
     * Algorithm Section {@link DefaultUpdater} implementation of the handler to update the Algorithm Section UI.
     * 
     * @author Kathrin Schaffert
     *
     */
    protected class AlgorithmSectionUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
            super.updateControl(control, propertyName, newValue, oldValue);

            if (propertyName.equals(OptimizerComponentConstants.ALGORITHMS) && control instanceof Combo && !newValue.equals(oldValue)) {
                if (newValue.equals("")) {
                    setMethodConfigurationsIfNotExisting();
                }
                refreshAlgorithmSection();
            }
        }
    }
}
