/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;

/**
 * Type of the data management object.
 * 
 * @author Markus Litz
 * @author Robert Mischke
 */
public enum DMBrowserNodeType {

    /** DM-Object is a chameleon-rce workflow. */
    WorkflowRunInformation,

    /** DM-Object is a chameleon-rce workflow. */
    Workflow,

    /** DM-Object is a disabled chameleon-rce workflow. */
    Workflow_Disabled,

    /** DM-Object is a timeline. */
    Timeline,

    /** DM-Object is a component container. */
    Components,

    /** DM-Object is a chameleon-rce file resource. */
    Resource,

    /** DM-Object is a chameleon-rce component. */
    Component,

    /** DM-Object is a chameleon-rce folder. */
    DMDirectoryReference,

    /** DM-Object is a chameleon-rce versioned file. */
    VersionizedResource,

    /**
     * Node type for the root of a history object group.
     */
    HistoryRoot,

    /**
     * Node type for individual history objects.
     */
    HistoryObject,

    /**
     * A type for nodes that represent a DM reference which in turn represents a file; such a file is expected to have an filename
     * associated via {@link MetaDataKeys#FILENAME}.
     */
    DMFileResource,

    /**
     * Node type for information text nodes.
     */
    InformationText,

    /**
     * Node type for warning text nodes.
     */
    WarningText,

    /**
     * Node type for an empty node.
     */
    Empty,

    /**
     * Node type for placeholder nodes indicating that content is being fetched.
     */
    Loading,

    /**
     * Node type for inputs and outputs of DataType ShortText.
     */
    ShortText,

    /**
     * Node type for inputs and outputs of DataType Boolean.
     */
    Boolean,

    /**
     * Node type for inputs and outputs of DataType Integer.
     */
    Integer,

    /**
     * Node type for inputs and outputs of DataType Float.
     */
    Float,

    /**
     * Node type for inputs and outputs of DataType Vector.
     */
    Vector,

    /**
     * Node type for inputs and outputs of DataType SmallTable.
     */
    SmallTable,

    /**
     * Node type for inputs and outputs of DataType Matrix.
     */
    Matrix,
    
    /**
     * Node type for inputs and outputs of DataType Indefinite.
     */
    Indefinite,

    /**
     * Node type for inputs and outputs of DataType File.
     */
    File,

    /**
     * Node type for inputs and outputs of DataType Directory.
     */
    Directory,

    /**
     * Node type for inputs node.
     */
    Input,

    /**
     * Node type for outputs node.
     */
    Output,

    /**
     * Node type for log folder node.
     */
    LogFolder,

    /**
     * Node type for tool input/output folder node.
     */
    ToolInputOutputFolder,

    /**
     * Node type for intermediate inputs folder node.
     */
    IntermediateInputsFolder,
    
    /**
     * Node type for common text nodes. Content will be opened in editor and must be provided via
     * {@link DMBrowserNode#setFileContentAndName(String, String)}.
     */
    CommonText,
    
    /**
     * Node type for condition folder nodes.
     */
    Custom,

    /**
     * Node type for host information of components.
     */
    ComponentHostInformation,

    /**
     * .... 
     */
    SqlFolder;
}
