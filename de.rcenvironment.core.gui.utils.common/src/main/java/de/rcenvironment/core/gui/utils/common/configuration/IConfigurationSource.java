/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;

/**
 * The Interface IConfigurationSource.
 * 
 * @author Christian Weiss
 */
public interface IConfigurationSource extends IPropertySource2 {

    /**
     * Returns the {@IPropertyDescriptor}s for the
     * <code>ConfigurationProperty</code>s.
     * 
     * @return the configuration property descriptors
     */
    IPropertyDescriptor[] getConfigurationPropertyDescriptors();

}
