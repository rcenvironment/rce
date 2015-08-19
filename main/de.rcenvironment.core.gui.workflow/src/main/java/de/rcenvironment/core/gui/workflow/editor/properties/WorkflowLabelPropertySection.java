/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;

/**
 * Property Section for all WorkflowLabel.
 * 
 * @author Sascha Zur
 */
public class WorkflowLabelPropertySection extends AbstractPropertySection {

    private static final int COLORSELECTOROFFSET_X = 50;   
    
    private static final int COLORSELECTOROFFSET_Y = 200;   
    
    private static final int ALPHA_TEXT_WIDTH = 20;

    private static final String ALPHA_VALUE_TEXT = " Transparency:";

    private static final String BACKGROUND_COLOR_TEXT = " Background color:";

    private static final String TABS = "     ";

    private static final String TEXT_COLOR = "Text color:";

    private static final int TEXTFIELD_HEIGHT = 20 * 15;

    private static final int MAX_255 = 255;

    private static final int MAX_100 = 100;

    private static final double SCALE_TO_PERCENT_FACTOR = 2.55;

    private WorkflowLabel label;

    private Text textfield;

    private Label textColorPreviewLabel;

    private Label backgroundColorPreviewLabel;

    private Scale alphaScale;

    private Text alphaValueText;

    @Override
    public void setInput(final IWorkbenchPart part, final ISelection selection) {
        final Object firstSelectionElement = ((IStructuredSelection) selection).getFirstElement();
        final WorkflowLabelPart workflowLabelPart = (WorkflowLabelPart) firstSelectionElement;
        final WorkflowLabel workflowLabel = (WorkflowLabel) workflowLabelPart.getModel();
        if (getPart() == null || !getPart().equals(part)
            || label == null || !label.equals(workflowLabel)) {
            super.setInput(part, selection);
            setWorkflowLabelBase(workflowLabel);
        }
    }

    private void setWorkflowLabelBase(final WorkflowLabel workflowLabel) {
        this.label = workflowLabel;
    }

    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final Section propertiesSection =
            PropertyTabGuiHelper.createSingleColumnSectionComposite(parent, aTabbedPropertySheetPage.getWidgetFactory(),
                "Settings");

