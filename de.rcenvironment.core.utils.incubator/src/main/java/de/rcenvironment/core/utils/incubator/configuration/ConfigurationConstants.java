/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.incubator.configuration;

import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.LabelProvider;
import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.NoLabelProvider;
import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.NoValueProvider;
import de.rcenvironment.core.utils.incubator.configuration.annotation.Configurable.ValueProvider;

/**
 * Class holding constants uses within the {@link Configurable} annotation. If it is declared there
 * the javac generates an error "annotation is missing <clinit>"
 * @author Doreen Seider
 */
public final class ConfigurationConstants {

    /** The {@link ChoiceProvider} representing a not set value. */
    public static final Class<? extends LabelProvider> NO_LABEL_PROVIDER = NoLabelProvider.class;

    /** The {@link ChoiceProvider} representing a not set value. */
    public static final Class<? extends ValueProvider> NO_VALUE_PROVIDER = NoValueProvider.class;
    
    /** Private constructor of this utility class. */
    private ConfigurationConstants() {}
}
