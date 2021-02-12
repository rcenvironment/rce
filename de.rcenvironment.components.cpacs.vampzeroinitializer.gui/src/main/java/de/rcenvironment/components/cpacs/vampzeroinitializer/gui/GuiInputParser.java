/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import static javax.xml.xpath.XPathConstants.NODESET;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Component;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Discipline;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Parameter;
import de.rcenvironment.core.utils.common.xml.XMLException;
import de.rcenvironment.core.utils.common.xml.api.XMLSupportService;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Parse the XML that describes the GUI and the parameters.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Brigitte Boden
 */
public final class GuiInputParser {

    private static final String NAME = "name";

    /**
     * The xpath object.
     */
    private XPath xpath;
    
    /**
     * The XMLSupport service.
     */
    private XMLSupportService xmlSupport;
    
    /**
     * Constructor.
     */
    public GuiInputParser() {
        xpath = XPathFactory.newInstance().newXPath();
        xmlSupport = ServiceRegistry.createAccessFor(this).getService(XMLSupportService.class);
    }

    /**
     * Parse the given file and return the model objects.
     * 
     * @param configuration The file to read
     * @return The components
     * @throws XPathExpressionException For any parsing error
     * @throws XMLException Error in XML handling
     */
    public List<Component> parse(final String configuration) throws XPathExpressionException, XMLException {
        final Document doc;
        if (configuration == null) {
            return new ArrayList<Component>();
        }
        doc = xmlSupport.readXMLFromString(configuration);
        return parse(doc, "/cpacs/toolspecific/vampZero/components/component");
    }

    /**
     * Parse the input document in vampzero format.
     * 
     * @param stream The file to read
     * @return The components
     * @throws XPathExpressionException For any parsing error
     * @throws XMLException Error in XML handling
     */
    public List<Component> parse(final InputStream stream) throws XPathExpressionException, XMLException {
        final Document doc;
        if (stream == null) {
            return new ArrayList<Component>();
        }
        doc = xmlSupport.readXMLFromStream(stream);
        return parse(doc, "/zeroGuiIn/component");
    }

    /**
     * Parse the input document in Cpacs or vampzero format (depending on prefix).
     * 
     * @param doc
     * @param xpathPrefix
     * @return a component list
     * @throws XPathExpressionException
     */
    protected List<Component> parse(final Document doc, final String xpathPrefix) throws XPathExpressionException {
        final List<Component> componentList = new ArrayList<Component>();
        final List<Discipline> disciplineList = new ArrayList<Discipline>();
        final List<Parameter> parameterList = new ArrayList<Parameter>();
        final NodeList components = (NodeList) xpath.evaluate(xpathPrefix, doc.getDocumentElement(), NODESET);
        int cn = components.getLength();
        for (int c = 0; c < cn; c++) {
            final Node component = components.item(c);
            final String componentName = getNodeTextValue(component, NAME);
            final NodeList disciplines = (NodeList) xpath.evaluate("disciplines/discipline", component, NODESET);
            disciplineList.clear();
            int dn = disciplines.getLength();
            for (int d = 0; d < dn; d++) {
                final Node discipline = disciplines.item(d);
                final String disciplineName = getNodeTextValue(discipline, NAME);
                final NodeList parameters = (NodeList) xpath.evaluate("parameters/parameter", discipline, NODESET);
                parameterList.clear();
                int pn = parameters.getLength();
                for (int p = 0; p < pn; p++) {
                    final Node parameter = parameters.item(p);
                    final String parameterName = getNodeTextValue(parameter, NAME);
                    final String parameterDescription = getNodeTextValue(parameter, "description");
                    final String parameterValue = getNodeTextValue(parameter, "value");
                    final String parameterFactor = getNodeTextValue(parameter, "factor");
                    final Parameter newParameter = ((Parameter) new Parameter().setName(parameterName))
                        .setDescription(parameterDescription)
                        .setValue(parameterValue)
                        .setFactor(parameterFactor);
                    parameterList.add(newParameter);
                }
                Collections.sort(parameterList);
                final Discipline newDiscipline = ((Discipline) new Discipline().setName(disciplineName)).setParameters(parameterList);
                disciplineList.add(newDiscipline);
            }
            Collections.sort(disciplineList);
            final Component newComponent = ((Component) new Component().setName(componentName)).setDisciplines(disciplineList);
            componentList.add(newComponent);
        }
        Collections.sort(componentList);
        return componentList;
    }

    /**
     * Small XML helper.
     * 
     * @param node The node
     * @return The text value
     * @throws XPathExpressionException
     */
    private static String getNodeTextValue(final Node parentNode, final String elementName) throws XPathExpressionException {
        final NodeList nodes = parentNode.getChildNodes();
        int n = nodes.getLength();
        for (int i = 0; i < n; i++) {
            final Node node = nodes.item(i);
            if (node.getNodeName().equals(elementName)) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }

}
