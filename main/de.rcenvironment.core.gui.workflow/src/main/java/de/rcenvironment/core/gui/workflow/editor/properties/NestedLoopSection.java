/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Nested loop config gui.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class NestedLoopSection extends ValidatingWorkflowNodePropertySection {

    private Button nestedLoopButton;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.nestedLoopTitle);
        final Composite sectionInstallationClient = factory.createFlatFormComposite(sectionProperties);
        sectionInstallationClient.setLayout(new GridLayout(1, false));
        nestedLoopButton = factory.createButton(sectionInstallationClient, Messages.isNestedLoop, SWT.CHECK);
        nestedLoopButton.setData(CONTROL_PROPERTY_KEY, LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP);
        nestedLoopButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                widgetDefaultSelected(event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                if (nestedLoopButton.getSelection()) {
                    execute(new AddOuterLoopInputCommand());
                } else {
                    execute(new RemoveOuterLoopInputCommand());
                }
            }
        });

        factory.createLabel(sectionInstallationClient, Messages.nestedLoopNote);

        sectionProperties.setClient(sectionInstallationClient);
    }

    /**
     * Adds an input to a {@link WorkflowNode}.
     * 
     * @author Doreen Seider
     */
    private class AddOuterLoopInputCommand extends AbstractWorkflowNodeCommand {

        @Override
        protected void execute2() {
            addOuterLoopInput(getWorkflowNode());
        }

        @Override
        protected void undo2() {

            removeOuterLoopOutput(getWorkflowNode());
        }

    }

    /**
     * Removes an input to a {@link WorkflowNode}.
     * 
     * @author Doreen Seider
     */
    private class RemoveOuterLoopInputCommand extends AbstractWorkflowNodeCommand {

        @Override
        public final void execute2() {
            removeOuterLoopOutput(getWorkflowNode());
        }

        @Override
        public final void undo2() {
            addOuterLoopInput(getWorkflowNode());
        }

    }

    @Override
    protected Synchronizer createSynchronizer() {
        return new NestedLoopSynchronizer();
    }

    @Override
    protected Updater createUpdater() {
        return new NestedLoopConfigrationWidgetsUpdater();
    }

    /**
     * Updater for the nested loop button.
     * 
     * @author Sascha Zur
     */
    private class NestedLoopConfigrationWidgetsUpdater extends DefaultUpdater {

        @Override
        public void updateControl(final Control control, final String propertyName, final String newValue,
            final String oldValue) {
            if (propertyName.equals(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP) && newValue != null) {
                nestedLoopButton.setSelection(Boolean.valueOf(newValue));
            } else {
                super.updateControl(control, propertyName, newValue, oldValue);
            }
        }
    }

    /**
     * 
     * Synchronizer for the nested loop button.
     * 
     * @author Sascha Zur
     */
    private class NestedLoopSynchronizer extends DefaultSynchronizer {

        @Override
        protected void handlePropertyChange(Control control, String key, String newValue, String oldValue) {
            if (key.equals(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP)) {
                nestedLoopButton.setSelection(Boolean.valueOf(newValue));
                return;
            }
            super.handlePropertyChange(control, key, newValue, oldValue);
        }
    }

    private void addOuterLoopInput(WorkflowNode node) {
        EndpointDescriptionsManager manager = node.getInputDescriptionsManager();
        manager.addDynamicEndpointDescription(LoopComponentConstants.INPUT_ID_OUTER_LOOP_DONE,
            LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE, DataType.Boolean,
            LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        setProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, String.valueOf(true));
    }

    private void removeOuterLoopOutput(WorkflowNode node) {
        EndpointDescriptionsManager manager = node.getInputDescriptionsManager();
        manager.removeDynamicEndpointDescription(LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE);
        setProperty(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, String.valueOf(false));
    }

}
