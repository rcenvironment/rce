/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewer;
import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerContentProvider;
import de.rcenvironment.core.gui.utils.common.configuration.ConfigurationViewerLabelProvider;
import de.rcenvironment.core.gui.utils.common.configuration.IConfigurationSource;


/**
 * {@link WorkflowNodePropertySection} for displaying and editing the properties of a workflow node.
 *
 * @author Christian Weiss
 */
public class WorkflowNodePropertiesSection extends WorkflowNodePropertySection {

    private static final String NULL_CONTROL_PROPERTY_KEY = "";

    private static final int MINIMUM_HEIGHT = 60;

    /** The content provider for the {@link #configurationViewer}. */
    private final ConfigurationViewerContentProvider configurationViewerContentProvider = new ConfigurationViewerContentProvider();

    /** The label provider for the {@link #configurationViewer}. */
    private final ConfigurationViewerLabelProvider configurationViewerLabelProvider = new ConfigurationViewerLabelProvider();

    private final ISelectionChangedListener propertyValueTextSynchronizer = new ConfigurationViewerSelectionChangedListener();

    private ConfigurationViewer configurationViewer;

    private Text propertyValueText;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();

        final Composite content = parent;
        content.setLayout(new GridLayout(1, true));
        
        GridData layoutData;
        
        layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        parent.setLayoutData(layoutData);

        final SashForm sashForm = createSash(content, SWT.VERTICAL);
        toolkit.adapt(sashForm);
    }

    private SashForm createSash(final Composite content, final int style) {
        GridData layoutData;
        // sash
        final SashForm sashForm = new SashForm(content, style);
        // configuration viewer
        configurationViewer = new ConfigurationViewer(sashForm, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE);
        layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.minimumHeight = MINIMUM_HEIGHT;
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        configurationViewer.getTree().getParent().setLayoutData(layoutData);
        configurationViewer.setContentProvider(configurationViewerContentProvider);
        configurationViewer.setLabelProvider(configurationViewerLabelProvider);
        // property value text
        propertyValueText = new Text(sashForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        propertyValueText.setText("");
        layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.minimumHeight = MINIMUM_HEIGHT;
        propertyValueText.setLayoutData(layoutData);
        propertyValueText.setData(CONTROL_PROPERTY_KEY, NULL_CONTROL_PROPERTY_KEY);
        sashForm.setWeights(new int[] { 3, 1 });
        layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        layoutData.minimumHeight = MINIMUM_HEIGHT;
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        sashForm.setLayoutData(layoutData);
        return sashForm;
    }

    @Override
    protected void afterInitializingModelBinding() {
        configurationViewer.addSelectionChangedListener(propertyValueTextSynchronizer);
    }

    @Override
    protected void beforeTearingDownModelBinding() {
        configurationViewer.removeSelectionChangedListener(propertyValueTextSynchronizer);
    }

    @Override
    protected Synchronizer createSynchronizer() {
        return new PropertiesSynchronizer();
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        configurationViewer.setInput(new PropertiesConfigurationSource(getCommandStack(), (WorkflowNode) getConfiguration()));
        configurationViewer.getTree().update();
    }

    /**
     * {@link Synchronizer} implementation to update the {@link #configurationViewer} upon property
     * changes.
     * 
     * @author Christian Weiss
     */
    protected class PropertiesSynchronizer extends DefaultSynchronizer {

        @Override
        public void handlePropertyChange(final String propertyName, final String newValue, final String oldValue) {
            super.handlePropertyChange(propertyName, newValue, oldValue);
            super.handlePropertyChange(propertyName, newValue, oldValue);
            configurationViewer.refresh();
        }

    }

    /**
     * {@link IConfigurationSource} implementation to wrap the properties of a
     * {@link de.rcenvironment.rce.component.workflow.ReadableComponentInstanceConfiguration} to
     * display them in a {@link ConfigurationViewer}.
     * 
     * @author Christian Weiss
     */
    protected class PropertiesConfigurationSource extends ComponentPropertySource implements IConfigurationSource {

        public PropertiesConfigurationSource(final CommandStack stack, final WorkflowNode node) {
            super(stack, node);
        }
        
        @Override
        public Object getPropertyValue(final Object key) {
            return getProperty((String) key);
        }

        @Override
        public void setPropertyValue(final Object key, final Object value) {
            final String propertyName = (String) key;
            String propertyValue = (String) value;
            WorkflowNodePropertiesSection.this.setProperty(propertyName, propertyValue);
        }

        @Override
        public IPropertyDescriptor[] getConfigurationPropertyDescriptors() {
            final IPropertyDescriptor[] propertyDescriptors = getPropertyDescriptors();
            final int propertyCount = propertyDescriptors.length;
            final IPropertyDescriptor[] result = new IPropertyDescriptor[propertyCount];
            for (int index = 0; index < propertyCount; ++index) {
                result[index] = propertyDescriptors[index];
            }
            return result;
        }

    }

    /**
     * {@link ISelectionChangedListener} to listen to selection changes in the
     * {@link #configurationViewer} and update the {@link #propertyValueText} with the property
     * content.
     * 
     * @author Christian Weiss
     */
    private class ConfigurationViewerSelectionChangedListener implements ISelectionChangedListener {

        @Override
        public void selectionChanged(final SelectionChangedEvent event) {
            String propertyKey = NULL_CONTROL_PROPERTY_KEY;
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.size() == 1) {
                final Object firstElement = selection.getFirstElement();
                if (firstElement instanceof TextPropertyDescriptor) {
                    final IPropertyDescriptor propertyDescriptor = (IPropertyDescriptor) firstElement;
                    propertyKey = (String) propertyDescriptor.getId();
                }
            }
            final boolean validProperty = propertyKey != null && !propertyKey.equals(NULL_CONTROL_PROPERTY_KEY);
            propertyValueText.setEnabled(validProperty);
            propertyValueText.setData(CONTROL_PROPERTY_KEY, propertyKey);
            final String value = getProperty(propertyKey);
            getUpdater().initializeControl(propertyValueText, propertyKey, value);
        }
        
    }

}
