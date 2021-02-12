/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel.LabelPosition;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel.TextAlignmentType;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.utils.common.OSFamily;

/**
 * Property Section for all WorkflowLabel.
 * 
 * @author Sascha Zur
 * @author Marc Stammerjohann
 * @author Doreen Seider
 * @author Hendrik Abbenhaus
 */
public class WorkflowLabelPropertySection extends WorkflowPropertySection implements WorkflowLabelCommand.Executor {

    private static final int HORIZONTAL_SPACING = 20;

    private static final int COLORSELECTOROFFSET_X = 50;

    private static final int COLORSELECTOROFFSET_Y = 200;

    private static final int ALPHA_TEXT_WIDTH = 25;

    private static final String TABS = "     ";

    private static final int MAX_255 = 255;

    private static final int MAX_100 = 100;

    private static final double SCALE_TO_PERCENT_FACTOR = 2.55;

    private static final String COLOR = "Color:";

    private WorkflowLabel label;

    private StyledText headerTextField;

    private StyledText textfield;

    private Button[] labelPositionButtons;

    private Button[] headerAlignmentButtons;

    private Button[] textAlignmentButtons;

    private LabelPosition[] values;

    private Label headerColorPreviewLabel;

    private Label textColorPreviewLabel;

    private Spinner headerSizeSpinner;

    private Spinner textSizeSpinner;

    private Label bgColorPreviewLabel;

    private Scale bgAlphaScale;

    private Text bgAlphaValueText;

    private Button bgBorderButton;

    private EditValueCommand editTextCommand;

    private EditValueCommand editTransparencyCommand;

    private final Updater updater = createUpdater();

    private final Synchronizer synchronizer = createSynchronizer();

    private final SynchronizerAdapter synchronizerAdapter = new SynchronizerAdapter();

    @Override
    public void setInput(final IWorkbenchPart part, final ISelection selection) {
        final Object firstSelectionElement = ((IStructuredSelection) selection).getFirstElement();
        final WorkflowLabelPart workflowLabelPart = (WorkflowLabelPart) firstSelectionElement;
        final WorkflowLabel workflowLabel = (WorkflowLabel) workflowLabelPart.getModel();
        if (getPart() == null || !getPart().equals(part)
            || label == null || !label.equals(workflowLabel)) {
            super.setInput(part, selection);
            setWorkflowLabelBase(workflowLabel);
            updateLabelPositionSelection(label.getLabelPosition());
            updateAlignmentButtonsSelection(headerAlignmentButtons, label.getHeaderAlignmentType());
            updateAlignmentButtonsSelection(textAlignmentButtons, label.getTextAlignmentType());
            if (headerTextField != null && !headerTextField.isDisposed()) {
                headerTextField.setText(workflowLabel.getHeaderText());
                headerTextField.setSelection(headerTextField.getText().length());
                headerColorPreviewLabel.setBackground(createColor(label.getColorHeader()));
                headerSizeSpinner.setSelection(label.getHeaderTextSize());
                headerAlignmentButtons[label.getHeaderAlignmentType().ordinal()].setSelection(true);

            }
            if (textfield != null && !textfield.isDisposed()) {
                textfield.setText(workflowLabel.getText());
                textColorPreviewLabel.setBackground(createColor(label.getColorText()));
                textSizeSpinner.setSelection(label.getTextSize());
                textAlignmentButtons[label.getTextAlignmentType().ordinal()].setSelection(true);
                bgColorPreviewLabel.setBackground(createColor(label.getColorBackground()));
                bgAlphaScale.setSelection(label.getAlphaDisplay());
                bgAlphaValueText.setText("" + ((int) Math.ceil((label.getAlphaDisplay() / SCALE_TO_PERCENT_FACTOR))));
                bgBorderButton.setSelection(label.hasBorder());
            }

        }
    }

    private static Color createColor(int[] color) {
        return new Color(Display.getCurrent(), color[0], color[1], color[2]);
    }

    private void setWorkflowLabelBase(final WorkflowLabel workflowLabel) {
        this.label = workflowLabel;
        initializeModelBindingBase();
    }

    protected final Updater getUpdater() {
        return updater;
    }

    protected Updater createUpdater() {
        return new DefaultUpdater();
    }

    protected Synchronizer createSynchronizer() {
        return new DefaultSynchronizer();
    }

    @Override
    public void dispose() {
        tearDownModelBindingBase();
        super.dispose();
    }

    private void initializeModelBindingBase() {
        label.addPropertyChangeListener(synchronizerAdapter);
    }

    private void tearDownModelBindingBase() {
        label.removePropertyChangeListener(synchronizerAdapter);
    }

    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        GridData mGridData = new GridData(GridData.FILL_BOTH);
        mGridData.grabExcessHorizontalSpace = true;
        mGridData.grabExcessVerticalSpace = true;
        mGridData.horizontalAlignment = SWT.FILL;
        parent.setLayoutData(mGridData);

        parent.setLayout(new GridLayout(2, false));

        createHeaderSection(aTabbedPropertySheetPage, parent);
        createBackgroundSection(aTabbedPropertySheetPage, parent);
        Composite composite = new Composite(parent, SWT.NONE);
        GridData cData = new GridData();
        cData.horizontalSpan = 2;
        cData.grabExcessHorizontalSpace = false;
        cData.grabExcessVerticalSpace = true;
        cData.horizontalAlignment = SWT.FILL;
        cData.verticalAlignment = SWT.FILL;
        composite.setLayoutData(cData);
        GridLayout mainLayout = new GridLayout(4, false);
        mainLayout.marginTop = 0;
        mainLayout.marginLeft = 0;
        mainLayout.marginWidth = 0;
        mainLayout.marginHeight = 0;
        composite.setLayout(mainLayout);

