/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common.internal;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;

import de.rcenvironment.components.sql.common.JDBCProfile;
import de.rcenvironment.components.sql.common.JDBCService;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default implementation of {@link JDBCService}.
 * 
 * @author Christian Weiss
 */
public class JDBCServiceImpl implements JDBCService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^.*(\\$\\{(.*)\\}).*$");

    private final Map<JDBCProfile, ClassLoader> classLoaders = new HashMap<JDBCProfile, ClassLoader>();

    private final Set<JDBCProfile> loadedProfiles = new HashSet<JDBCProfile>();

    private ConfigurationService configurationService;

    private JDBCConfiguration configuration;

    protected void bindConfigurationService(final ConfigurationService newConfigurationService) {
        this.configurationService = newConfigurationService;
    }

    protected void activate(BundleContext context) {
        ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("componentSettings/de.rcenvironment.sql");
        configuration = new JDBCConfiguration(configurationSegment);
        validateProfiles();
    }

    private void validateProfiles() {
        final Set<String> labels = new HashSet<String>();
        for (final JDBCProfile profile : getProfiles()) {
            if (labels.contains(profile.getLabel())) {
                throw new RuntimeException(
                    StringUtils.format("Duplicate sql connection label '%s'.", profile.getLabel()));
            }
        }
    }

    @Override
    public List<JDBCProfile> getProfiles() {
        return Collections.unmodifiableList(configuration.getProfiles());
    }

    @Override
    public JDBCProfile getProfileByLabel(String label) {
        JDBCProfile result = null;
        for (final JDBCProfile profile : getProfiles()) {
            if (profile.getLabel().equals(label)) {
                result = profile;
            }
        }
        return result;
    }

    @Override
    public Connection getConnection(final JDBCProfile profile) throws SQLException {
        loadDriver(profile);
        String url = profile.getJdbc().getUrl();
        url = replace(url, profile);
        Connection connection;
        try {
            final String user = profile.getUser();
            String password = profile.getPassword();
            if (password.isEmpty()) {
                password = null;
            }
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new SQLException("Connection could not be established / retrieved.", e.toString());
        }
        return connection;
    }

    protected String replace(final String string, final Object... objects) {
        String result = string;
        // FIXME - use static object
        do {
            final Matcher matcher = VARIABLE_PATTERN.matcher(result);
            if (matcher.matches()) {
                final String variablePlaceholder = matcher.group(1).replaceAll("([\\$\\{\\}\\[\\]])", "\\\\$0");
                final String variableName = matcher.group(2);
                final String variableValue = getVariableValue(variableName, objects);
                result = result.replaceAll(variablePlaceholder, variableValue.replaceAll("\\\\", "\\\\\\\\"));
            } else {
                break;
            }
        } while (true);
        return result;
    }

    private String getVariableValue(final String propertyName, final Object... objects) {
        final String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            return propertyValue;
        }
        final String envValue = System.getenv(propertyName);
        if (envValue != null) {
            return envValue;
        }
        String result = null;
        final String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        for (final Object object : objects) {
            try {
                final Method getter = object.getClass().getMethod(getterName);
                if (getter.getReturnType() != Void.class) {
                    Object getterResultValue;
                    try {
                        getterResultValue = getter.invoke(object);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    if (getterResultValue != null) {
                        result = getterResultValue.toString();
                        break;
                    }
                }
            } catch (SecurityException e) {
                // throw new RuntimeException(e);
                e = null;
            } catch (NoSuchMethodException e) {
                // throw new RuntimeException(StringUtils.format("No property '%s' in jdbc profile.", propertyName), e);
                e = null;
            }
        }
        return result;
    }

    private synchronized void loadDriver(final JDBCProfile profile) {
        if (!loadedProfiles.contains(profile)) {
            final String file = replace(profile.getJdbc().getFile());
            File installationLocation = configurationService.getInstallationDir();
            final File driverFile = new File(installationLocation, file);
            final String jdbcDriverFileInfo = StringUtils.format("JDBC driver file '%s'", driverFile.getAbsolutePath());
            if (!driverFile.exists() || !driverFile.isFile() || !driverFile.canRead()) {
                throw new RuntimeException(jdbcDriverFileInfo + " could not be found.");
            }
            try {
                final URL[] urls = new URL[] { driverFile.toURI().toURL() };
                final URLClassLoader classLoader = new URLClassLoader(urls);
                classLoaders.put(profile, classLoader);
                final String driverClassName = profile.getJdbc().getDriver();
                final Class<?> driverClass = Class.forName(driverClassName, true, classLoader);
                final Driver driver = (Driver) driverClass.newInstance();
                DriverManager.registerDriver(new DriverAdapter(driver));
            } catch (MalformedURLException e) {
                throw new RuntimeException(jdbcDriverFileInfo + " could not be loaded. " + e.toString());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(jdbcDriverFileInfo + " could not be found. " + e.toString());
            } catch (InstantiationException e) {
                throw new RuntimeException(jdbcDriverFileInfo + " could not be instantiated. " + e.toString());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(jdbcDriverFileInfo + " could not be accessed. " + e.toString());
            } catch (SQLException e) {
                throw new RuntimeException(jdbcDriverFileInfo + " could not be registered. " + e.toString());
            }
        }
        loadedProfiles.add(profile);
    }

}
