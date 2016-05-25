/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
