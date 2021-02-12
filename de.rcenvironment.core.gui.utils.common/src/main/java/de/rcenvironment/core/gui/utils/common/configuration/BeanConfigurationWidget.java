/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * A {@link Composite} displaying editors for the immediate properties of a configuration based on
 * its {@link IConfigurationSource}.
 * 
 * @author Christian Weiss
 */
public class BeanConfigurationWidget extends BeanPropertyWidget {

    /** The {@link IConfigurationSource} providing the property information. */
    private IConfigurationSource configurationSource;

    /**
     * Instantiates a new bean configuration widget.
     * 
     * @param parent the parent composite
     * @param style the style
     */
    public BeanConfigurationWidget(final Composite parent, final int style) {
        super(parent, style);
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.utils.common.configuration.BeanPropertyWidget#setObject(java.lang.Object)
     */
    @Override
    public void setObject(final Object object, final boolean lookupPropertySource) {
        if (lookupPropertySource) {
            IConfigurationSource newConfigurationSource =
                (IConfigurationSource) AdapterManager.getInstance().getAdapter(object, IConfigurationSource.class);
            if (newConfigurationSource == null) {
                newConfigurationSource = (IConfigurationSource) Platform.getAdapterManager().getAdapter(object, IConfigurationSource.class);
            }
            this.configurationSource = newConfigurationSource;
            setPropertySource(newConfigurationSource);
        }
        createControls();
        resetFocus();
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.utils.common.configuration.BeanPropertyWidget#createControls()
     */
    @Override
    protected void createControls() {
        final List<IPropertyDescriptor> descriptors = Arrays.asList(configurationSource.getConfigurationPropertyDescriptors());
        // sort the properties according to their display name
        sortPropertyDescriptors(descriptors);
        for (final IPropertyDescriptor descriptor : descriptors) {
            createControls(descriptor);
        }
    }

}
