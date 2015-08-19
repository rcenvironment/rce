/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.authorization.AuthorizationService;
import de.rcenvironment.core.authorization.AuthorizationStore;
import de.rcenvironment.core.authorization.rbac.Permission;
import de.rcenvironment.core.authorization.rbac.Role;
import de.rcenvironment.core.authorization.rbac.Subject;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Implementation of the <code>AuthorizationService</code> interface.
 * 
 * @author Doreen Seider
 */
public final class AuthorizationServiceImpl implements AuthorizationService {

    /**
     * Error message.
     */
    private static final String ERROR_SERVICE_NOT_REGISTERED = "A service providing the desired"
        + " authorization store \"{0}\" is not registered.";

    /**
     * Error message.
     */
    private static final String ERROR_BUNDLE_NOT_INSTALLED = "A bundle providing the desired"
        + " authorization store \"{0}\" is not installed.";

    /**
     * Logger for this class.
     */
    private static final Log LOGGER = LogFactory.getLog(AuthorizationStore.class);

    /**
     * Constant.
     */
    private static final String SUBJECT_ID = "subjectID";

    /**
     * Constant.
     */
    private static final String ERROR_PARAMETERS_NULL = "The parameter \"{0}\" must not be null.";

    /**
     * The authorization store to use.
     */
    private static AuthorizationStore myStore = null;

    /**
     * Configuration of this bundle.
     */
    private AuthorizationConfiguration myConfiguration;

    private ConfigurationService configurationService;

    private BundleContext bundleContext;

    protected void activate(BundleContext context) {
        bundleContext = context;
        // note: disabled old configuration loading for 6.0.0 as it is not being used anyway
        // myConfiguration = configurationService.getConfiguration(context.getBundle().getSymbolicName(), AuthorizationConfiguration.class);
        // TODO load default values until reworked or removed
        myConfiguration = new AuthorizationConfiguration();
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    @Override
    public Permission getPermission(String permissionID) {
        myStore = getAuthorizationStore();
        Assertions.isDefined(permissionID, MessageFormat.format(ERROR_PARAMETERS_NULL, "permissionID"));

        final Permission permission = myStore.lookupPermission(permissionID);
        return permission;
    }

    @Override
    public Set<Permission> getPermissions(String subjectID) {
        myStore = getAuthorizationStore();
        Assertions.isDefined(subjectID, MessageFormat.format(ERROR_PARAMETERS_NULL, SUBJECT_ID));

        Set<Permission> permissions = new HashSet<Permission>();

        Set<Role> roles = myStore.lookupSubject(subjectID).getRoles();
        for (Role role : roles) {
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission);
            }
        }

        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public Role getRole(String roleID) {
        myStore = getAuthorizationStore();
        Assertions.isDefined(roleID, MessageFormat.format(ERROR_PARAMETERS_NULL, "roleID"));

        final Role role = myStore.lookupRole(roleID);
        return role;
    }

    @Override
    public Set<Role> getRoles(String subjectID) {
        myStore = getAuthorizationStore();
        Assertions.isDefined(subjectID, MessageFormat.format(ERROR_PARAMETERS_NULL, SUBJECT_ID));

        Set<Role> roles = myStore.lookupSubject(subjectID).getRoles();
        return Collections.unmodifiableSet(roles);

    }

    @Override
    public Subject getSubject(String subjectID) {
        myStore = getAuthorizationStore();
        Assertions.isDefined(subjectID, MessageFormat.format(ERROR_PARAMETERS_NULL, SUBJECT_ID));

        final Subject subject = myStore.lookupSubject(subjectID);
        return subject;
    }

    @Override
    public boolean hasPermission(String subjectID, Permission permission) {
        myStore = getAuthorizationStore();
        Assertions.isDefined(subjectID, MessageFormat.format(ERROR_PARAMETERS_NULL, SUBJECT_ID));
        Assertions.isDefined(permission, MessageFormat.format(ERROR_PARAMETERS_NULL, "permission"));

        boolean hasPermission = false;

        Set<Role> roles = myStore.lookupSubject(subjectID).getRoles();
        for (Role role : roles) {
            if (role.hasPermission(permission)) {
                hasPermission = true;
                break;
            }
        }

        return hasPermission;
    }

    @Override
    public boolean hasRole(String subjectID, Role role) {
        myStore = getAuthorizationStore();
        Assertions.isDefined(subjectID, MessageFormat.format(ERROR_PARAMETERS_NULL, SUBJECT_ID));
        Assertions.isDefined(role, MessageFormat.format(ERROR_PARAMETERS_NULL, "role"));

        return myStore.lookupSubject(subjectID).hasRole(role);
    }

    /**
     * 
     * Returns a <code>AuthorizationStore</code> instance.
     * 
     * @return the <code>AuthorizationStore</code> instance.
     */
    private AuthorizationStore getAuthorizationStore() {

        String store = myConfiguration.getStore();

        // try to start the authorization store bundle
        Bundle[] bundles = bundleContext.getBundles();

        if (bundles == null) {
            throw new RuntimeException(MessageFormat.format(ERROR_BUNDLE_NOT_INSTALLED, store));
        } else {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(store)) {
                    try {
                        bundle.start();
                    } catch (BundleException e) {
                        throw new RuntimeException(MessageFormat.format(ERROR_BUNDLE_NOT_INSTALLED, store), e);
                    }
                }
            }
        }

        // try to get the authorization store service
        String protocolFilter = "(" + AuthorizationStore.STORE + "=" + store + ")";

        ServiceReference<?>[] storeReferences = null;
        try {
            storeReferences = bundleContext.getAllServiceReferences(AuthorizationStore.class.getName(), protocolFilter);
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Failed to get an authorization store service. Invalid protocol filter syntax.");
        }

        if (storeReferences == null || storeReferences.length < 1) {
            throw new RuntimeException(MessageFormat.format(ERROR_SERVICE_NOT_REGISTERED, store));
        }

        return (AuthorizationStore) bundleContext.getService(storeReferences[0]);
    }
}
