/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

/**
 * A selectable implementation of a certain {@link ComponentInterface}.
 * {@link ComponentRevision}s are abstract representations of a workflow
 * behaviour, independent of the actual execution location; the latter is
 * represented by {@link ComponentInstallation} s. Each
 * {@link ComponentInstallation} of the same {@link ComponentRevision} is
 * expected to show the same semantic behaviour.
 * 
 * Examples of {@link ComponentRevision}s are different versions of the same
 * tool, or different implementations like Jython vs. CPython for the Script
 * component.
 * 
 * In the user interface, {@link ComponentRevision}s may be shown as
 * second-level elements to the {@link ComponentInterface} they belong to.
 * 
 * @author Robert Mischke
 */
public interface ComponentRevision {

    /**
     * @return the (semantic) {@link ComponentInterface} that this
     *         {@link ComponentRevision} implements
     */
    ComponentInterface getComponentInterface();

    /**
     * @return name of the implementing class
     */
    String getClassName();

}
