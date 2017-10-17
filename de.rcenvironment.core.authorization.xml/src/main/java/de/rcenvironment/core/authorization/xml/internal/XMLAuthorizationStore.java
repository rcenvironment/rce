/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.xml.internal;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authorization.AuthorizationStore;
import de.rcenvironment.core.authorization.AuthorizationStoreException;
import de.rcenvironment.core.authorization.rbac.Permission;
import de.rcenvironment.core.authorization.rbac.RBACObject;
import de.rcenvironment.core.authorization.rbac.Role;
import de.rcenvironment.core.authorization.rbac.Subject;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.Assertions;
import de.rcenvironment.core.utils.incubator.xml.XMLIOSupport;
import de.rcenvironment.core.utils.incubator.xml.XMLSupport;

/**
 * 
 * Provides an implementation of {@link AuthorizationStore} by using an XML document as store.
 * 
 * @author Doreen Seider
 */
public class XMLAuthorizationStore implements AuthorizationStore {

    /**
     * Constant for a logger or exception message.
     */
    private static final String ERROR_PARSING_DOCUMENT = "Error while parsing the XML document.";

    /**
     * Constant for a logger or exception message.
     */
    private static final String ERROR_ARGUMENT_IS_NULL = "Argument for parameter %s is null.";

    /**
     * XPath query to select permission elements.
     */
    private static final String XPATH_QUERY_SELECT_DESCRIPTION = "./description";

    /**
     * XPath query to select role elements.
     */
    private static final String XPATH_QUERY_SELECT_ROLES = "child::role";

    /**
     * XPath query to select permission elements.
     */
    private static final String XPATH_QUERY_SELECT_PERMISSIONS = "child::permission";

    /**
     * XPath query to select an users.
     */
    private static final String XPATH_QUERY_SELECT_SUBJECT = "//subject[@id='%s']";

    /**
     * XPath query to select a group.
     */
    private static final String XPATH_QUERY_SELECT_ROLE = "//role[@id='%s']";

    /**
     * XPath query to select an extension.
     */
    private static final String XPATH_QUERY_SELECT_ALL_ROLES = "//roles/*";

    /**
     * XPath query to select an extension.
     */
    private static final String XPATH_QUERY_SELECT_PERMISSION = "//permission[@id='%s']";

    /**
     * XPath query to select an extension.
     */
    private static final String XPATH_QUERY_SELECT_ALL_PERMISSIONS = "//permissions/*";

    /**
     * XPath query to select an extension.
     */
    private static final String XPATH_QUERY_SELECT_ID = "attribute::id";

    /**
     * Logger for this class.
     */
    private static final Log LOGGER = LogFactory.getLog(XMLAuthorizationStore.class);

    /**
     * Configuration of this bundle.
     */
    private XMLAuthorizationConfiguration myConfiguration;

    /**
     * The internal representation of the XML file.
     */
    private Document myXMLDocument = null;

    private ConfigurationService configurationService;

    private String bundleSymbolicName;

    protected void activate(BundleContext context) {
        bundleSymbolicName = context.getBundle().getSymbolicName();
        // note: disabled old configuration loading for 6.0.0 as it is not being used anyway
        // myConfiguration = configurationService.getConfiguration(context.getBundle().getSymbolicName(),
        // XMLAuthorizationConfiguration.class);
        // TODO using default values until reworked or removed
        myConfiguration = new XMLAuthorizationConfiguration();
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        this.configurationService = newConfigurationService;
    }

    @Override
    public void initialize() throws AuthorizationStoreException {
        // Try to load the XML file and store it as a 'Document' representation.
        try {
            String absPath = configurationService.resolveBundleConfigurationPath(bundleSymbolicName, myConfiguration.getXmlFile());
            if (absPath != null && new File(absPath).exists()) {
                myXMLDocument = XMLIOSupport.readXML(absPath);
            } else {
                myXMLDocument = XMLIOSupport.readXML(getClass().getResourceAsStream("/placeholder.xml"));
                LOGGER.info("No authorization store is given under: " + absPath + ". Using an empty one.");
            }
        } catch (DocumentException e) {
            throw new AuthorizationStoreException(ERROR_PARSING_DOCUMENT, e);
        }
        LOGGER.info("XML authorization store initialized.");
    }

    @Override
    public Permission lookupPermission(String permissionID) {

        Assertions.isDefined(permissionID, StringUtils.format(ERROR_ARGUMENT_IS_NULL, "permissionID"));

        try {
            LOGGER.debug("Retrieving entry for permission: " + permissionID);
            return getPermission(permissionID);

        } catch (DocumentException e) {
            return null;
        }
    }

    @Override
    public Role lookupRole(String roleID) {

        Assertions.isDefined(roleID, StringUtils.format(ERROR_ARGUMENT_IS_NULL, "roleID"));

        try {
            LOGGER.debug("Retrieving entry for role: " + roleID);
            return getRole(roleID);

        } catch (DocumentException e) {
            return null;
        }
    }

    @Override
    public Subject lookupSubject(String subjectID) {

        Assertions.isDefined(subjectID, StringUtils.format(ERROR_ARGUMENT_IS_NULL, "subjectID"));

        try {
            LOGGER.debug("Retrieving entry for subject: " + subjectID);
            return getSubject(subjectID);

        } catch (DocumentException e) {
            return null;
        }

    }

