/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel.AlignmentType;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.utils.common.OSFamily;

/**
 * Property Section for all WorkflowLabel.
 * 
 * @author Sascha Zur
 * @author Marc Stammerjohann
 * @author Doreen Seider
 */
public class WorkflowLabelPropertySection extends WorkflowPropertySection implements WorkflowLabelCommand.Executor {

    private static final int HORIZONTAL_SPACING = 20;

    private static final int COLORSELECTOROFFSET_X = 50;

    private static final int COLORSELECTOROFFSET_Y = 200;

    private static final int ALPHA_TEXT_WIDTH = 25;

    private static final String TABS = "     ";

    private static final int TEXTFIELD_HEIGHT = 10 * 10;

    private static final int MAX_255 = 255;

    private static final int MAX_100 = 100;

    private static final double SCALE_TO_PERCENT_FACTOR = 2.55;

    private WorkflowLabel label;

    private StyledText textfield;

    private Button[] alignmentButtons;

    private AlignmentType[] values;

    private Label textColorPreviewLabel;

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
            updateAlignmentSelection(label.getAlignmentType());
        }
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
        parent.setLayout(new FillLayout(SWT.VERTICAL));

        createTextSection(aTabbedPropertySheetPage, parent);
        createBackgroundSection(aTabbedPropertySheetPage, parent);

    }

    private void createTextSection(TabbedPropertySheetPage aTabbedPropertySheetPage, Composite parent) {
        final Section textPropertiesSection = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent,
            aTabbedPropertySheetPage.getWidgetFactory(), "Text");

        Composite textPropertiesComposite = aTabbedPropertySheetPage.getWidgetFactory().createFlatFormComposite(textPropertiesSection);
        textPropertiesComposite.setLayout(new GridLayout(2, false));
        textPropertiesComposite.setLayoutData(new GridData(
            GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | SWT.BORDER));
        textPropertiesSection.setClient(textPropertiesComposite);

        textfield = new StyledText(textPropertiesComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData textData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        textData.heightHint = TEXTFIELD_HEIGHT;
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
            }

            @Override
            public void focusGained(FocusEvent arg0) {}
        });

        createLabelAlignmentGroup(textPropertiesComposite);

        Composite textColorSizePropertiesComposite = new Composite(textPropertiesComposite, SWT.NONE);
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
        textColorLabel.setText("Color:");
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
    }

    private void createBackgroundSection(TabbedPropertySheetPage aTabbedPropertySheetPage, Composite parent) {
        final Section bgPropertiesSection = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent,
            aTabbedPropertySheetPage.getWidgetFactory(), "Background");

        Composite bgPropertiesComposite = aTabbedPropertySheetPage.getWidgetFactory().createFlatFormComposite(bgPropertiesSection);
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
        bgAlphaLabel.setText("Transparancy:");
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

    private void createLabelAlignmentGroup(Composite content) {
        Group group = new Group(content, SWT.SHADOW_IN);
        group.setText(Messages.labelAlignment);
        group.setLayout(new GridLayout(3, true));
        GridData groupData = new GridData();
        groupData.heightHint = TEXTFIELD_HEIGHT;
        group.setLayoutData(groupData);
        int[] horizontalAlignment = { SWT.LEFT, SWT.CENTER, SWT.RIGHT };
        int[] verticalAlignment = { SWT.TOP, SWT.CENTER, SWT.BOTTOM };
        alignmentButtons = new Button[9];
        values = AlignmentType.values();
        // vertical index is always same value for 3 buttons
        int verticalIndex = 0 - 1;
        for (int i = 0; i < alignmentButtons.length; i++) {
            if (i % verticalAlignment.length == 0) {
                verticalIndex++;
            }
            alignmentButtons[i] = new Button(group, SWT.RADIO);
            alignmentButtons[i].setLayoutData(new GridData(horizontalAlignment[i % horizontalAlignment.length],
                verticalAlignment[verticalIndex], true,
                true, 1, 1));
            alignmentButtons[i].addSelectionListener(new AlignmentSelectionListener(values[i]));
        }
    }

    private void updateAlignmentSelection(AlignmentType alignmentType) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(alignmentType)) {
                alignmentButtons[i].setSelection(true);
            } else {
                alignmentButtons[i].setSelection(false);
            }
        }
    }

    protected EditValueCommand editProperty(final Value oldValue, final Value newValue) {
        final EditValueCommand command = new EditValueCommand(oldValue, newValue);
        execute(command);
        return command;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (textfield != null) {
            textfield.setText(label.getText());
            textfield.setSelection(textfield.getText().length());
        }
        if (textColorPreviewLabel != null) {
            textColorPreviewLabel.setBackground(new Color(null, label.getColorText()[0], label.getColorText()[1], label.getColorText()[2]));
        }
        if (bgColorPreviewLabel != null) {
            bgColorPreviewLabel.setBackground(new Color(null, label.getColorBackground()[0], label.getColorBackground()[1], label
                .getColorBackground()[2]));
        }
        if (bgAlphaScale != null) {
            bgAlphaScale.setSelection(label.getAlphaDisplay());
        }
        if (bgAlphaValueText != null) {
            bgAlphaValueText.setText("" + ((int) Math.ceil((label.getAlphaDisplay() / SCALE_TO_PERCENT_FACTOR))));
        }
        if (bgBorderButton != null) {
            bgBorderButton.setSelection(label.hasBorder());
        }
        if (alignmentButtons != null) {
            updateAlignmentSelection(label.getAlignmentType());
        }
        if (textSizeSpinner != null) {
            textSizeSpinner.setSelection(label.getTextSize());
        }
    }

    /**
     * Selection listener for selecting the alignment.
     *
     * @author Marc Stammerjohann
     */
    private class AlignmentSelectionListener implements SelectionListener {

        private AlignmentType type;

        AlignmentSelectionListener(AlignmentType type) {
            this.type = type;
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            AlignmentType oldValue = label.getAlignmentType();
            Button selectedButton = (Button) event.getSource();
            if (selectedButton.getSelection()) {
                if (!type.equals(oldValue)) {
                    editProperty(new Value(oldValue), new Value(type));
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
                textfield.selectAll();
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
            case TEXT:
                label.setText(newValue.getTextValue());
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
            case ALIGNMENT:
                label.setAlignmentType(newValue.getAlignmentValue());
                break;
            case BORDER:
                label.setHasBorder(newValue.getHasBorder());
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
            case TEXT:
                label.setText(oldValue.getTextValue());
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
            case ALIGNMENT:
                label.setAlignmentType(oldValue.getAlignmentValue());
                break;
            case BORDER:
                label.setHasBorder(oldValue.getHasBorder());
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
     */
    private static class Value {

        private ValueType type;

        private String textValue;

        private int[] colorValues;

        private int transparencyValue;

        private AlignmentType alignmentValue;

        private boolean hasBorder;

        private int textSize;

        Value(final ValueType type, final String value) {
            this.type = type;
            if (type == ValueType.TEXT) {
                this.textValue = value;
            }
        }

        Value(final ValueType type, final int[] colorValues) {
            this.type = type;
            if (type == ValueType.COLOR_BACKGROUND || type == ValueType.COLOR_TEXT) {
                this.colorValues = colorValues;
            }
        }

        Value(final ValueType type, final int value) {
            this.type = type;
            if (type == ValueType.TEXT_SIZE) {
                this.textSize = value;
            } else if (type == ValueType.TRANSPARENCY) {
                this.transparencyValue = value;
            }
        }

        Value(AlignmentType alignmentValue) {
            this.type = ValueType.ALIGNMENT;
            this.alignmentValue = alignmentValue;
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

        public AlignmentType getAlignmentValue() {
            return alignmentValue;
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
            TEXT, COLOR_TEXT, COLOR_BACKGROUND, TRANSPARENCY, ALIGNMENT, BORDER, TEXT_SIZE;
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
     * Adapter to listen to events in the backing model and translate it to events in the
     * {@link Synchronizer}.
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
     * The <code>Synchronizer</code> gets registered at the model to listen to change events
     * (properties & channels) and executes appropriate actions to reflect those changes in the GUI.
     * </p>
     * 
     * <p>
     * The integration of a <code>Synchronizer</code> is as follows:
     * <ul>
     * <li>A {@link SynchronizerAdapter} gets registered to the backing model (a
     * {@link WorkflowLabel}. This adapter filters events and converts the non-filtered to
     * invocations of the {@link Synchronizer} instance created via
     * {@link WorkflowLabelPropertySection#createSynchronizer()}.</li>
     * <li>The {@link Synchronizer} receives those filtered events via its custom interface and
     * reacts through updating the GUI appropriately. The default implementation
     * {@link DefaultSynchronizer} forwards updates to the {@link Updater} instance created via
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
     * Default implementation of a {@link Synchronizer}, forwarding all updates to the
     * {@link Updater}.
     * 
     * <p>
     * It is adviced to derive from this class and call the super class implementation as the very
     * first thing in overwritten methods.
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
            case TEXT:
                control = textfield;
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
                case TEXT:
                    if (control instanceof StyledText) {
                        final StyledText sytledTextControl = (StyledText) control;
                        sytledTextControl.setText(newValue.getTextValue());
                        sytledTextControl.setSelection(newValue.getTextValue().length());
                    }
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
                case TEXT_SIZE:
                    if (control instanceof Spinner) {
                        Spinner spinner = (Spinner) control;
                        spinner.setSelection(newValue.getTextSize());
                    }
                    break;
                case ALIGNMENT:
                    if (!alignmentButtons[0].isDisposed()) {
                        updateAlignmentSelection(newValue.getAlignmentValue());
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
                labelControl.setBackground(new Color(null, newValue.getColorValues()[0], newValue.getColorValues()[1],
                    newValue.getColorValues()[2]));
            }
        }

    }

}
