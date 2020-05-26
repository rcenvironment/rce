/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.eclipse.swt.graphics.Image;

/**
 * An extension interface for builders that generate the GUI subtrees of history data items.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface ComponentHistoryDataItemSubtreeBuilder {

    /**
     *
     * @return identifier (as returned by ComponentHistoryDataItem#getIdentifier) of all
     *         history data items this builder implementation can handle. Regular expressions are allowed.
     */
    String[] getSupportedHistoryDataItemIdentifier();

    /**
     * This interface method delegates object deserialization back to the builder implementation.
     * This causes deserialization to take place within the classloader context of the builder
     * implementation, which has access to the history object class definitions. Without this, the
     * calling UI bundle would require a "Dynamic-PackageImport: *", which is undesirable.
     * 
     * @param ois the {@link ObjectInputStream} to read from
     * @return the deserialized history object
     * @throws IOException if stream reading fails
     * @throws ClassNotFoundException if the read object could not be instantiated
     */
    Serializable deserializeHistoryDataItem(ObjectInputStream ois) throws IOException, ClassNotFoundException;

    /**
     * Generate the initial subtree elements for the given history object. Currently, no incremental
     * building is supported, so this method must create the whole subtree at once; this may change
     * in the future.
     * 
     * @param historyDataItem the history object
     * @param parent the parent node to construct the subtree under
     */
    void buildInitialHistoryDataItemSubtree(Serializable historyDataItem, DMBrowserNode parent);
    
    /**
     * @param historyDataItemIdentifier of the history data item
     * @return icon of the component related to the history data item with the given identifier. If <code>null</code> default image is taken
     */
    Image getComponentIcon(String historyDataItemIdentifier);

}