        createTextSection(aTabbedPropertySheetPage, composite);
        createPositionSection(aTabbedPropertySheetPage, composite);

    }

    private void createHeaderSection(TabbedPropertySheetPage tabbedPropertySheetPage, Composite parent) {
        final Section headerPropertiesSection = tabbedPropertySheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        GridData hGridData = new GridData(GridData.FILL_BOTH);
        hGridData.horizontalSpan = 1;
        hGridData.grabExcessHorizontalSpace = true;
        hGridData.grabExcessVerticalSpace = false;
        // hGridData
        headerPropertiesSection.setLayoutData(hGridData);
        headerPropertiesSection.setLayout(new GridLayout(1, false));
        headerPropertiesSection.setText("Header");

        Composite headerPropertiesComposite = tabbedPropertySheetPage.getWidgetFactory().createComposite(headerPropertiesSection);
        GridData hPGridData = new GridData(GridData.FILL | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        hPGridData.horizontalSpan = 2;
        headerPropertiesComposite.setLayout(new GridLayout(4, false));
        headerPropertiesComposite.setLayoutData(hPGridData);
        headerPropertiesSection.setClient(headerPropertiesComposite);

        headerTextField = new StyledText(headerPropertiesComposite, SWT.SINGLE | SWT.BORDER);
        GridData headerTextData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL | GridData.FILL_HORIZONTAL);
        headerTextField.setLayoutData(headerTextData);
        headerTextField.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                String oldValue = label.getHeaderText();
                String newValue = headerTextField.getText();
                if (!newValue.equals(oldValue)) {
                    int position = headerTextField.getCaretOffset();
                    if (editTextCommand == null) {
                        editTextCommand =
                            editProperty(new Value(Value.ValueType.HEADER_TEXT, oldValue),
                                new Value(Value.ValueType.HEADER_TEXT, newValue));
                    } else {
                        editTextCommand.setNewValue(new Value(Value.ValueType.HEADER_TEXT, newValue));
                    }

                    headerTextField.setCaretOffset(position);
                }
            }
        });
        headerTextField.addKeyListener(new TextKeyListener());
        headerTextField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent arg0) {}

            @Override
            public void focusLost(FocusEvent arg0) {
                editTextCommand = null;
                headerTextField.setSelection(0);
            }

        });

        Composite textColorSizePropertiesComposite = new Composite(headerPropertiesComposite, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = HORIZONTAL_SPACING;
        textColorSizePropertiesComposite.setLayout(gridLayout);
        textColorSizePropertiesComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridData g2 = new GridData();
        g2.horizontalSpan = 1;
        textColorSizePropertiesComposite.setLayoutData(g2);

        Composite textColorComposite = new Composite(textColorSizePropertiesComposite, SWT.NONE);
        textColorComposite.setLayout(new GridLayout(2, false));
        textColorComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Label textColorLabel = new Label(textColorComposite, SWT.NONE);
        textColorLabel.setText(COLOR);
        textColorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        headerColorPreviewLabel = new Label(textColorComposite, SWT.BORDER);
        headerColorPreviewLabel.setText(TABS);
        headerColorPreviewLabel.addMouseListener(new ColorMouseListener(Value.ValueType.COLOR_HEADER));

        Composite textSizeComposite = new Composite(textColorSizePropertiesComposite, SWT.NONE);
        textSizeComposite.setLayout(new GridLayout(2, false));
        textSizeComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Label textSizeLabel = new Label(textSizeComposite, SWT.NONE);
        textSizeLabel.setText("Size:");
        textSizeLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        headerSizeSpinner = new Spinner(textSizeComposite, SWT.BORDER);
        headerSizeSpinner.setMinimum(1);
        final int maxFontSize = 99;
        headerSizeSpinner.setMaximum(maxFontSize);
        headerSizeSpinner.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                int oldValue = label.getHeaderTextSize();
                int newValue = headerSizeSpinner.getSelection();
                if (newValue != oldValue) {
                    editProperty(new Value(Value.ValueType.HEADER_SIZE, oldValue), new Value(Value.ValueType.HEADER_SIZE, newValue));
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        headerAlignmentButtons = new Button[3];
        addHeaderAlignmentGroup(headerPropertiesComposite);

    }

    private void createTextSection(TabbedPropertySheetPage tabbedPropertySheetPage, Composite parent) {
        final Section textPropertiesSection = tabbedPropertySheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);

        GridData tGridData = new GridData(GridData.FILL_BOTH);
        tGridData.horizontalSpan = 3;

        tGridData.grabExcessHorizontalSpace = true;
        tGridData.grabExcessVerticalSpace = true;
        textPropertiesSection.setLayoutData(tGridData);
        textPropertiesSection.setLayout(new GridLayout(1, false));

        textPropertiesSection.setText("Text");

        Composite textPropertiesComposite = tabbedPropertySheetPage.getWidgetFactory().createComposite(textPropertiesSection, SWT.NONE);
        GridData tPGridData = new GridData(GridData.FILL_BOTH);
        tPGridData.horizontalSpan = 1;
        tPGridData.grabExcessHorizontalSpace = true;
        tPGridData.grabExcessVerticalSpace = true;
        tPGridData.horizontalAlignment = GridData.FILL;
        tPGridData.verticalAlignment = GridData.FILL;
        textPropertiesComposite.setLayoutData(tPGridData);
        textPropertiesComposite.setLayout(new GridLayout(3, false));
        textPropertiesSection.setClient(textPropertiesComposite);

        textfield = new StyledText(textPropertiesComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData textData = new GridData();
        textData.grabExcessHorizontalSpace = true;
        textData.grabExcessVerticalSpace = true;
        textData.horizontalAlignment = GridData.FILL;
        textData.verticalAlignment = GridData.FILL;
        textData.minimumWidth = 1;
        textData.widthHint = 1;
        textData.heightHint = 1;
        textData.minimumHeight = 1;
        textfield.setLayoutData(textData);

        textfield.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                String oldValue = label.getText();
                String newValue = textfield.getText();
                if (!newValue.equals(oldValue)) {
                    int position = textfield.getCaretOffset();
                    if (editTextCommand == null) {
                        editTextCommand =
                            editProperty(new Value(Value.ValueType.TEXT, oldValue), new Value(Value.ValueType.TEXT, newValue));
                    } else {
                        editTextCommand.setNewValue(new Value(Value.ValueType.TEXT, newValue));
                    }
                    textfield.setCaretOffset(position);
                }
            }
        });
        textfield.addKeyListener(new TextKeyListener());
        textfield.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent arg0) {
                editTextCommand = null;
                textfield.setSelection(0);
            }

            @Override
            public void focusGained(FocusEvent arg0) {}
        });

        Composite textColorSizePropertiesComposite = new Composite(textPropertiesComposite, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = HORIZONTAL_SPACING;
        textColorSizePropertiesComposite.setLayout(gridLayout);
        textColorSizePropertiesComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridData g2 = new GridData();
        g2.horizontalSpan = 1;
        g2.verticalAlignment = GridData.BEGINNING;
        textColorSizePropertiesComposite.setLayoutData(g2);

        Composite textColorComposite = new Composite(textColorSizePropertiesComposite, SWT.NONE);
        textColorComposite.setLayout(new GridLayout(2, false));
        textColorComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Label textColorLabel = new Label(textColorComposite, SWT.NONE);
        textColorLabel.setText(COLOR);
        textColorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        textColorPreviewLabel = new Label(textColorComposite, SWT.BORDER);
        textColorPreviewLabel.setText(TABS);
        textColorPreviewLabel.addMouseListener(new ColorMouseListener(Value.ValueType.COLOR_TEXT));

        Composite textSizeComposite = new Composite(textColorSizePropertiesComposite, SWT.NONE);
        textSizeComposite.setLayout(new GridLayout(2, false));
        textSizeComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Label textSizeLabel = new Label(textSizeComposite, SWT.NONE);
        textSizeLabel.setText("Size:");
        textSizeLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        textSizeSpinner = new Spinner(textSizeComposite, SWT.BORDER);
        textSizeSpinner.setMinimum(1);
        final int maxFontSize = 99;
        textSizeSpinner.setMaximum(maxFontSize);
        textSizeSpinner.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                int oldValue = label.getTextSize();
                int newValue = textSizeSpinner.getSelection();
                if (newValue != oldValue) {
                    editProperty(new Value(Value.ValueType.TEXT_SIZE, oldValue), new Value(Value.ValueType.TEXT_SIZE, newValue));
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        textAlignmentButtons = new Button[3];
        addTextAlignmentGroup(textPropertiesComposite);

    }

    private void createBackgroundSection(TabbedPropertySheetPage tabbedPropertySheetPage, Composite parent) {

        final Section bgPropertiesSection = tabbedPropertySheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        bgPropertiesSection.setText("Background");
        GridData sGridData = new GridData();
        sGridData.horizontalSpan = 1;
        sGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
        sGridData.verticalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
        bgPropertiesSection.setLayoutData(sGridData);

        Composite bgPropertiesComposite = tabbedPropertySheetPage.getWidgetFactory().createComposite(bgPropertiesSection);
        GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.horizontalSpacing = HORIZONTAL_SPACING;
        bgPropertiesComposite.setLayout(gridLayout);
        bgPropertiesComposite.setLayoutData(
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | SWT.BORDER));
        bgPropertiesSection.setClient(bgPropertiesComposite);

        Composite bgColorComposite = new Composite(bgPropertiesComposite, SWT.NONE);
        bgColorComposite.setLayout(new GridLayout(2, false));
        bgColorComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Label bgColorLabel = new Label(bgColorComposite, SWT.NONE);
        bgColorLabel.setText("Color:");
        bgColorLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        bgColorPreviewLabel = new Label(bgColorComposite, SWT.BORDER);
        bgColorPreviewLabel.setText(TABS);
        bgColorPreviewLabel.addMouseListener(new ColorMouseListener(Value.ValueType.COLOR_BACKGROUND));

        Composite bgAlphaComposite = new Composite(bgPropertiesComposite, SWT.NONE);
        bgAlphaComposite.setLayout(new GridLayout(3, false));
        bgAlphaComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Label bgAlphaLabel = new Label(bgAlphaComposite, SWT.NONE);
        bgAlphaLabel.setText("Transparency:");
        bgAlphaLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        bgAlphaScale = new Scale(bgAlphaComposite, SWT.HORIZONTAL);
        bgAlphaScale.setMaximum(MAX_255);
        bgAlphaScale.setMinimum(0);
        bgAlphaScale.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        bgAlphaScale.addSelectionListener(new AplhaSelectionListener());
        bgAlphaScale.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent arg0) {
                editTransparencyCommand = null;
            }

            @Override
            public void focusGained(FocusEvent arg0) {}
        });
        bgAlphaValueText = new Text(bgAlphaComposite, SWT.NONE);
        bgAlphaValueText.setEditable(false);
        bgAlphaValueText.addVerifyListener(new NumericalTextConstraintListener(bgAlphaValueText,
            NumericalTextConstraintListener.ONLY_INTEGER));
        bgAlphaValueText.addModifyListener(new AlphaValueListener());
        GridData alphaData = new GridData();
        alphaData.widthHint = ALPHA_TEXT_WIDTH;
        bgAlphaValueText.setLayoutData(alphaData);
        bgAlphaValueText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        bgBorderButton = new Button(bgPropertiesComposite, SWT.CHECK);
        bgBorderButton.setText("Border");
        bgBorderButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean oldValue = label.hasBorder();
                boolean newValue = bgBorderButton.getSelection();
                if (newValue != oldValue) {
                    editProperty(new Value(Value.ValueType.BORDER, oldValue), new Value(Value.ValueType.BORDER, newValue));
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
    }

    private void createPositionSection(TabbedPropertySheetPage tabbedPropertySheetPage, Composite parent) {
        final Section positionSection = tabbedPropertySheetPage.getWidgetFactory().createSection(parent, Section.TITLE_BAR);
        GridData positionGridData = new GridData(GridData.FILL_VERTICAL);
        positionGridData.horizontalAlignment = GridData.END;
        positionGridData.grabExcessHorizontalSpace = false;
        positionGridData.grabExcessVerticalSpace = true;
        positionSection.setLayoutData(positionGridData);
        positionSection.setLayout(new FillLayout());
        positionSection.setText("Text Position");
        Composite positionPropertiesComposite = tabbedPropertySheetPage.getWidgetFactory().createComposite(positionSection);
        positionPropertiesComposite.setLayout(new GridLayout(1, false));
        positionSection.setClient(positionPropertiesComposite);

        Group positionGroup = new Group(positionPropertiesComposite, SWT.SHADOW_IN);
        GridLayout gridLayout = new GridLayout(3, true);
        gridLayout.horizontalSpacing = 10;
        gridLayout.verticalSpacing = 10;
        positionGroup.setLayout(gridLayout);
        GridData groupGridData = new GridData();
        groupGridData.verticalAlignment = GridData.BEGINNING;
        groupGridData.grabExcessHorizontalSpace = true;
        positionGroup.setLayoutData(groupGridData);
        int[] horizontalAlignment = { GridData.BEGINNING, GridData.CENTER, GridData.END };
        int[] verticalAlignment = horizontalAlignment;
        labelPositionButtons = new Button[9];
        values = LabelPosition.values();
        int buttonIndex = 0;
        for (int j = 0; j < verticalAlignment.length; j++) {
            for (int i = 0; i < horizontalAlignment.length; i++) {
                labelPositionButtons[buttonIndex] = new Button(positionGroup, SWT.RADIO);
                GridData gridData = new GridData(horizontalAlignment[i], verticalAlignment[j], true, true, 1, 1);
                labelPositionButtons[buttonIndex].setLayoutData(gridData);
                labelPositionButtons[buttonIndex].addSelectionListener(new PositionSelectionListener(values[buttonIndex]));
                buttonIndex++;
            }
        }
    }

    private void addHeaderAlignmentGroup(Composite parent) {
        Group alignmentGroup = new Group(parent, SWT.SHADOW_IN);
        alignmentGroup.setText(Messages.textAlignment);
        alignmentGroup.setLayout(new GridLayout(3, true));
        Button leftAlignment = new Button(alignmentGroup, SWT.RADIO | SWT.LEFT);
        leftAlignment.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true, 1, 1));
        leftAlignment.addSelectionListener(new TextAlignmentSelectionListener(Value.ValueType.HEADER_ALIGNMENT, TextAlignmentType.LEFT));
        headerAlignmentButtons[0] = leftAlignment;
        Button centerAlignment = new Button(alignmentGroup, SWT.RADIO | SWT.CENTER);
        centerAlignment.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true, 1, 1));
        centerAlignment.addSelectionListener(
            new TextAlignmentSelectionListener(Value.ValueType.HEADER_ALIGNMENT, TextAlignmentType.CENTER));
        headerAlignmentButtons[1] = centerAlignment;
        Button rightAlignment = new Button(alignmentGroup, SWT.RADIO | SWT.RIGHT);
        rightAlignment.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true, 1, 1));
        rightAlignment.addSelectionListener(new TextAlignmentSelectionListener(Value.ValueType.HEADER_ALIGNMENT, TextAlignmentType.RIGHT));
        headerAlignmentButtons[2] = rightAlignment;
    }

    private void addTextAlignmentGroup(Composite parent) {
        Group alignmentGroup = new Group(parent, SWT.SHADOW_IN);
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.BEGINNING;
        alignmentGroup.setText(Messages.textAlignment);
        alignmentGroup.setLayoutData(gridData);
        alignmentGroup.setLayout(new GridLayout(3, true));
        Button leftAlignment = new Button(alignmentGroup, SWT.RADIO | SWT.LEFT);
        leftAlignment.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true, 1, 1));
        leftAlignment.addSelectionListener(new TextAlignmentSelectionListener(Value.ValueType.TEXT_ALIGNMENT, TextAlignmentType.LEFT));
        textAlignmentButtons[0] = leftAlignment;
        Button centerAlignment = new Button(alignmentGroup, SWT.RADIO | SWT.CENTER);
        centerAlignment.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true, 1, 1));
        centerAlignment.addSelectionListener(new TextAlignmentSelectionListener(Value.ValueType.TEXT_ALIGNMENT, TextAlignmentType.CENTER));
        textAlignmentButtons[1] = centerAlignment;
        Button rightAlignment = new Button(alignmentGroup, SWT.RADIO | SWT.RIGHT);
        rightAlignment.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true, 1, 1));
        rightAlignment.addSelectionListener(new TextAlignmentSelectionListener(Value.ValueType.TEXT_ALIGNMENT, TextAlignmentType.RIGHT));
        textAlignmentButtons[2] = rightAlignment;
    }

    private void updateLabelPositionSelection(LabelPosition labelPosition) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(labelPosition)) {
                labelPositionButtons[i].setSelection(true);
            } else {
                labelPositionButtons[i].setSelection(false);
            }
        }
    }

    private void updateAlignmentButtonsSelection(Button[] buttonList, TextAlignmentType textAlignmentType) {
        if (buttonList == null) {
            return;
        }
        switch (textAlignmentType) {
        case LEFT:
            buttonList[0].setSelection(true);
            buttonList[1].setSelection(false);
            buttonList[2].setSelection(false);
            break;
        case CENTER:
            buttonList[0].setSelection(false);
            buttonList[1].setSelection(true);
            buttonList[2].setSelection(false);
            break;
        case RIGHT:
            buttonList[0].setSelection(false);
            buttonList[1].setSelection(false);
            buttonList[2].setSelection(true);
            break;
        default:
            break;
        }
    }

    protected EditValueCommand editProperty(final Value oldValue, final Value newValue) {
        final EditValueCommand command = new EditValueCommand(oldValue, newValue);
        execute(command);
        return command;
    }

    private void updateContent() {
        Display.getCurrent().asyncExec(new Runnable() {

            @Override
            public void run() {
                if (isAccessible(headerTextField)) {
                    int location = headerTextField.getCaretOffset(); // remember caret location because setting the text resets it
                    headerTextField.setText(label.getHeaderText());
                    headerTextField.setSelection(location);
                }
                if (isAccessible(textfield)) {
                    int location = textfield.getCaretOffset(); // remember caret location because setting the text resets it
                    textfield.setText(label.getText());
                    textfield.setSelection(location);
                }
                if (isAccessible(headerColorPreviewLabel)) {
                    headerColorPreviewLabel.setBackground(
                        new Color(null, label.getColorHeader()[0], label.getColorHeader()[1], label.getColorHeader()[2]));
                }
                if (isAccessible(textColorPreviewLabel)) {
                    // FIXME: resource leak: The color object is never disposed!
                    textColorPreviewLabel.setBackground(
                        new Color(null, label.getColorText()[0], label.getColorText()[1], label.getColorText()[2]));
                }
                if (isAccessible(bgColorPreviewLabel)) {
                    // FIXME: resource leak: The color object is never disposed!
                    bgColorPreviewLabel.setBackground(new Color(null, label.getColorBackground()[0], label.getColorBackground()[1], label
                        .getColorBackground()[2]));
                }
                if (isAccessible(bgAlphaScale)) {
                    bgAlphaScale.setSelection(label.getAlphaDisplay());
                }
                if (isAccessible(bgAlphaValueText)) {
                    bgAlphaValueText.setText("" + ((int) Math.ceil((label.getAlphaDisplay() / SCALE_TO_PERCENT_FACTOR))));
                }
                if (isAccessible(bgBorderButton)) {
                    bgBorderButton.setSelection(label.hasBorder());
                }
                if (isAccessible(labelPositionButtons)) {
                    updateLabelPositionSelection(label.getLabelPosition());
                }
                if (isAccessible(textAlignmentButtons)) {
                    updateAlignmentButtonsSelection(textAlignmentButtons, label.getTextAlignmentType());
                }
                if (isAccessible(headerAlignmentButtons)) {
                    updateAlignmentButtonsSelection(headerAlignmentButtons, label.getHeaderAlignmentType());
                }
                if (isAccessible(headerSizeSpinner)) {
                    headerSizeSpinner.setSelection(label.getHeaderTextSize());
                }
                if (isAccessible(textSizeSpinner)) {
                    textSizeSpinner.setSelection(label.getTextSize());
                }
            }

        });
    }

    private boolean isAccessible(Control[] controls) {
        boolean accessible = true;
        for (Control c : Arrays.asList(controls)) {
            accessible &= isAccessible(c);
        }
        return accessible;
    }

    private boolean isAccessible(Control c) {
        return c != null && !c.isDisposed();
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        updateContent();
    }

    /**
     * Selection listener for selecting the alignment.
     *
     * @author Marc Stammerjohann
     */
    private class PositionSelectionListener implements SelectionListener {

        private LabelPosition type;

        PositionSelectionListener(LabelPosition type) {
            this.type = type;
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            LabelPosition oldValue = label.getLabelPosition();
            Button selectedButton = (Button) event.getSource();
            if (selectedButton.getSelection()) {
                if (!type.equals(oldValue)) {
                    editProperty(new Value(Value.ValueType.LABEL_POSITION, oldValue), new Value(Value.ValueType.TEXT_ALIGNMENT, type));
                }
                if (OSFamily.isLinux()) {
                    // preventing focus frame around the empty button text
                    textfield.setFocus();
                }
            }

        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {}

    }

    /**
     * Selection listener for alignment selection.
     * 
     * @author Jascha Riedel
     */
    private class TextAlignmentSelectionListener implements SelectionListener {

        private final TextAlignmentType textAlignmentType;

        private final Value.ValueType valueType;

        TextAlignmentSelectionListener(Value.ValueType valueType, TextAlignmentType textAlignmentType) {
            this.valueType = valueType;
            this.textAlignmentType = textAlignmentType;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {}

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            TextAlignmentType oldValue;
            if (valueType == Value.ValueType.HEADER_ALIGNMENT) {
                oldValue = label.getHeaderAlignmentType();
            } else if (valueType == Value.ValueType.TEXT_ALIGNMENT) {
                oldValue = label.getTextAlignmentType();
            } else {
                oldValue = TextAlignmentType.LEFT;
            }

            if (((Button) arg0.getSource()).getSelection()) {
                if (textAlignmentType != oldValue) {
                    editProperty(new Value(valueType, oldValue),
                        new Value(valueType, textAlignmentType));
                }
                if (OSFamily.isLinux()) {
                    // preventing focus frame around the empty button text
                    textfield.setFocus();
                }
            }
        }
    }

    /**
     * Selection listener for selecting the transparency.
     *
     * @author Marc Stammerjohann
     */
    private class AplhaSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            int oldValue = label.getAlphaDisplay();
            int newValue = bgAlphaScale.getSelection();
            if (newValue != oldValue) {
                if (editTransparencyCommand == null) {
                    editTransparencyCommand =
                        editProperty(new Value(Value.ValueType.TRANSPARENCY, oldValue), new Value(Value.ValueType.TRANSPARENCY,
                            newValue));
                } else {
                    editTransparencyCommand.setNewValue(new Value(Value.ValueType.TRANSPARENCY, newValue));
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
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
                    bgAlphaScale.setSelection(((int) (SCALE_TO_PERCENT_FACTOR * value)));
                }
            } catch (NumberFormatException e) {
                text = ""; // Should never happen
            }
        }

    }

    /**
     * Mouse listener for selecting the color.
     *
     * @author Marc Stammerjohann
     */
    private class ColorMouseListener implements MouseListener {

        private Value.ValueType type;

        ColorMouseListener(final Value.ValueType type) {
            this.type = type;
        }

        @Override
        public void mouseUp(MouseEvent event) { // Create own shell to place the color selector
                                                // where desired.
            int[] oldValue = null;
            if (type.equals(Value.ValueType.COLOR_BACKGROUND)) {
                oldValue = label.getColorBackground();
            } else if (type.equals(Value.ValueType.COLOR_TEXT)) {
                oldValue = label.getColorText();
            } else if (type.equals(Value.ValueType.COLOR_HEADER)) {
                oldValue = label.getColorHeader();
            } else {
                oldValue = new int[] { 0, 0, 0 };
            }
            int displayWidth = event.display.getBounds().width;
            int displayHeight = event.display.getBounds().height;
            Shell colorSelectorShell = new Shell(event.display);
            colorSelectorShell.setLocation(displayWidth / 2 - COLORSELECTOROFFSET_X, displayHeight / 2 - COLORSELECTOROFFSET_Y);
            ColorSelector cs = new ColorSelector(colorSelectorShell);
            cs.setColorValue(new RGB(oldValue[0], oldValue[1], oldValue[2]));
            cs.open();
            int[] newValue = new int[] { cs.getColorValue().red, cs.getColorValue().green, cs.getColorValue().blue };
            if (!Arrays.equals(newValue, oldValue)) {
                editProperty(new Value(type, oldValue), new Value(type, newValue));
            }
            colorSelectorShell.dispose();
        }

        @Override
        public void mouseDoubleClick(MouseEvent arg0) {}

        @Override
        public void mouseDown(MouseEvent arg0) {}

    }

    /**
     * Key listener for the text field.
     *
     * @author Marc Stammerjohann
     */
    private class TextKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent event) {
            if (event.stateMask == SWT.CTRL && event.keyCode == 'a') {
                textfield.setSelection(0);
                headerTextField.setSelection(0);
                Object src = event.getSource();
                if (src.equals(textfield)) {
                    textfield.selectAll();
                } else if (src.equals(headerTextField)) {
                    headerTextField.selectAll();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent arg0) {}
    }

    @Override
    public void execute(WorkflowLabelCommand command) {
        command.setCommandStack(getCommandStack());
        command.setWorkflowLabel(label);
        command.initialize();
        if (command.canExecute()) {
            getCommandStack().execute(new LabelCommandWrapper(command));
        }
    }

    /**
     * {@link WorkflowLabelCommand} to change the value of a property through editing.
     *
     * @author Marc Stammerjohann
     */
    protected final class EditValueCommand extends AbstractWorkflowLabelCommand {

        private final Value oldValue;

        private Value newValue;

        private EditValueCommand(final Value oldValue, Value newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Value getOldValue() {
            return oldValue;
        }

        public Value getNewValue() {
            return newValue;
        }

        public void setNewValue(final Value newValue) {
            this.newValue = newValue;
            execute2();
        }

        @Override
        protected void execute2() {
            switch (newValue.getType()) {
            case HEADER_TEXT:
                label.setHeaderText(newValue.getTextValue());
                break;
            case TEXT:
                label.setText(newValue.getTextValue());
                break;
            case COLOR_HEADER:
                label.setColorHeader(newValue.getColorValues());
                break;
            case COLOR_TEXT:
                label.setColorText(newValue.getColorValues());
                break;
            case COLOR_BACKGROUND:
                label.setColorBackground(newValue.getColorValues());
                break;
            case TRANSPARENCY:
                label.setAlpha(newValue.getTransparencyValue());
                break;
            case LABEL_POSITION:
                label.setLabelPosition(newValue.getLabelPosition());
                break;
            case TEXT_ALIGNMENT:
                label.setTextAlignmentType(newValue.getTextAlignmentType());
                break;
            case HEADER_ALIGNMENT:
                label.setHeaderAlignmentType(newValue.getTextAlignmentType());
            case BORDER:
                label.setHasBorder(newValue.getHasBorder());
                break;
            case HEADER_SIZE:
                label.setHeaderTextSize(newValue.getTextSize());
                break;
            case TEXT_SIZE:
                label.setTextSize(newValue.getTextSize());
                break;
            default:
                break;
            }
            label.firePropertChangeEvent();
            label.firePropertyChange(WorkflowLabel.COMMAND_CHANGE, newValue);
        }

        @Override
        protected void undo2() {
            switch (oldValue.getType()) {
            case HEADER_TEXT:
                label.setHeaderText(oldValue.getTextValue());
                break;
            case TEXT:
                label.setText(oldValue.getTextValue());
                break;
            case COLOR_HEADER:
                label.setColorHeader(oldValue.getColorValues());
                break;
            case COLOR_TEXT:
                label.setColorText(oldValue.getColorValues());
                break;
            case COLOR_BACKGROUND:
                label.setColorBackground(oldValue.getColorValues());
                break;
            case TRANSPARENCY:
                label.setAlpha(oldValue.getTransparencyValue());
                break;
            case LABEL_POSITION:
                label.setLabelPosition(oldValue.getLabelPosition());
                break;
            case TEXT_ALIGNMENT:
                label.setTextAlignmentType(oldValue.getTextAlignmentType());
                break;
            case HEADER_ALIGNMENT:
                label.setHeaderAlignmentType(oldValue.getTextAlignmentType());
                break;
            case BORDER:
                label.setHasBorder(oldValue.getHasBorder());
                break;
            case HEADER_SIZE:
                label.setHeaderTextSize(oldValue.getTextSize());
                break;
            case TEXT_SIZE:
                label.setTextSize(oldValue.getTextSize());
                break;
            default:
                break;
            }
            label.firePropertChangeEvent();
            label.firePropertyChange(WorkflowLabel.COMMAND_CHANGE, oldValue);
        }
    }

    /**
     * A wrapper class to wrap different data types to be used within the {@link EditValueCommand}.
     *
     * @author Marc Stammerjohann
     * @author Jascha Riedel
     */
    private static class Value {

        private ValueType type;

        private String textValue;

        private int[] colorValues;

        private int transparencyValue;

        private LabelPosition labelPosition;

        private TextAlignmentType textAlignmentType;

        private boolean hasBorder;

        private int textSize;

        Value(final ValueType type, final String value) {
            this.type = type;
            if (type == ValueType.TEXT
                || type == ValueType.HEADER_TEXT) {
                this.textValue = value;
            }
        }

        Value(final ValueType type, final int[] colorValues) {
            this.type = type;
            if (type == ValueType.COLOR_BACKGROUND
                || type == ValueType.COLOR_TEXT
                || type == ValueType.COLOR_HEADER) {
                this.colorValues = colorValues;
            }
        }

        Value(final ValueType type, final int value) {
            this.type = type;
            if (type == ValueType.TEXT_SIZE || type == ValueType.HEADER_SIZE) {
                this.textSize = value;
            } else if (type == ValueType.TRANSPARENCY) {
                this.transparencyValue = value;
            }
        }

        Value(final ValueType type, LabelPosition labelPosition) {
            this.type = ValueType.LABEL_POSITION;
            this.labelPosition = labelPosition;
        }

        Value(final ValueType type, TextAlignmentType textAlignmentType) {
            this.type = type;
            this.textAlignmentType = textAlignmentType;
        }

        Value(final ValueType type, boolean border) {
            this.type = type;
            if (type == ValueType.BORDER) {
                this.hasBorder = border;
            }
        }

        public int[] getColorValues() {
            return colorValues;
        }

        public String getTextValue() {
            return textValue;
        }

        public int getTransparencyValue() {
            return transparencyValue;
        }

        public LabelPosition getLabelPosition() {
            return labelPosition;
        }

        public TextAlignmentType getTextAlignmentType() {
            return textAlignmentType;
        }

        public boolean getHasBorder() {
            return hasBorder;
        }

        public int getTextSize() {
            return textSize;
        }

        public ValueType getType() {
            return type;
        }

        /**
         * Contains all editable properties.
         *
         * @author Marc Stammerjohann
         */
        private enum ValueType {
            HEADER_TEXT,
            TEXT,
            COLOR_HEADER,
            COLOR_TEXT,
            COLOR_BACKGROUND,
            TRANSPARENCY,
            LABEL_POSITION,
            HEADER_ALIGNMENT,
            TEXT_ALIGNMENT,
            BORDER,
            HEADER_SIZE,
            TEXT_SIZE;
        }

    }

    /**
     * A wrapper class to wrap {@link WorkflowLabelCommand}s in GEF {@link Command}s.
     * 
     * @author Marc Stammerjohann
     */
    private static final class LabelCommandWrapper extends WorkflowPropertySection.CommandWrapper {

        private WorkflowLabelCommand command;

        LabelCommandWrapper(WorkflowLabelCommand command) {
            super(command);
            this.command = command;
        }

    }

    /**
     * Adapter to listen to events in the backing model and translate it to events in the {@link Synchronizer}.
     * 
     * @author Christian Weiss
     * @author Marc Stammerjohann
     */
    private final class SynchronizerAdapter implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            final String propertyNameValue = event.getPropertyName();
            if (propertyNameValue.equals(WorkflowLabel.COMMAND_CHANGE)) {
                synchronizer.handlePropertyChange((Value) event.getNewValue());
            }
        }

    }

    /**
     * Listener class responsible for keeping the GUI in sync with the model.
     * 
     * <p>
     * The <code>Synchronizer</code> gets registered at the model to listen to change events (properties & channels) and executes
     * appropriate actions to reflect those changes in the GUI.
     * </p>
     * 
     * <p>
     * The integration of a <code>Synchronizer</code> is as follows:
     * <ul>
     * <li>A {@link SynchronizerAdapter} gets registered to the backing model (a {@link WorkflowLabel}. This adapter filters events and
     * converts the non-filtered to invocations of the {@link Synchronizer} instance created via
     * {@link WorkflowLabelPropertySection#createSynchronizer()}.</li>
     * <li>The {@link Synchronizer} receives those filtered events via its custom interface and reacts through updating the GUI
     * appropriately. The default implementation {@link DefaultSynchronizer} forwards updates to the {@link Updater} instance created via
     * {@link WorkflowLabelPropertySection#createUpdater()}.</li>
     * </ul>
     * </p>
     * 
     * @author Christian Weiss
     * @author Marc Stammerjohann
     */
    public interface Synchronizer {

        /**
         * React on the change of a property.
         * 
         * @param newValue the new value of the property
         */
        void handlePropertyChange(final Value newValue);

    }

    /**
     * Default implementation of a {@link Synchronizer}, forwarding all updates to the {@link Updater}.
     * 
     * <p>
     * It is adviced to derive from this class and call the super class implementation as the very first thing in overwritten methods.
     * </p>
     * 
     * @author Christian Weiss
     * @author Marc Stammerjohann
     */
    protected class DefaultSynchronizer implements Synchronizer {

        @Override
        public void handlePropertyChange(Value newValue) {
            Control control = null;
            switch (newValue.getType()) {
            case HEADER_TEXT:
                control = headerTextField;
                break;
            case TEXT:
                control = textfield;
                break;
            case COLOR_HEADER:
                control = headerColorPreviewLabel;
                break;
            case COLOR_TEXT:
                control = textColorPreviewLabel;
                break;
            case COLOR_BACKGROUND:
                control = bgColorPreviewLabel;
                break;
            case TRANSPARENCY:
                getUpdater().updateControl(bgAlphaValueText, newValue);
                control = bgAlphaScale;
                break;
            case BORDER:
                control = bgBorderButton;
                break;
            case HEADER_SIZE:
                control = headerSizeSpinner;
                break;
            case TEXT_SIZE:
                control = textSizeSpinner;
                break;
            default:
                break;
            }
            getUpdater().updateControl(control, newValue);
        }

    }

    /**
     * Interface for handlers updating the UI.
     * 
     * @author Christian Weiss
     * @author Marc Stammerjohann
     */
    protected interface Updater {

        /**
         * Updates the gui elements.
         * 
         * @param newValue the value to display
         */
        void updateControl(final Control control, final Value newValue);

    }

    /**
     * Default {@link Updater} implementation of the handler to update the UI.
     * 
     * @author Christian Weiss
     * @author Marc Stammerjohann
     */
    protected class DefaultUpdater implements Updater {

        @Override
        public void updateControl(Control control, Value newValue) {
            if (control != null && control.isDisposed()) {
                return;
            }
            if (newValue != null) {
                switch (newValue.getType()) {
                case HEADER_TEXT:
                    if (control instanceof StyledText) {
                        final StyledText styledTextControl = (StyledText) control;
                        styledTextControl.setText(newValue.getTextValue());
                        styledTextControl.setSelection(newValue.getTextValue().length());
                    }
                    break;
                case TEXT:
                    if (control instanceof StyledText) {
                        final StyledText sytledTextControl = (StyledText) control;
                        int offset = sytledTextControl.getCaretOffset();
                        int scrollbarSel = sytledTextControl.getTopIndex();

                        sytledTextControl.setText(newValue.getTextValue());
                        if (offset > 0 && offset < newValue.getTextValue().length() - 1) {
                            sytledTextControl.setSelection(offset);
                            sytledTextControl.setTopIndex(scrollbarSel);
                        } else {
                            sytledTextControl.setSelection(newValue.getTextValue().length());
                        }
                    }
                    break;
                case COLOR_HEADER:
                    refreshColorLabels(control, newValue);
                    break;
                case COLOR_TEXT:
                    refreshColorLabels(control, newValue);
                    break;
                case COLOR_BACKGROUND:
                    refreshColorLabels(control, newValue);
                    break;
                case TRANSPARENCY:
                    if (control instanceof Scale) {
                        Scale scaleControl = (Scale) control;
                        scaleControl.setSelection(newValue.getTransparencyValue());
                    } else if (control instanceof Text) {
                        Text textControl = (Text) control;
                        textControl.setText("" + ((int) Math.ceil((newValue.getTransparencyValue() / SCALE_TO_PERCENT_FACTOR))));
                    }
                    break;
                case BORDER:
                    if (control instanceof Button) {
                        Button buttonControl = (Button) control;
                        buttonControl.setSelection(newValue.getHasBorder());
                    }
                    break;
                case HEADER_SIZE:
                    if (control instanceof Spinner) {
                        Spinner spinner = (Spinner) control;
                        spinner.setSelection(newValue.getTextSize());
                    }
                    break;
                case TEXT_SIZE:
                    if (control instanceof Spinner) {
                        Spinner spinner = (Spinner) control;
                        spinner.setSelection(newValue.getTextSize());
                    }
                    break;
                case LABEL_POSITION:
                    if (!labelPositionButtons[0].isDisposed()) {
                        updateLabelPositionSelection(newValue.getLabelPosition());
                    }
                    break;
                case HEADER_ALIGNMENT:
                    if (!headerAlignmentButtons[0].isDisposed()) {
                        updateAlignmentButtonsSelection(headerAlignmentButtons, newValue.getTextAlignmentType());
                    }
                    break;
                case TEXT_ALIGNMENT:
                    if (!textAlignmentButtons[0].isDisposed()) {
                        updateAlignmentButtonsSelection(textAlignmentButtons, newValue.getTextAlignmentType());
                    }
                    break;
                default:
                    break;
                }
            }
        }

        private void refreshColorLabels(Control control, Value newValue) {
            if (control instanceof Label) {
                final Label labelControl = (Label) control;
                // FIXME: resource leak: The color object is never disposed!
                labelControl.setBackground(new Color(null, newValue.getColorValues()[0], newValue.getColorValues()[1],
                    newValue.getColorValues()[2]));
            }
        }

    }

}
