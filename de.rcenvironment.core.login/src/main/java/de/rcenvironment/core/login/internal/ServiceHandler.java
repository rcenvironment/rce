/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.login.AbstractLogin;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.utils.common.ServiceUtils;

/**
 * Class handling services used this {@link Bundle}. The services are injected then provided by getters. This kind of workaround is needed
 * because the class {@link AbstractLogin} can not get the service injected directly because it is abstract and thus can not be
 * instantiated. But this is a prerequisite for declarative service components.
 * 
 * @author Doreen Seider
 * @author Tobias Menden
 */
public class ServiceHandler {

    private static String bundleSymbolicName;

    private static AuthenticationService nullAuthenticationService =
        ServiceUtils.createFailingServiceProxy(AuthenticationService.class);

    private static DistributedNotificationService nullNotificationService =
        ServiceUtils.createFailingServiceProxy(DistributedNotificationService.class);

    private static ConfigurationService nullConfigurationService =
        ServiceUtils.createFailingServiceProxy(ConfigurationService.class);

    private static AuthenticationService authenticationService = nullAuthenticationService;

    private static DistributedNotificationService notificationService = nullNotificationService;

    private static ConfigurationService configurationService = nullConfigurationService;

    private static final Log LOGGER = LogFactory.getLog(ServiceHandler.class);

    /**
     * Activation method called by OSGi. Sets the bundle symbolic name.
     * 
     * @param context of the Bundle
     */
    public void activate(BundleContext context) {
        bundleSymbolicName = context.getBundle().getSymbolicName();

        // LoginConfiguration loginConfiguration =
        // configurationService.getConfiguration(bundleSymbolicName, LoginConfiguration.class);
        // tries to automatically log in

        new SingleUserAutoLogin().login();
        notificationService.send(AbstractLogin.LOGIN_NOTIFICATION_ID, "Anonymouslogin"); //$NON-NLS-1$
        LOGGER.debug("Using anonymous/default login");
    }

    /**
     * Deactivation method called by OSGi. Unregisters the publisher.
     * 
     * @param context of the Bundle
     */
    public void deactivate(BundleContext context) {
        notificationService.removePublisher(AbstractLogin.LOGIN_NOTIFICATION_ID);
    }

    /**
     * Bind the ConfigurationService of the LoginConfiguration to configurationService.
     * 
     * @param newConfigurationService The {@link ConfigurationService} to bind.
     */
    public void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    /**
     * 
     * Bind the AuthenticationService to authenticationService.
     * 
     * @param newAuthenticationService The {@link AuthenticationService} to bind.
     */
    public void bindAuthenticationService(AuthenticationService newAuthenticationService) {
        authenticationService = newAuthenticationService;
    }

    /**
     * Bind the AuthenticationService to authenticationService.
     * 
     * @param newNotificationService The {@link NotificationService} to bind.
     */
    public void bindNotificationService(DistributedNotificationService newNotificationService) {
        notificationService = newNotificationService;
    }

    /**
     * Unbind the {@link NotificationService}.
     * 
     * @param oldConfigurationService The {@link ConfigurationService} to unbind.
     */
    public void unbindConfigurationService(ConfigurationService oldConfigurationService) {
        configurationService = nullConfigurationService;
    }

    /**
     * Unbind the {@link AuthenticationService}.
     * 
     * @param oldAuthenticationService The {@link AuthenticationService} to unbind.
     */
    public void unbindAuthenticationService(AuthenticationService oldAuthenticationService) {
        authenticationService = nullAuthenticationService;
    }

    /**
     * Unbind the {@link NotificationService}.
     * 
     * @param oldNotificationService The {@link NotificationService} to unbind.
     */
    protected void unbindNotificationService(DistributedNotificationService oldNotificationService) {
        notificationService = nullNotificationService;
    }

    public static String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public static ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public static AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public static DistributedNotificationService getNotificationService() {
        return notificationService;
    }
}
