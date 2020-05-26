/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.configuration.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that a certain property of a java bean shall appear as a
 * {@link de.rcenvironment.core.utils.incubator.configuration.ConfigurationProperty} in the
 * {@link de.rcenvironment.core.utils.incubator.configuration.ConfigurationInfo} of the holder class.
 * 
 * @author Christian Weiss
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Configurable {

    /**
     * The interface to be implemented by value providers of a custom choice set.
     */
    public interface ValueProvider {

        /**
         * Injects the object holding the associated
         * {@link de.rcenvironment.core.utils.incubator.configuration.ConfigurationProperty}.
         * 
         * @param object the holding object
         */
        void setObject(Object object);

        /**
         * Returns the values which are valid for the configuration property.
         * 
         * @return the valid values
         */
        Object[] getValues();

    }

    /**
     * The interface to be implemented by label providers of a custom choice set.
     */
    public interface LabelProvider {

        /**
         * Injects the object holding the associated
         * {@link de.rcenvironment.core.utils.incubator.configuration.ConfigurationProperty}.
         * 
         * @param object the holding object
         */
        void setObject(Object object);

        /**
         * Returns the label for the given value object.
         * 
         * @param object the value object
         * @return the label
         */
        String getLabel(Object object);

    }

    /**
     * The information whether the annotated property represents a
     * {@link de.rcenvironment.core.utils.incubator.configuration.ConfigurationProperty}.
     * 
     * @return true, if the annotated property represents a
     *         {@link de.rcenvironment.core.utils.incubator.configuration.ConfigurationProperty}.
     */
    boolean value() default true;

    /**
     * Returns the configured choice provider.
     * 
     * @return the class<? extends choice provider>
     */
    Class<? extends ValueProvider> valueProvider() default NoValueProvider.class;

    /**
     * Returns the configured choice provider.
     * 
     * @return the class<? extends choice provider>
     */
    Class<? extends LabelProvider> labelProvider() default NoLabelProvider.class;

    /**
     * The abstract {@link ValueProvider} class providing a value for an empty selection.
     */
    abstract class NoValueProvider implements ValueProvider {
        //
    }

    /**
     * The abstract {@link LabelProvider} class providing a value for an empty selection.
     */
    abstract class NoLabelProvider implements LabelProvider {
        //
    }

}
