/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
