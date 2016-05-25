/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * An abstract source of {@link ImageDescriptor}s. Added to provide a flexible way for bundles to provide their own image resources, and to
 * make the "extensible enum" pattern possible.
 * 
 * @author Robert Mischke
 */
public interface ImageSource {

    /**
     * @return the {@link ImageDescriptor} of this image resource
     */
    ImageDescriptor getImageDescriptor();

}
