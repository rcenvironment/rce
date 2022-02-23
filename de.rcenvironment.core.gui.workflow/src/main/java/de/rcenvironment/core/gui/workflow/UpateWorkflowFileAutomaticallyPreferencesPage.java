/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * Preferences page for pref: update workflow file automatically.
 *
 * @author Doreen Seider
 */
public class UpateWorkflowFileAutomaticallyPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public UpateWorkflowFileAutomaticallyPreferencesPage() {
        super(GRID);
    }
    
    @Override
    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(GUIWorkflowDescriptionLoaderCallback.PREFS_KEY_UPDATEAUTOMATICALLY,
            Messages.updateIncompatibleVersionSilently, getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getInstance().getPreferenceStore());
        
    }

}
