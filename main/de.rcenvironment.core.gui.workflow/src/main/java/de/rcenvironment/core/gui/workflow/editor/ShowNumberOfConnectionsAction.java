/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.RetargetAction;

import de.rcenvironment.core.gui.workflow.Activator;

/**
 * Action that shows or hides all numbers of channels per connection in the workflow editor.
 * 
 * @author Oliver Seebach
 *
 */
public class ShowNumberOfConnectionsAction extends RetargetAction {

    private static IPreferenceStore preferenceStore;
    
    private IWorkbenchPart activePart;
    
    public ShowNumberOfConnectionsAction(String actionID, String text) {
        super(actionID, text);
        preferenceStore = Activator.getInstance().getPreferenceStore();
    }

    @Override
    public void partActivated(IWorkbenchPart part) {
        super.partActivated(part);
        activePart = part;
        // set button state accordingly
        setChecked(preferenceStore.getBoolean(WorkflowEditor.SHOW_LABELS_PREFERENCE_KEY));
        if (activePart instanceof WorkflowEditor) {
            if (preferenceStore.getBoolean(WorkflowEditor.SHOW_LABELS_PREFERENCE_KEY)){
                ((WorkflowEditor) activePart).showAllConnectionLabels();
            } else {
                ((WorkflowEditor) activePart).hideUnselectedConnectionLabels();
            }
        }
        isEnabled();
    }

    @Override
    public boolean isEnabled() {
        boolean enabled = (activePart instanceof WorkflowEditor);
        super.setEnabled(enabled);
        return enabled;
    }
      
    @Override
    public int getStyle() {
        return IAction.AS_CHECK_BOX;
    }
    
    @Override
    public void runWithEvent(Event event) {
        if (activePart instanceof WorkflowEditor) {
            WorkflowEditor editor = (WorkflowEditor) activePart;
            if (isChecked()){
                editor.showAllConnectionLabels();
                preferenceStore.setValue(WorkflowEditor.SHOW_LABELS_PREFERENCE_KEY, true);
            } else {
                editor.hideUnselectedConnectionLabels();
                preferenceStore.setValue(WorkflowEditor.SHOW_LABELS_PREFERENCE_KEY, false);
            }
        }
        super.runWithEvent(event);
    }
}