        Composite content = aTabbedPropertySheetPage.getWidgetFactory().createFlatFormComposite(propertiesSection);
        content.setLayout(new GridLayout(1, false));
        content.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | SWT.BORDER));
        propertiesSection.setClient(content);

        textfield = new Text(content, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        GridData textData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        textData.heightHint = TEXTFIELD_HEIGHT;
        textfield.setLayoutData(textData);
        textfield.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                label.setText(textfield.getText());
                label.firePropertyChange(WorkflowLabel.PROPERTY_CHANGE);
            }
        });

        Composite colorComposite = new Composite(content, SWT.NONE);
        colorComposite.setLayout(new GridLayout(6, false));
        colorComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridData g2 = new GridData();
        g2.horizontalSpan = 1;
        colorComposite.setLayoutData(g2);

        Label textColorLabel = new Label(colorComposite, SWT.NONE);
        textColorLabel.setText(TEXT_COLOR);
        textColorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        textColorPreviewLabel = new Label(colorComposite, SWT.BORDER);
        textColorPreviewLabel.setText(TABS);
        textColorPreviewLabel.addMouseListener(new MouseListener() {

            
            @Override
            public void mouseUp(MouseEvent event) {
                // create own shell to place the colorselector where desired
                int displayWidth = event.display.getBounds().width;
                int displayHeight = event.display.getBounds().height;
                Shell colorSelectorShell = new Shell(event.display);
                colorSelectorShell.setLocation(displayWidth/2 - COLORSELECTOROFFSET_X, displayHeight/2 - COLORSELECTOROFFSET_Y);
                ColorSelector cs = new ColorSelector(colorSelectorShell);
                cs.setColorValue(new RGB(label.getColorText()[0], label.getColorText()[1], label.getColorText()[2]));
                cs.open();
                label.setColorText(new int[] { cs.getColorValue().red, cs.getColorValue().green, cs.getColorValue().blue });
                label.firePropertyChange(WorkflowLabel.PROPERTY_CHANGE);
                colorSelectorShell.dispose();
                refresh();
            }

            @Override
            public void mouseDown(MouseEvent arg0) {

            }

            @Override
            public void mouseDoubleClick(MouseEvent arg0) {

            }
        });

        Label backgroundColorLabel = new Label(colorComposite, SWT.NONE);
        backgroundColorLabel.setText(BACKGROUND_COLOR_TEXT);
        backgroundColorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        backgroundColorPreviewLabel = new Label(colorComposite, SWT.BORDER);
        backgroundColorPreviewLabel.setText(TABS);

        backgroundColorPreviewLabel.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent event) {// create own shell to place the colorselector where desired
                int displayWidth = event.display.getBounds().width;
                int displayHeight = event.display.getBounds().height;
                Shell colorSelectorShell = new Shell(event.display);
                colorSelectorShell.setLocation(displayWidth/2 - COLORSELECTOROFFSET_X, displayHeight/2 - COLORSELECTOROFFSET_Y);
                ColorSelector cs = new ColorSelector(colorSelectorShell);
                cs.setColorValue(new RGB(label.getColorBackground()[0], label.getColorBackground()[1], label
                    .getColorBackground()[2]));
                cs.open();
                label.setColorBackground(new int[] { cs.getColorValue().red, cs.getColorValue().green, cs.getColorValue().blue });
                label.firePropertyChange(WorkflowLabel.PROPERTY_CHANGE);
                colorSelectorShell.dispose();
                refresh();
            }

            @Override
            public void mouseDown(MouseEvent arg0) {}

            @Override
            public void mouseDoubleClick(MouseEvent arg0) {}
        });

        Label alphaLabel = new Label(colorComposite, SWT.NONE);
        alphaLabel.setText(ALPHA_VALUE_TEXT);
        alphaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Composite alphaComposite = new Composite(colorComposite, SWT.NONE);
        alphaComposite.setLayout(new GridLayout(2, false));
        alphaComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        alphaScale = new Scale(alphaComposite, SWT.HORIZONTAL);
        alphaScale.setMaximum(MAX_255);
        alphaScale.setMinimum(0);
        alphaScale.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        alphaScale.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                label.setAlpha(alphaScale.getSelection());
                label.firePropertyChange(WorkflowLabel.PROPERTY_CHANGE);
                refresh();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        alphaValueText = new Text(alphaComposite, SWT.NONE);
        alphaValueText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        alphaValueText.addVerifyListener(new NumericalTextConstraintListener(alphaValueText, NumericalTextConstraintListener.ONLY_INTEGER));
        alphaValueText.addModifyListener(new AlphaValueListener());
        GridData alphaData = new GridData();
        alphaData.widthHint = ALPHA_TEXT_WIDTH;
        alphaValueText.setLayoutData(alphaData);

    }

    @Override
    public void refresh() {
        super.refresh();
        if (textfield != null) {
            textfield.setText(label.getText());
        }
        if (textColorPreviewLabel != null) {
            textColorPreviewLabel.setBackground(new Color(null, label.getColorText()[0], label.getColorText()[1], label.getColorText()[2]));
        }
        if (backgroundColorPreviewLabel != null) {
            backgroundColorPreviewLabel.setBackground(new Color(null, label.getColorBackground()[0], label.getColorBackground()[1], label
                .getColorBackground()[2]));
        }
        if (alphaScale != null) {
            alphaScale.setSelection(label.getAlphaDisplay());
        }
        if (alphaValueText != null) {
            alphaValueText.setText("" + ((int) Math.ceil((label.getAlphaDisplay() / SCALE_TO_PERCENT_FACTOR))));
        }

    }

    /**
     * Listener for the alpha value text field.
     * 
     * @author Sascha Zur
     */
    private class AlphaValueListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent arg0) {
            String text = ((Text) arg0.getSource()).getText();
            try {
                int value = Integer.parseInt(text);
                if (value > MAX_100) {
                    ((Text) arg0.getSource()).setText("" + MAX_100);
                } else if (value < 0) {
                    ((Text) arg0.getSource()).setText("0");
                } else {
                    alphaScale.setSelection(((int) (SCALE_TO_PERCENT_FACTOR * value)));

                }
                label.setAlpha(alphaScale.getSelection());
                label.firePropertyChange(WorkflowLabel.PROPERTY_CHANGE);
            } catch (NumberFormatException e) {
                text = ""; // Should never happen
            }
        }

    }
}
