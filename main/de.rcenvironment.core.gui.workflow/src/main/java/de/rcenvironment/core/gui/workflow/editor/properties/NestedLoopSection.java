/*
 * Copyright (C) 2006-2015 DLR, Germany
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
 */
public class NestedLoopSection extends ValidatingWorkflowNodePropertySection {

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();
        final Section sectionProperties = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionProperties.setText(Messages.nestedLoopTitle);
        final Composite sectionInstallationClient = factory.createFlatFormComposite(sectionProperties);
        sectionInstallationClient.setLayout(new GridLayout(1, false));
        final Button button = factory.createButton(sectionInstallationClient, Messages.isNestedLoop, SWT.CHECK);
        button.setData(CONTROL_PROPERTY_KEY, LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP);
        button.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                widgetDefaultSelected(event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                if (button.getSelection()) {
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

    private void addOuterLoopInput(WorkflowNode node) {
        EndpointDescriptionsManager manager = node.getInputDescriptionsManager();
        manager.addDynamicEndpointDescription(LoopComponentConstants.INPUT_ID_OUTER_LOOP_DONE,
            LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE, DataType.Boolean, 
            LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
    }

    private void removeOuterLoopOutput(WorkflowNode node) {
        EndpointDescriptionsManager manager = node.getInputDescriptionsManager();
        manager.removeDynamicEndpointDescription(LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE);
    }
}
