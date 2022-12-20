/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.executor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeUtil;
import de.rcenvironment.core.gui.utils.common.widgets.LineNumberStyledText;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * Abstract component for the ScriptSection.
 * 
 * @author Sascha Zur
 * @author Hendrik Abbenhaus
 */
public abstract class AbstractScriptSection extends ValidatingWorkflowNodePropertySection {

    /** Check WhiteSpaceCharacter key. */
    public static final String CHECKBOX_KEY = "checkShowWhitespace";

    private static final String KEY_SCRIPT_WHITESPACE_BOX = "ScriptWhitespaceBox";

    protected Button openInEditorButton;

    protected Button checkBoxWhitespace;

    protected WhitespaceShowListener whitespaceListener;

    protected EditScriptRunnable esr = null;

    protected LineNumberStyledText scriptingText;

    private final String scriptName;

    public AbstractScriptSection(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Template method to allow subclasses to add content at the very top.
     */
    protected void createCompositeContentAtVeryTop(Composite composite, TabbedPropertySheetWidgetFactory factory) {

    }

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        parent.setLayout(new GridLayout(1, true));

        final Composite composite = getWidgetFactory().createComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        composite.setLayout(new GridLayout(1, true));

        final Section scriptSection = getWidgetFactory().createSection(composite, Section.TITLE_BAR);
        scriptSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        scriptSection.setText(Messages.configureScript);

        Composite scriptComposite = getWidgetFactory().createComposite(composite);
        scriptComposite.setLayout(new GridLayout(1, false));
        scriptComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        createCompositeContentAtVeryTop(scriptComposite, getWidgetFactory());

        openInEditorButton = getWidgetFactory().createButton(scriptComposite, Messages.openInEditor, SWT.PUSH);

        // We intentionally do not use the getWidgetFactory().createButton() in the following. In addition we split the Button and the Label
        // instead of setting the Button's Text variable. The reason is GUI issues regarding the visibility of check marks on different
        // (Linux) platforms with different desktop variants. (see #17878)
        // Kathrin Schaffert, 02.03.2022
        final Composite checkBoxComposite = getWidgetFactory().createComposite(scriptComposite);
        checkBoxComposite.setLayout(new RowLayout());
        checkBoxWhitespace = new Button(checkBoxComposite, SWT.CHECK);
        Label labelWhitespace = new Label(checkBoxComposite, SWT.NONE);
        labelWhitespace.setText(Messages.showWhitespace);
        labelWhitespace.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

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

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        gridData.widthHint = 1;
        gridData.heightHint = 1;
        scriptingText = new LineNumberStyledText(scriptComposite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);

        scriptingText.setLayoutData(gridData);
        scriptingText.setData(CONTROL_PROPERTY_KEY, SshExecutorConstants.CONFIG_KEY_SCRIPT);
        scriptingText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                updateEditor(node);
            }

        });
        whitespaceListener = new WhitespaceShowListener(scriptingText);
        scriptingText.addPaintListener(whitespaceListener);

        checkBoxWhitespace.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selected = checkBoxWhitespace.getSelection();
                if (selected) {
                    whitespaceListener.setEnabled(true);
                    whitespaceListener.drawStyledText();
                    updateEditor(node);

                } else {
                    whitespaceListener.setEnabled(false);
                    whitespaceListener.redrawAll();
                }
                setProperty(KEY_SCRIPT_WHITESPACE_BOX, WorkflowNodeUtil.getConfigurationValue(node, KEY_SCRIPT_WHITESPACE_BOX));
            }
        });
        checkBoxWhitespace.setData(CONTROL_PROPERTY_KEY, KEY_SCRIPT_WHITESPACE_BOX);
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

    /**
     * Implementation of {@link AbstractEditScriptRunnable}.
     * 
     * @author Doreen Seider
     * @author Sascha Zur
     */
    private class EditScriptRunnable extends AbstractEditScriptRunnable {

        private final WorkflowNode node;

        EditScriptRunnable(WorkflowNode node) {
            this.node = node;
        }

        public WorkflowNode getNode() {
            return node;
        }

        @Override
        protected void setScript(String script) {
            setScriptProperty(node, script);
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

    /**
     * If the script is edited in an open editor, the workflow editor must get dirty when the script is saved. To do so, a command must be
     * executed, but it must contain the correct node.
     * 
     * @param node to execute the save command to.
     * @param newValue of the script.
     * @author Sascha Zur
     */
    private void setScriptProperty(WorkflowNode node, final String newValue) {
        final String oldValue = WorkflowNodeUtil.getConfigurationValue(node, SshExecutorConstants.CONFIG_KEY_SCRIPT);
        if ((oldValue != null && !oldValue.equals(newValue))
            || (oldValue == null && oldValue != newValue)) {
            final WorkflowNodeCommand command =
                new SetConfigurationValueCommand(SshExecutorConstants.CONFIG_KEY_SCRIPT, oldValue, newValue);
            execute(node, command);
        }
    }

    @Override
    protected Updater createUpdater() {
        return new DefaultUpdater() {

            @Override
            public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
                super.updateControl(control, propertyName, newValue, oldValue);
                if (propertyName.equals(KEY_SCRIPT_WHITESPACE_BOX)) {
                    checkBoxWhitespace.getSelection();
                    if (!checkBoxWhitespace.getSelection()) {
                        checkBoxWhitespace.setSelection(false);
                        whitespaceListener.setEnabled(false);
                        whitespaceListener.redrawAll();
                    } else {
                        checkBoxWhitespace.setSelection(true);
                        whitespaceListener.setEnabled(true);
                        whitespaceListener.drawStyledText();
                        updateEditor(node);
                    }
                }
            }
        };
    }
}
