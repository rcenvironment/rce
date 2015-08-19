/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * A {@link Composite} displaying editors for the immediate properties of a bean based on its
 * {@link IPropertySource}.
 * 
 * @author Christian Weiss
 */
public class BeanPropertyWidget extends Composite {

    /** The {@link IPropertySource} providing the property information. */
    private IPropertySource propertySource;

    private Control defaultFocusControl;

    /**
     * Instantiates a new {@link BeanPropertyWidget}.
     * 
     * @param parent the parent composite
     * @param style the style
     */
    public BeanPropertyWidget(final Composite parent, final int style) {
        super(parent, style);
        final GridLayout layout = new GridLayout(2, false);
        setLayout(layout);
    }

    /**
     * Sets the object.
     * 
     * @param object the new object
     */
    public void setObject(final Object object) {
        setObject(object, true);
    }

    /**
     * Sets the object.
     * 
     * @param object the object
     * @param lookupPropertySource true, if the {@link IPropertySource} shall be looked up
     */
    public void setObject(final Object object, final boolean lookupPropertySource) {
        if (lookupPropertySource) {
            IPropertySource newPropertySource = (IPropertySource) AdapterManager.getInstance().getAdapter(object, IPropertySource.class);
            if (newPropertySource == null) {
                newPropertySource = (IPropertySource) Platform.getAdapterManager().getAdapter(object, IPropertySource.class);
            }
            this.propertySource = newPropertySource;
            setPropertySource(newPropertySource);
        }
        createControls();
    }

    protected void setPropertySource(final IPropertySource propertySource) {
        this.propertySource = propertySource;
    }

    /**
     * Creates the controls based on the properties of the {@link IPropertySource}.
     * 
     */
    protected void createControls() {
        final List<IPropertyDescriptor> descriptors = Arrays.asList(propertySource.getPropertyDescriptors());
        // sort the properties according to their display name
        sortPropertyDescriptors(descriptors);
        for (final IPropertyDescriptor descriptor : descriptors) {
            createControls(descriptor);
        }
        resetFocus();
    }

    /**
     * Sorts the properties according to their display name.
     * 
     * @param descriptors the descriptors
     */
    protected void sortPropertyDescriptors(final List<? extends IPropertyDescriptor> descriptors) {
        Collections.sort(descriptors, new Comparator<IPropertyDescriptor>() {

            @Override
            public int compare(IPropertyDescriptor o1, IPropertyDescriptor o2) {
                return o1.getDisplayName().toLowerCase().compareTo(o2.getDisplayName().toLowerCase());
            }

        });
    }

    protected void createControls(final IPropertyDescriptor descriptor) {
        GridData layoutData;
        // label
        final Label label = new Label(this, SWT.NONE);
        label.setText(descriptor.getDisplayName());
        label.setToolTipText(descriptor.getDescription());
        layoutData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
        label.setLayoutData(layoutData);
        // editor
        final Composite editorComposite = new Composite(this, SWT.BORDER);
        editorComposite.setLayout(new FillLayout());
        final CellEditor editor = descriptor.createPropertyEditor(editorComposite);
        final Control control = editor.getControl();
        // set the very first input control as default focus control
        if (defaultFocusControl == null) {
            defaultFocusControl = control;
        }
        Object value = propertySource.getPropertyValue(descriptor.getId());
        if (editor instanceof TextCellEditor && value == null) {
            value = "";
        }
        boolean setValue = true;
        if (descriptor instanceof SelectionPropertyDescriptor && value == null) {
            setValue = false;
        }
        if (setValue) {
            editor.setValue(value);
        }
        final ICellEditorListener editorListener = new ICellEditorListener() {

            private final Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    editor.activate();
                    control.setVisible(true);
                }

            };

            @Override
            public void cancelEditor() {
                Display.getCurrent().asyncExec(runnable);
            }

            @Override
            public void editorValueChanged(boolean oldValidState,
                    boolean newValidState) {
                // Do nothing
            }

            @Override
            public void applyEditorValue() {
                final Object newValue;
                if (descriptor instanceof SelectionPropertyDescriptor) {
                    newValue = ((SelectionPropertyDescriptor) descriptor).getValue((Integer) editor.getValue());
                } else {
                    newValue = editor.getValue();
                }
                propertySource.setPropertyValue(descriptor.getId(), newValue);
                Display.getCurrent().asyncExec(runnable);
            }
        };
        editor.addListener(editorListener);
        control.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        editor.activate();
        control.setVisible(true);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        editorComposite.setLayoutData(layoutData);
    }

    /**
     * Resets the focus to the default input control, which usually is the topmost input control.
     * 
     */
    public void resetFocus() {
        defaultFocusControl.setFocus();
    }

}
