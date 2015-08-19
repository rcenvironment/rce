/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.xml;

import java.text.MessageFormat;
import java.util.List;

import org.dom4j.Node;

import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Utility class that provides methods to retrieve elements and values from XML files.
 * 
 * @author Andre Nurzenski
 */
public final class XMLSupport {

    /**
     * Constant for a logger or exception message.
     */
    private static final String ERROR_PARAMETER_NULL = "The parameter \"{0}\" must not be null or empty.";

    /**
     * Constant for a logger or exception message.
     */
    private static final String PARAMETER_ROOT_NODE = "rootNode";

    /**
     * Constant for a logger or exception message.
     */
    private static final String PARAMETER_XPATH_EXPRESSION = "xPathExpression";

    /**
     * 
     * Private constructor, because this is a utility class.
     * 
     */
    private XMLSupport() {

    }

    /**
     * 
     * Retrieves the string representation of a node matching an XPath expression in a dom4j tree.
     * 
     * @param rootNode
     *            The node containing the subtree to search within.
     * @param xPathExpression
     *            The XPath expression.
     * @return the node's value as a string.
     */
    public static String getNodeStringValue(Node rootNode, String xPathExpression) {
        
        Assertions.isDefined(rootNode, MessageFormat.format(ERROR_PARAMETER_NULL, PARAMETER_ROOT_NODE));
        Assertions.isDefined(xPathExpression, MessageFormat.format(ERROR_PARAMETER_NULL, PARAMETER_XPATH_EXPRESSION));

        // Retrieve a node matching the XPath expression and return its value as a string
        // representation if it's not null.
        String nodeValue = "";
        Node node = rootNode.selectSingleNode(xPathExpression);
        if (node != null) {
            nodeValue = node.getStringValue();
        }

        return nodeValue;
    }

    /**
     * 
     * Retrieves a node matching an XPath expression in a dom4j tree.
     * 
     * @param rootNode
     *            The node containing the subtree to search within.
     * @param xPathExpression
     *            The XPath expression.
     * @return the node described by the XPath expression or <code>null</code> if it does not
     *         exists.
     */
    public static Node selectNode(Node rootNode, String xPathExpression) {
        
        Assertions.isDefined(rootNode, MessageFormat.format(ERROR_PARAMETER_NULL, PARAMETER_ROOT_NODE));
        Assertions.isDefined(xPathExpression, MessageFormat.format(ERROR_PARAMETER_NULL, PARAMETER_XPATH_EXPRESSION));

        // Retrieve a node matching the XPath expression and return it.
        return rootNode.selectSingleNode(xPathExpression);
    }

    /**
     * 
     * Retrieves a list of nodes matching an XPath expression in a dom4j tree.
     * 
     * @param rootNode
     *            The node containing the subtree to search within.
     * @param xPathExpression
     *            The XPath expression.
     * @return a list containing the matching nodes or an empty list if the nodes do not exist.
     */
    public static List<Node> selectNodes(Node rootNode, String xPathExpression) {
        
        Assertions.isDefined(rootNode, MessageFormat.format(ERROR_PARAMETER_NULL, PARAMETER_ROOT_NODE));
        Assertions.isDefined(xPathExpression, MessageFormat.format(ERROR_PARAMETER_NULL, PARAMETER_XPATH_EXPRESSION));

        // Retrieve a list of nodes which match the XPath expression and return it.
        return rootNode.selectNodes(xPathExpression);
    }

}
