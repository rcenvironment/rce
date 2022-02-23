/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.EndpointContentProvider.Endpoint;


/**
 * The TreeViewer to display components, one additional method to find an item by path.
 *
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class EndpointTreeViewer extends TreeViewer {

    /**
     * Constructor.
     * 
     * @param parent See parent.
     * @param style See parent.
     */
    public EndpointTreeViewer(Composite parent, int style) {
        super(parent, style);
        
        addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                TreeViewer viewer = (TreeViewer) event.getViewer();
                IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
                Object selectedNode = thisSelection.getFirstElement();
                viewer.setExpandedState(selectedNode, !viewer.getExpandedState(selectedNode));
            }
        });
    }

    /**
     * Locates the TreeItem associated with a given Endpoint.
     * 
     * @param node The parent WorkflowNode.
     * @param name The Endpoint name.
     * @return The associated TreeItem.
     */
    public TreeItem findEndpoint(WorkflowNode node, String name) {
        TreeItem foundItem = null;

        for (TreeItem item: this.getTree().getItems()) {
            if (item.getData().equals(node)) {
                foundItem = item;
                if (item.getExpanded()) {
                    for (TreeItem child: item.getItems()) {
                        TreeItem endpoint = findEndpoint(child, name);
                        if (endpoint != null) {
                            foundItem = endpoint;
                            break;
                        }
                    }
                }
            }
        }

        return foundItem;
    }
    
    private TreeItem findEndpoint(TreeItem item, String name) {
        
        TreeItem matchingItem = null;
        
        if (item.getData() instanceof Endpoint) {
            if (((Endpoint) item.getData()).getName().equals(name)) {
                return item;
            }
        }
        
        return matchingItem;

    }
}
