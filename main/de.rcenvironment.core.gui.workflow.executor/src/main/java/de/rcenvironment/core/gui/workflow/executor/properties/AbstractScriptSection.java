/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.executor.properties;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Abstract component for the ScriptSection.
 * 
 * @author Sascha Zur
 */
public abstract class AbstractScriptSection extends ValidatingWorkflowNodePropertySection {

    /** Load script from local file system should be supported. */
    public static final int LOCAL_FILE = 1;

    /** Use script from built-in editor should be supported. */
    public static final int NEW_SCRIPT_FILE = 4;

    /** Both supported. */
    public static final int ALL = LOCAL_FILE | NEW_SCRIPT_FILE;

    /** None supported. */
    public static final int NO_SCRIPT_FILENAME = 8;

    private static final int MINIMUM_HEIGHT_OF_JOB_SCRIPTING_TEXT = 500;

    protected Button openInEditorButton;

    protected Composite newScriptArea;

    protected EditScriptRunnable esr = null;

    private StyledText scriptingText;

    private final String scriptName;

    public AbstractScriptSection(int style, String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Template method to allow subclasses to add content at the very top.
     */
    protected void createCompositeContentAtVeryTop(Composite composite, TabbedPropertySheetWidgetFactory factory) {

    }

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {

        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();

        Section jobSection = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        jobSection.setText(Messages.configureScript);

        Composite jobParent = factory.createFlatFormComposite(jobSection);

        createCompositeContentAtVeryTop(jobParent, factory);

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        jobParent.setLayout(layout);

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        openInEditorButton = factory.createButton(jobParent, Messages.openInEditor, SWT.PUSH);
        openInEditorButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                esr = new EditScriptRunnable(node);
                esr.run();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        newScriptArea = factory.createFlatFormComposite(jobParent);
        newScriptArea.setLayoutData(gridData);

        layout = new GridLayout();
        layout.numColumns = 2;
        newScriptArea.setLayout(layout);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = GridData.FILL;

        final int aKeyCode = 97;

        scriptingText = new StyledText(newScriptArea, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
        scriptingText.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        scriptingText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == aKeyCode) {
                    scriptingText.selectAll();
                }
                updateEditor(node);
            }

        });

        scriptingText.setLayoutData(gridData);
        scriptingText.setData(CONTROL_PROPERTY_KEY, SshExecutorConstants.CONFIG_KEY_SCRIPT);
        ((GridData) newScriptArea.getLayoutData()).heightHint = MINIMUM_HEIGHT_OF_JOB_SCRIPTING_TEXT;

        addResizingListenerForJobScriptingText(parent.getParent());

        jobSection.setClient(jobParent);
    }

    private void updateEditor(WorkflowNode node) {
        if (esr != null && scriptingText != null && esr.getNode().equals(node)) {
            esr.update(scriptingText.getText());
        }
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        refresh();
    }

    private void addResizingListenerForJobScriptingText(final Composite parent) {

        parent.addListener(SWT.Resize, new Listener() {

            @Override
            public void handleEvent(Event e) {
                setSizeOfJobScriptingText(parent);
            }
        });
    }

    private void setSizeOfJobScriptingText(Composite parent) {
        final int topMargin = 125;
        if (parent.getSize().y < MINIMUM_HEIGHT_OF_JOB_SCRIPTING_TEXT) {
            ((GridData) newScriptArea.getLayoutData()).heightHint = MINIMUM_HEIGHT_OF_JOB_SCRIPTING_TEXT;
        } else {
            ((GridData) newScriptArea.getLayoutData()).heightHint = parent.getSize().y - topMargin;
            newScriptArea.update();
        }
    }

    /**
     * Implementation of {@link AbstractEditScriptRunnable}.
     * 
     * @author Doreen Seider
     */
    private class EditScriptRunnable extends AbstractEditScriptRunnable {

        private final WorkflowNode node;

        public EditScriptRunnable(WorkflowNode node) {
            this.node = node;
        }

        public WorkflowNode getNode() {
            return node;
        }

        @Override
        protected void setScript(String script) {
            node.getComponentDescription().getConfigurationDescription()
                .setConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, script);
        }

        @Override
        protected String getScript() {
            return node.getComponentDescription().getConfigurationDescription()
                .getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT);
        }

        @Override
        protected String getScriptName() {
            return scriptName;
        }
    }

}
