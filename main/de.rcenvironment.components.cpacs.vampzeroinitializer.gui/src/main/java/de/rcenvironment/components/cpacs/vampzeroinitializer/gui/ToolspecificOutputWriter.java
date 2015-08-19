/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Component;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Discipline;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Parameter;
import de.rcenvironment.core.utils.incubator.XMLHelper;

/**
 * Write the XML that describes the toolspecific VampZero input.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public final class ToolspecificOutputWriter {

    private static final String NAME = "name";
    private static XMLHelper xml = new XMLHelper();

    private ToolspecificOutputWriter() {
        // private constructor.
    }
    
    static String createOutput(final List<Component> components) {
        final Document doc = xml.createDocument();
        final Node componentsNode = xml.createElementTree(doc, "/cpacs/toolspecific/vampZero/components");
        for (final Component component : components) {
            final Node disciplinesNode = createNodes(doc, componentsNode, "component", "disciplines");
            createNodeAndValue(doc, disciplinesNode.getParentNode(), NAME, component.getName(), true);
            for (final Discipline discipline : component.getDisciplines()) {
                final Node parametersNode = createNodes(doc, disciplinesNode, "discipline", "parameters");
                createNodeAndValue(doc, parametersNode.getParentNode(), NAME, discipline.getName(), true);
                for (final Parameter parameter : discipline.getParameters()) {
                    final Node parameterNode = createNodes(doc, parametersNode, "parameter");
                    createNodeAndValue(doc, parameterNode, NAME, parameter.getName());
                    createNodeAndValue(doc, parameterNode, "description", parameter.getDescription());
                    createNodeAndValue(doc, parameterNode, "value", parameter.getValue());
                    createNodeAndValue(doc, parameterNode, "factor", parameter.getFactor());
                }
            }
        }
        return xml.writeXMLToString(doc);
    }

    private static Node createNodes(final Document doc, final Node parent, final String... names) {
        Node node = null;
        Node father = parent;
        for (final String name : names) {
            node = doc.createElement(name);
            doc.importNode(node, false);
            father.appendChild(node);
            father = node;
        }
        return node;
    }

    private static void createNodeAndValue(final Document doc, final Node parent, final String name, final String value,
        final boolean... insertBefore) {
        final Node node = doc.createElement(name);
        doc.importNode(node, false);
        if ((insertBefore.length) > 0 && insertBefore[0]) {
            parent.insertBefore(node, parent.getFirstChild());
        } else {
            parent.appendChild(node);
        }
        node.setTextContent(value);
    }

}
