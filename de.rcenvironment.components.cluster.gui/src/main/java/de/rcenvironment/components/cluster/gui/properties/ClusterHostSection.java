/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cluster.gui.properties;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory.LabelAndTextForProperty;
import de.rcenvironment.core.gui.workflow.executor.properties.HostSection;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;

/**
 * {@link HostSection} enhanced with configuration options for cluster, e.g. queuing system.
 * 
 * @author Doreen Seider
 */
public class ClusterHostSection extends HostSection {

    private static final String QSUB = "qsub";
    
    private static final String QSTAT = "qstat";
    
    private static final String SHOWQ = "showq";
    
    private final Updater updater = createUpdater();
    
    private Combo queuingSystemCombo;
    
    private Text qsubPathText;
    
    private Text qstatPathText;
    
    private Text showqPathText;

    private Label showQPathLabel;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {

        super.createCompositeContent(parent, aTabbedPropertySheetPage);

        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();

        Section queuingSystemSection = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        queuingSystemSection.setText(Messages.configureQueuingSystem);

        Composite queuingSystemParent = factory.createFlatFormComposite(queuingSystemSection);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        queuingSystemParent.setLayout(layout);

        Label queuingSystemLabel = new Label(queuingSystemParent, SWT.NONE);
        queuingSystemLabel.setText(Messages.queueingSystemLabel);

        GridData gridData = new GridData();
        gridData.widthHint = TEXT_WIDTH;

        queuingSystemCombo = new Combo(queuingSystemParent, SWT.DROP_DOWN | SWT.READ_ONLY);
        queuingSystemCombo.setLayoutData(gridData);
        for (ClusterQueuingSystem system : ClusterQueuingSystem.values()) {
            queuingSystemCombo.add(system.name());
        }
        qsubPathText = WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(queuingSystemParent, "Path 'qsub' (optional)",
            ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS, TEXT_WIDTH, WidgetGroupFactory.NONE).text;
        
        qstatPathText = WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(queuingSystemParent, "Path 'qstat' (optional)",
            ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS, TEXT_WIDTH, WidgetGroupFactory.NONE).text;
        
        LabelAndTextForProperty labelAndText = WidgetGroupFactory.addLabelAndTextfieldForPropertyToComposite(queuingSystemParent,
            "Path 'showq' (optional)", ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS, TEXT_WIDTH,
            WidgetGroupFactory.NONE);
        showQPathLabel = labelAndText.label;
        showqPathText = labelAndText.text;
        
        queuingSystemCombo.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                setProperty(ClusterComponentConstants.CONFIG_KEY_QUEUINGSYSTEM,
                    queuingSystemCombo.getItem(queuingSystemCombo.getSelectionIndex()));
                enableShowqWidgets(queuingSystemCombo.getSelectionIndex() == 1);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        

        queuingSystemSection.setClient(queuingSystemParent);
    }
    
    private void enableShowqWidgets(boolean enable) {
        showQPathLabel.setEnabled(enable);
        showqPathText.setEnabled(enable);
    }
    
    @Override
    protected void setWorkflowNode(WorkflowNode workflowNode) {
        super.setWorkflowNode(workflowNode);
        queuingSystemCombo.select(queuingSystemCombo.indexOf(getProperty(ClusterComponentConstants.CONFIG_KEY_QUEUINGSYSTEM)));
        enableShowqWidgets(queuingSystemCombo.getSelectionIndex() == 1);
    }

    @Override
    protected Controller createController() {
        return new PropertyController();
    }
    @Override
    protected Updater createUpdater() {
        return new PropertyUpdater();
    }
    
    @Override
    protected Synchronizer createSynchronizer() {
        return new PropertySynchronizer();
    }
    
    /**
     * Handles the queuing system command paths as they are handled with three {@link Text}s, but stored within one property.
     * @author Doreen Seoder
     */
    private class PropertyController extends DefaultController {

        @Override
        public void modifyText(final ModifyEvent event) {
            final Object source = event.getSource();
            if (source == qsubPathText) {
                controls(qsubPathText, QSUB);
            } else if (source == qstatPathText) {
                controls(qstatPathText, QSTAT);
            } else if (source == showqPathText) {
                controls(showqPathText, SHOWQ);
            } else {
                super.modifyText(event);
            }
        }
        
        private void controls(Text text, String command) {
            String oldProp = getProperty(ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS);
            Map<String, String> paths = ClusterComponentConstants.extractPathsToQueuingSystemCommands(oldProp);
            String textContent = text.getText();
            if (!textContent.equals(paths.get(command))) {
                if (editCommand == null || !editCommand.isEditable()) {
                    editCommand = editProperty(ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS);
                }
                if (textContent.isEmpty()) {
                    paths.remove(command);
                } else {
                    if (!textContent.endsWith("/")) {
                        textContent = textContent + "/";
                    }
                    paths.put(command, textContent);                    
                }
                editCommand.setNewValue(ClusterComponentConstants.getCommandsPathsAsPropertyString(paths));
            }
        }
    }
    
    /**
     * Handles the queuing system command paths as they are handled with three {@link Text}s, but stored within one property.
     * @author Doreen Seoder
     */
    protected class PropertyUpdater extends DefaultUpdater {

        @Override
        public void initializeControl(final Control control, final String propertyName, final String value) {
            updateControl(control, propertyName, value, null);
        }

        @Override
        public void updateControl(final Control control, final String propertyName, final String newValue, final String oldValue) {
            if (newValue != oldValue && propertyName.equals(ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS)) {
                Map<String, String> paths = ClusterComponentConstants.extractPathsToQueuingSystemCommands(newValue);
                if (paths.containsKey(QSUB) && !qsubPathText.getText().equals(paths.get(QSUB))) {
                    qsubPathText.setText(valueOrDefault(paths.get(QSUB), ""));
                }
                if (paths.containsKey(QSTAT) && !qstatPathText.getText().equals(paths.get(QSTAT))) {
                    qstatPathText.setText(valueOrDefault(paths.get(QSTAT), ""));
                }
                if (paths.containsKey(SHOWQ) && !showqPathText.getText().equals(paths.get(SHOWQ))) {
                    showqPathText.setText(valueOrDefault(paths.get(SHOWQ), ""));
                }
            } else {
                super.updateControl(control, propertyName, newValue, oldValue);
            }
        }
    }
    
    /**
     * Delegates to own {@link Updater} implementation.
     * 
     * @author Doreen Seider
     */
    private class PropertySynchronizer extends DefaultSynchronizer {

        @Override
        protected void handlePropertyChange(final Control control, final String key, final String newValue, final String oldValue) {
            updater.updateControl(control, key, newValue, oldValue);
        }
    }
    
}
