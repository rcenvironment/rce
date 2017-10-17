/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.spi.module;

/**
 * The default {@link ClassFilter} implementation. Its criterion for publishing an interface is whether the interface's FQN contains ".api."
 * or ".spi." as a substring.
 * 
 * @author Robert Mischke
 * 
 */
public class DefaultClassFilter implements ClassFilter {

    @Override
    public boolean accept(Class<?> clazz) {
        String fullName = clazz.getName();
        return fullName.contains(".api.") || fullName.contains(".spi.");
    }

}