    /**
     * 
     * Creates a new {@link Subject} object from the underlying entry in the XML document.
     * 
     * @param subjectID The ID of the specified {@link Subject}.
     * @return the created {@link Subject} object.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Subject getSubject(String subjectID) throws DocumentException {

        Node subjectNode = XMLSupport.selectNode(myXMLDocument, StringUtils.format(XPATH_QUERY_SELECT_SUBJECT, subjectID));
        return getSubject(subjectNode);
    }

    /**
     * 
     * Creates a new {@link Role} object from the underlying entry in the XML document.
     * 
     * @param roleID The ID of the specified {@link Role}.
     * @return the created {@link Role} object.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Role getRole(String roleID) throws DocumentException {

        Node roleNode = XMLSupport.selectNode(myXMLDocument, StringUtils.format(XPATH_QUERY_SELECT_ROLE, roleID));
        return getRole(roleNode);
    }

    /**
     * 
     * Creates a new {@link Subject} object from the underlying entry in the XML document.
     * 
     * @param permissionID The ID of the specified {@link Permission}.
     * @return the created {@link Permission} object.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Permission getPermission(String permissionID) throws DocumentException {
        Node permissionNode = XMLSupport.selectNode(myXMLDocument, StringUtils.format(XPATH_QUERY_SELECT_PERMISSION, permissionID));

        return getPermission(permissionNode);
    }

    /**
     * 
     * Creates a new {@link Subject} object from the underlying entry in the XML document.
     * 
     * @param subjectNode The node describing the {@link Subject} entry.
     * @return the created {@link Subject} object.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Subject getSubject(Node subjectNode) throws DocumentException {

        String id = getID(subjectNode);
        String description = getDescription(subjectNode);

        List<Node> roleNodes = XMLSupport.selectNodes(subjectNode, XPATH_QUERY_SELECT_ROLES);
        Set<Role> roles = new HashSet<Role>();

        // Add all defined roles to this subject
        for (Node roleNode : roleNodes) {

            Set<String> roleIDs = getMatchingRoleIDs(roleNode.getText());

            for (String roleID : roleIDs) {
                roles.add(getRole(roleID));
            }
        }

        if (description.isEmpty()) {
            return new Subject(id, roles);
        } else {
            return new Subject(id, description, roles);
        }
    }

    /**
     * 
     * Creates a new {@link Role} object from the underlying entry in the XML document.
     * 
     * @param roleNode The node describing the {@link Role} entry.
     * @return the created {@link Role} object.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Role getRole(Node roleNode) throws DocumentException {

        // Create a new role object using the information provided by the underlying XML file.
        String id = getID(roleNode);
        String description = getDescription(roleNode);

        List<Node> permissionNodes = XMLSupport.selectNodes(roleNode, XPATH_QUERY_SELECT_PERMISSIONS);
        Set<Permission> permissions = new HashSet<Permission>();

        // Add all defined permissions to this role.
        for (Node permissionNode : permissionNodes) {

            Set<String> permissionIDs = getMatchingPermissionIDs(permissionNode.getText());
            for (String permissionID : permissionIDs) {
                permissions.add(getPermission(permissionID));
            }

        }

        if (description.isEmpty()) {
            return new Role(id, permissions);
        } else {
            return new Role(id, description, permissions);
        }
    }

    /**
     * 
     * Creates a new {@link Permission} object from the underlying entry in the XML document.
     * 
     * @param permissionNode The {@link Node} describing the permission entry.
     * @return the created {@link Permission} object.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Permission getPermission(Node permissionNode) throws DocumentException {
        String id = getID(permissionNode);
        String description = getDescription(permissionNode);

        if (description.isEmpty()) {
            return new Permission(id);
        } else {
            return new Permission(id, description);
        }
    }

    /**
     * 
     * Returns the description of an {@link RBACObject} from the underlying entry in the XML document.
     * 
     * @param node The node of the authorization entry.
     * @return the description of the authorization object or an empty.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private String getDescription(Node node) throws DocumentException {

        Node descriptionNode = XMLSupport.selectNode(node, XPATH_QUERY_SELECT_DESCRIPTION);
        if (descriptionNode != null) {
            return descriptionNode.getText();
        } else {
            return "";
        }
    }

    /**
     * 
     * Returns the ID of an <code>RBACObject</code> from the underlying entry in the XML document.
     * 
     * @param node The node of the authorization entry.
     * @return the ID of the authorization object.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private String getID(Node node) throws DocumentException {

        Node idNode = XMLSupport.selectNode(node, XPATH_QUERY_SELECT_ID);
        if (idNode != null) {
            return idNode.getText();
        } else {
            throw new DocumentException(ERROR_PARSING_DOCUMENT);
        }
    }

    /**
     * 
     * Returns all role IDs of the underlying XML document matching the given regular expression.
     * 
     * @param regExpressedRoleID The regular expression.
     * @return a set of matching role IDs.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Set<String> getMatchingRoleIDs(String regExpressedRoleID) throws DocumentException {

        Set<String> permissionIDs = new HashSet<String>();

        List<Node> roleNodes = XMLSupport.selectNodes(myXMLDocument, XPATH_QUERY_SELECT_ALL_ROLES);
        for (Node roleNode : roleNodes) {
            String id = getID(roleNode);
            if (id.matches(regExpressedRoleID)) {
                permissionIDs.add(id);
            }
        }

        return permissionIDs;
    }

    /**
     * 
     * Returns all permission IDs of the underlying XML document matching the given regular expression.
     * 
     * @param regExpressedPermissionID The regular expression.
     * @return a set of matching permission IDs.
     * @throws DocumentException if the specified arguments are illegal.
     */
    private Set<String> getMatchingPermissionIDs(String regExpressedPermissionID) throws DocumentException {

        Set<String> permissionIDs = new HashSet<String>();

        List<Node> permissionNodes = XMLSupport.selectNodes(myXMLDocument, XPATH_QUERY_SELECT_ALL_PERMISSIONS);
        for (Node permissionNode : permissionNodes) {
            String id = getID(permissionNode);
            if (id.matches(regExpressedPermissionID)) {
                permissionIDs.add(id);
            }
        }

        return permissionIDs;
    }

}
