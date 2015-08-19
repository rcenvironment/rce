/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cluster.gui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.core.gui.workflow.executor.properties.AbstractScriptSection;


/**
 * "Properties" view tab for configuring job script used for job submission.
 *
 * @author Doreen Seider
 */
public class ScriptSection extends AbstractScriptSection {

    private Button isScriptProvidedbutton;

    public ScriptSection() {
        super(AbstractScriptSection.ALL, Messages.scriptname);
    }
    
    @Override
    protected void createCompositeContentAtVeryTop(Composite composite, TabbedPropertySheetWidgetFactory factory) {
        isScriptProvidedbutton = factory.createButton(composite,
            String.format(Messages.isScriptProvided, ClusterComponentConstants.JOB_SCRIPT_NAME), SWT.CHECK);
        isScriptProvidedbutton.setData(CONTROL_PROPERTY_KEY, ClusterComponentConstants.KEY_IS_SCRIPT_PROVIDED_WITHIN_INPUT_DIR);
        super.createCompositeContentAtVeryTop(composite, factory);
        
        factory.createCLabel(composite, "Note: Component is marked as failed, if a file with name 'cluster_job_failed' exists "
            + "in at least one of the job's output directories. See F1 help for more details.");
    }
    
    @Override
    protected Updater createUpdater() {
        return new DefaultUpdater() {
            
            @Override
            public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
                super.updateControl(control, propertyName, newValue, oldValue);
                handleUpdate(propertyName, newValue);
            }
            
            @Override
            public void initializeControl(Control control, String propertyName, String value) {
                super.initializeControl(control, propertyName, value);
                handleUpdate(propertyName, value);
            }
            
            private void handleUpdate(String propertyName, String value) {
                if (propertyName.equals(ClusterComponentConstants.KEY_IS_SCRIPT_PROVIDED_WITHIN_INPUT_DIR)) {
                    isScriptProvidedbutton.setSelection(Boolean.valueOf(value));
                    enableButtonAndScriptArea(!Boolean.valueOf(value));
                }
            }
        };
    }
    
    @Override
    protected Synchronizer createSynchronizer() {
        return new DefaultSynchronizer() {
            
            @Override
            public void handlePropertyChange(String propertyName, String newValue, String oldValue) {
                super.handlePropertyChange(propertyName, newValue, oldValue);
                if (propertyName.equals(ClusterComponentConstants.KEY_IS_SCRIPT_PROVIDED_WITHIN_INPUT_DIR)) {
                    enableButtonAndScriptArea(!Boolean.valueOf(newValue));
                }
                
            }
        };
    }
  
    private void enableButtonAndScriptArea(boolean enabled) {
        openInEditorButton.setEnabled(enabled);
        newScriptArea.setEnabled(enabled);
    }
    
}

