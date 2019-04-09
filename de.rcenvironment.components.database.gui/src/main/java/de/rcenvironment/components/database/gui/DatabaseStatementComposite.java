/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Database statement composite.
 *
 * @author Oliver Seebach
 */
public class DatabaseStatementComposite extends Composite {

    private static final int KEY_CODE_A = 97;
    
    private static final int OUTPUT_COMBO_MINIMUM_WIDTH = 150;

    private static final int STATEMENT_TEXT_MINIMUM_HEIGHT = 100;

    private CCombo outputCombo;

    private StyledText statementText;

    private Text statementNameText;

    private Button writeToOutputCheckButton;

    public DatabaseStatementComposite(Composite parent, int style) {
        super(parent, style);
    }

    /**
     * Creates the GUI.
     * 
     */
    public void createControls() {
        GridData layoutData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        setLayout(new GridLayout(1, false));
        setLayoutData(layoutData);

        Composite statementNameComposite = new Composite(this, SWT.NONE);
        statementNameComposite.setLayout(new GridLayout(2, false));
        GridData statementNameData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        statementNameComposite.setLayoutData(statementNameData);

        Label statementNameLabel = new Label(statementNameComposite, SWT.NONE);
        statementNameLabel.setText("Statement Name:");

        statementNameText = new Text(statementNameComposite, SWT.BORDER);
        GridData statementNameTextData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        statementNameText.setLayoutData(statementNameTextData);
//        statementNameText.setData(WorkflowNodePropertySection.CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DB_STATEMENTS_KEY);

        Composite statementComposite = new Composite(this, SWT.NONE);
        statementComposite.setLayout(new GridLayout(1, false));
        GridData statementData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        statementComposite.setLayoutData(statementData);

        Label statementTextLabel = new Label(statementComposite, SWT.NONE);
        statementTextLabel.setText("Database Statement:");

        statementText = new StyledText(statementComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        GridData statementTextData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        statementTextData.minimumHeight = STATEMENT_TEXT_MINIMUM_HEIGHT;
        statementText.setLayoutData(statementTextData);
//        statementText.setData(WorkflowNodePropertySection.CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DB_STATEMENTS_KEY);
        statementText.setIndent(3);
        statementText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.CTRL && e.keyCode == KEY_CODE_A) {
                    statementText.selectAll();
                }
            }

        });
        
        Group writeToOutputGroup = new Group(this, SWT.NONE);
        writeToOutputGroup.setText("Result to Output:");
        writeToOutputGroup.setLayout(new GridLayout(2, false));
        GridData writeToOutputGroupData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        writeToOutputGroup.setLayoutData(writeToOutputGroupData);

        writeToOutputCheckButton = new Button(writeToOutputGroup, SWT.CHECK);
        writeToOutputCheckButton.setText("Write result to output:");
//        writeToOutputCheckButton.setData(WorkflowNodePropertySection.CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DB_STATEMENTS_KEY);

        outputCombo = new CCombo(writeToOutputGroup, SWT.READ_ONLY | SWT.BORDER);
        outputCombo.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridData outputComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        outputComboData.minimumWidth = OUTPUT_COMBO_MINIMUM_WIDTH;
        outputCombo.setLayoutData(outputComboData);
//        outputCombo.setData(WorkflowNodePropertySection.CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DB_STATEMENTS_KEY);
    }

    public StyledText getStatementText() {
        return statementText;
    }

    public Text getStatementNameText() {
        return statementNameText;
    }

    public Button getWriteToOutputCheckButton() {
        return writeToOutputCheckButton;
    }

    public CCombo getOutputCombo() {
        return outputCombo;
    }

    /**
     * Fills output combo and sets selection.
     * 
     * @param items The items to set.
     * @param selection The selection to set.
     */
    public void fillOutputComboAndSetSelection(String[] items, String selection) {
        fillOutputCombo(items);
        outputCombo.select(outputCombo.indexOf(selection));
    };

    /**
     * Fills output combo and sets selection.
     * 
     * @param items The items to set.
     */
    public void fillOutputCombo(String[] items) {
        outputCombo.setItems(items);
    };

}
