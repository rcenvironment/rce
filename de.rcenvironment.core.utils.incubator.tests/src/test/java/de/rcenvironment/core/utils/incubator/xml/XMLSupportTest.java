/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.xml;

import java.util.List;

import junit.framework.TestCase;
import org.junit.Ignore;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.InvalidXPathException;
import org.dom4j.Node;


/**
 * 
 * Test cases for the class <code>XMLSupport</code>.
 * 
 * @author Andre Nurzenski
 */
//Ignore tests because the corresponding code is not used in RCE anymore
@Ignore 
public class XMLSupportTest extends TestCase {

    /**
     * An XPath expression for the tests.
     */
    private static final String TEST_XPATH_EXPRESSION_1 = "///blub[*]";

    /**
     * An XPath expression for the tests.
     */
    private static final String TEST_XPATH_EXPRESSION_2 = "/blub";

    /**
     * An XPath expression for the tests.
     */
    private static final String TEST_XPATH_EXPRESSION_3 = "//subject";

    /**
     * An XPath expression for the tests.
     */
    private static final String TEST_XPATH_EXPRESSION_4 = "/authorization";

    /**
     * An XPath expression for the tests.
     */
    private static final String TEST_XPATH_EXPRESSION_5 = "/authorization/subjects/subject"
        + "[@id='CN=Rainer Tester,OU=SC,O=DLR,L=Cologne,ST=NRW,C=DE']";

    /**
     * An XPath expression for the tests.
     */
    private static final String TEST_XPATH_EXPRESSION_6 = "//role[@id='org.cmt-net']";

    /**
     * Message that indicates that a test has failed because no exception was thrown.
     */
    private static final String EXCEPTION_MESSAGE = "Should raise an IllegalArgumentException.";

    /**
     * XML document for the tests.
     */
    private Document myDocument = null;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myDocument = XMLIOSupport.readXML(getClass().getResourceAsStream("/authorization.xml"));
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myDocument = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * 
     * Test if the string value of a node can be retrieved.
     * 
     * @throws DocumentException
     *             if the specified arguments are illegal.
     * 
     */
    public void testGetNodeStringValueForSuccess() throws DocumentException {
        XMLSupport.getNodeStringValue(myDocument, TEST_XPATH_EXPRESSION_3);
    }

    /**
     * 
     * Test if a node can be retrieved.
     * 
     * @throws DocumentException
     *             if the specified arguments are illegal.
     * 
     */
    public void testSelectNodeForSuccess() throws DocumentException {
        XMLSupport.selectNode(myDocument, TEST_XPATH_EXPRESSION_4);
    }

    /**
     * 
     * Test if several nodes can be retrieved.
     * 
     * @throws DocumentException
     *             if the specified arguments are illegal.
     * 
     */
    public void testSelectNodesForSuccess() throws DocumentException {
        XMLSupport.selectNodes(myDocument, TEST_XPATH_EXPRESSION_3);
    }

    /*
     * #################### Test for failure ####################
     */

    /**
     * 
     * Test if an IllegalArgumentException is thrown.
     * 
     * @throws DocumentException
     *             if an exception occurs.
     * 
     */
    public void testGetNodeStringValueForFailure() throws DocumentException {
        try {
            XMLSupport.getNodeStringValue(myDocument, TEST_XPATH_EXPRESSION_1);
            fail(EXCEPTION_MESSAGE);
        } catch (InvalidXPathException e) {
            assertTrue(true);
        }

        try {
            XMLSupport.getNodeStringValue(myDocument, "");
            fail(EXCEPTION_MESSAGE);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            XMLSupport.getNodeStringValue(null, TEST_XPATH_EXPRESSION_2);
            fail(EXCEPTION_MESSAGE);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test if an IllegalArgumentException is thrown.
     * 
     * @throws DocumentException
     *             if an exception occurs.
     * 
     */
    public void testSelectNodeForFailure() throws DocumentException {
        try {
            XMLSupport.selectNode(myDocument, TEST_XPATH_EXPRESSION_1);
            fail(EXCEPTION_MESSAGE);
        } catch (InvalidXPathException e) {
            assertTrue(true);
        }

        try {
            XMLSupport.selectNode(myDocument, "");
            fail(EXCEPTION_MESSAGE);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            XMLSupport.selectNode(null, TEST_XPATH_EXPRESSION_2);
            fail(EXCEPTION_MESSAGE);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test if an IllegalArgumentException is thrown.
     * 
     * @throws DocumentException
     *             if an exception occurs.
     * 
     */
    public void testSelectNodesForFailure() throws DocumentException {
        try {
            XMLSupport.selectNodes(myDocument, TEST_XPATH_EXPRESSION_1);
            fail(EXCEPTION_MESSAGE);
        } catch (InvalidXPathException e) {
            assertTrue(true);
        }

        try {
            XMLSupport.selectNodes(myDocument, "");
            fail(EXCEPTION_MESSAGE);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            XMLSupport.selectNodes(null, TEST_XPATH_EXPRESSION_2);
            fail(EXCEPTION_MESSAGE);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /*
     * #################### Test for sanity ####################
     */

    /**
     * 
     * Test if the string value of a node can be retrieved.
     * 
     * @throws DocumentException
     *             if the specified arguments are illegal.
     * 
     */
    public void testGetNodeStringValueForSanity() throws DocumentException {
        String nodeValue = XMLSupport.getNodeStringValue(myDocument, TEST_XPATH_EXPRESSION_6);
        assertNotNull(nodeValue);
        assertEquals("de\\.rcenvironment\\.ship:enter", nodeValue);
    }

    /**
     * 
     * Test if a node can be retrieved.
     * 
     * @throws DocumentException
     *             if the specified arguments are illegal.
     * 
     */
    public void testSelectNodeForSanity() throws DocumentException {
        Node node = XMLSupport.selectNode(myDocument, TEST_XPATH_EXPRESSION_6);

        assertNotNull(node);
        assertTrue(node.hasContent());
    }

    /**
     * 
     * Test if several nodes can be retrieved.
     * 
     * @throws DocumentException
     *             if the specified arguments are illegal.
     * 
     */
    public void testSelectNodesForSanity() throws DocumentException {
        List<Node> list = XMLSupport.selectNodes(myDocument, TEST_XPATH_EXPRESSION_3);

        assertFalse(list.isEmpty());
        assertEquals(10, list.size());
    }

}
