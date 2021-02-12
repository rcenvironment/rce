/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import java.util.List;
import java.util.Set;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;

/**
 * Represents the part of a component that is "anchored" in a workflow. Several {@link ComponentRevision}s may be suitable to be selected
 * for the same {@link ComponentInterface} .
 * 
 * In the user interface, available {@link ComponentInterface}s are shown as top-level elements in the workflow editor palette.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface ComponentInterface {

    /**
     * @return the end-user name of this "component"
     */
    String getDisplayName();

    /**
     * @return the name of the group/category to show this "component" under
     */
    String getGroupName();

    /**
     * @return The MD5 hash as a 32 character hex string of the concatenated icons.
     */
    String getIconHash();

    /**
     * @return 16x16 icon data (TODO specify format)
     */
    byte[] getIcon16();

    /**
     * @return 24x24 icon data (TODO specify format)
     *Note: It seems that no component needs this icon (call hierarchy), but data flow componenticons are not displayed correct without it
     */
    byte[] getIcon24();

    /**
     * @return 32x32 icon data (TODO specify format)
     */
    byte[] getIcon32();

    /**
     * @return color of component
     */
    ComponentColor getColor();

    /**
     * @return size of component
     */
    ComponentSize getSize();

    /**
     * @return shape of component
     */
    ComponentShape getShape();

    /**
     * @return the "base" identifier under which the component is registered, without any version component; note that this method signature
     *         included the version part in RCE before 9.0.0
     */
    String getIdentifier();

    /**
     * @return the "base" identifier under which the component is registered, with any non-empty version string appended with
     *         {@link ComponentConstants#ID_SEPARATOR}; note that this method may be removed/replaced in the future
     */
    String getIdentifierAndVersion();

    /**
     * @return all of the identifiers, under which the component is registered. It includes the default identifier and mainly, deprecated
     *         ones, each including any non-empty version strings appended with {@link ComponentConstants#ID_SEPARATOR}; note that the
     *         version parts may be removed in the future
     */
    List<String> getIdentifiers();

    /**
     * @return version information equivalent to {@link ComponentDescription#getVersion()} id; may be removed/replaced after migration
     */
    String getVersion();

    /**
     * @return {@link EndpointDefinitionsProvider} providing static and dynamic inpout {@link EndpointDefinition}s
     */
    EndpointDefinitionsProvider getInputDefinitionsProvider();

    /**
     * @return {@link EndpointDefinitionsProvider} providing static and dynamic outpout {@link EndpointDefinition}s
     */
    EndpointDefinitionsProvider getOutputDefinitionsProvider();

    /**
     * @return information about the component's configuration options
     */
    ConfigurationDefinition getConfigurationDefinition();

    /**
     * @return information about extended configuration options of a component
     */
    Set<ConfigurationExtensionDefinition> getConfigurationExtensionDefinitions();

    /**
     * @return <code>true</code> if the component can technically only be executed locally, otherwise <code>false</code>
     */
    boolean getLocalExecutionOnly();

    /**
     * @return <code>true</code> if the component should be disposed workflow disposal, <code>false</code> if immediately when the component
     *         had finished (default behavior)
     */
    boolean getPerformLazyDisposal();

    /**
     * @return <code>true</code> if the component is marked as deprecated and should not be used anymore as it will be removed in later
     *         versions
     */
    boolean getIsDeprecated();

    /**
     * @return <code>true</code> if the component can handle incoming {@link TypedDatum}s of {@link DataType} {@link NotAValueTD}, otherwise
     *         <code>false</code>
     */
    boolean getCanHandleNotAValueDataTypes();

    /**
     * This method is only allowed to return true, if the {@link ComponentInterface} specifies a loop driver.
     * 
     * @return <code>true</code> if the loop driver supports the discard inner loop runs, otherwise <code>false</code>
     */
    boolean getLoopDriverSupportsDiscard();

    /**
     * @return <code>true</code> if {@link TypedDatum}s of {@link DataType} {@link NotAValueTD} must not pass the component, otherwise
     *         <code>false</code>
     */
    boolean getIsLoopDriver();

    /**
     * @return hash of the documentation folder for the component or an empty string if it doesn't exist.
     */
    String getDocumentationHash();

}
