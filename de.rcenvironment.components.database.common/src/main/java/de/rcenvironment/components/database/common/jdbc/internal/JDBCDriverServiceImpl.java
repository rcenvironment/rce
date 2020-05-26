/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.common.jdbc.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.components.database.common.jdbc.JDBCDriverInformation;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverService;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathListId;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link JDBCDriverService}.
 * 
 * @author Doreen Seider
 * @author Oliver Seebach
 */
public class JDBCDriverServiceImpl implements JDBCDriverService {

    private static final String ERROR_MESSAGE = "Failed to register JDBC driver (file: %s, driver class: %s): %s";

    private static final Log LOGGER = LogFactory.getLog(JDBCDriverServiceImpl.class);

    private static Map<String, JDBCDriverInformation> driverClassNamesWithInfo = new HashMap<>();

    private final Map<String, URLClassLoader> classLoaders = new HashMap<>();

    private ConfigurationService configurationService;

    private Set<JDBCDriverInformation> driverInformations = new HashSet<>();

    private List<String> registeredDrivers = new ArrayList<>();

    // see http://www.devx.com/tips/Tip/28818 for list -- seeb_ol, November 2015
    static {
        driverClassNamesWithInfo.put("com.mysql.jdbc.Driver",
            new JDBCDriverInformationImpl("mysql", "MySQL"));
        driverClassNamesWithInfo.put("org.postgresql.Driver",
            new JDBCDriverInformationImpl("postgresql", "PostgreSQL"));
        driverClassNamesWithInfo.put("COM.ibm.db2.jdbc.app.DB2Driver",
            new JDBCDriverInformationImpl("db2", "IBM DB2L"));
        driverClassNamesWithInfo.put("sun.jdbc.odbc.JdbcOdbcDriver",
            new JDBCDriverInformationImpl("odbc", "JDBC-ODBC Bridge"));
        driverClassNamesWithInfo.put("weblogic.jdbc.mssqlserver4.Driver",
            new JDBCDriverInformationImpl("weblogic:mssqlserver4", "Microsoft SQL Server"));
        driverClassNamesWithInfo.put("oracle.jdbc.driver.OracleDriver",
            new JDBCDriverInformationImpl("oracle:thin", "Oracle Thin"));
        driverClassNamesWithInfo.put("com.pointbase.jdbc.jdbcUniversalDriver",
            new JDBCDriverInformationImpl("pointbase://embedded[", "PointBase Embedded Server"));
        driverClassNamesWithInfo.put("COM.cloudscape.core.JDBCDriver",
            new JDBCDriverInformationImpl("cloudscape", "Cloudscape"));
        driverClassNamesWithInfo.put("RmiJdbc.RJDriver",
            new JDBCDriverInformationImpl("rmi", "Cloudscape RMI"));
        driverClassNamesWithInfo.put("org.firebirdsql.jdbc.FBDriver",
            new JDBCDriverInformationImpl("firebirdsql", "Firebird (JCA/JDBC Driver)"));
        driverClassNamesWithInfo.put("ids.sql.IDSDriver",
            new JDBCDriverInformationImpl("ids", "IDS Server"));
        driverClassNamesWithInfo.put("com.informix.jdbc.IfxDriver",
            new JDBCDriverInformationImpl("informix-sqli", "Informix Dynamic Server"));
        driverClassNamesWithInfo.put("jdbc.idbDriver",
            new JDBCDriverInformationImpl("idb", "InstantDB (v3.13 and earlier)"));
        driverClassNamesWithInfo.put("org.enhydra.instantdb.jdbc.idbDriver",
            new JDBCDriverInformationImpl("idb", "InstantDB (v3.14 and later)"));
        driverClassNamesWithInfo.put("interbase.interclient.Driver",
            new JDBCDriverInformationImpl("interbase", "Interbase (InterClient Driver)"));
        driverClassNamesWithInfo.put("hSql.hDriver",
            new JDBCDriverInformationImpl("HypersonicSQL", "Hypersonic SQL (v1.2 and earlier)"));
        driverClassNamesWithInfo.put("org.hsql.jdbcDriver",
            new JDBCDriverInformationImpl("HypersonicSQL", "Hypersonic SQL (v1.3 and later)"));
        driverClassNamesWithInfo.put("com.ashna.jturbo.driver.Driver",
            new JDBCDriverInformationImpl("JTurbo", "Microsoft SQL Server (JTurbo Driver)"));
        driverClassNamesWithInfo.put("com.inet.tds.TdsDriver",
            new JDBCDriverInformationImpl("inetdae", "Microsoft SQL Server (Sprinta Driver)"));
        driverClassNamesWithInfo.put("com.microsoft.sqlserver.jdbc.SQLServerDriver",
            new JDBCDriverInformationImpl("microsoft:sqlserver", "Microsoft SQL Server 2000 (Microsoft Driver)"));
        driverClassNamesWithInfo.put("oracle.jdbc.driver.OracleDriver",
            new JDBCDriverInformationImpl("oracle:oci", "Oracle OCI 9i"));
        driverClassNamesWithInfo.put("com.sybase.jdbc.SybDriver",
            new JDBCDriverInformationImpl("sybase:Tds", "Sybase (jConnect 4.2 and earlier)"));
        driverClassNamesWithInfo.put("com.sybase.jdbc2.jdbc.SybDriver",
            new JDBCDriverInformationImpl("sybase:Tds", "Sybase (jConnect 5.2)"));
    }

    /**
     * Activate method.
     * 
     * @param context The BundleContext
     */
    public void activate(BundleContext context) {
        registerJDBCDrivers();
    }

    /**
     * OSGi-DS life cycle method.
     */
    public void deactivate() {
        for (String classloaderName : classLoaders.keySet()) {
            URLClassLoader classLoader = classLoaders.get(classloaderName);
            if (classLoader != null) {
                try {
                    classLoader.close();
                    LOGGER.debug("Closed classloader for " + classloaderName);
                } catch (IOException e) {
                    LOGGER.error("Failed to close classloader for " + classloaderName + ". Reason: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Set<JDBCDriverInformation> getRegisteredJDBCDrivers() {
        return Collections.unmodifiableSet(driverInformations);
    }

    /**
     * Bind configuration service method.
     * 
     * @param service The service to bind.
     */
    public void bindConfigurationService(ConfigurationService service) {
        configurationService = service;
    }

    private void loadDriverIfNotLoadedAlready(File driverFile, String driverClassName) {
        // TODO improve check if driver already loaded -- seeb_ol, November 2015
        if (!registeredDrivers.contains(driverClassName)) {
            loadDriver(driverFile, driverClassName);
        }
    }

    private void registerJDBCDrivers() {
        for (File dir : configurationService.getConfigurablePathList(ConfigurablePathListId.JDBC_DRIVER_DIRS)) {
            for (File file : dir.listFiles()) {
                // TODO improve filter and detection mechanism -- seeb_ol, November 2015
                if (file.getName().contains("mysql")) {
                    String scheme = "com.mysql.jdbc.Driver";
                    loadDriverIfNotLoadedAlready(file, scheme);
                } else if (file.getName().contains("postgresql")) {
                    String scheme = "org.postgresql.Driver";
                    loadDriverIfNotLoadedAlready(file, scheme);
                }
            }
        }
    }

    private void loadDriver(File driverFile, String driverClassName) {
        if (!driverFile.exists() || !driverFile.isFile() || !driverFile.canRead()) {
            LOGGER
                .error(StringUtils.format(ERROR_MESSAGE, driverClassName, driverFile.getAbsolutePath(), "file not found or not readable"));
            return;
        }
        URL[] urls;
        try {
            urls = new URL[] { driverFile.toURI().toURL() };
        } catch (MalformedURLException e) {
            LOGGER.error(StringUtils.format(ERROR_MESSAGE, driverClassName, driverFile.getAbsolutePath(), e.getMessage()));
            return;
        }
        try {
            URLClassLoader classLoader = new URLClassLoader(urls);
            final Class<?> driverClass = Class.forName(driverClassName, true, classLoader);
            final Driver driver = (Driver) driverClass.newInstance();
            DriverManager.registerDriver(new DriverAdapter(driver));
            registeredDrivers.add(driverClassName);
            classLoaders.put(driverClassName, classLoader);
            LOGGER.info("Successfully registered JDBC driver from file: " + driverFile.getAbsolutePath());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException e) {
            LOGGER.error(StringUtils.format(ERROR_MESSAGE, driverClassName, driverFile.getAbsolutePath(), e.getMessage()));
            return;
        }
        driverInformations.add(driverClassNamesWithInfo.get(driverClassName));
    }

    @Override
    public Connection getConnectionWithCredentials(String url, String databaseUser, String databasePassword)
        throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(url, databaseUser, databasePassword);
            return connection;
        } catch (SQLNonTransientConnectionException e) {
            throw new SQLException("Failed to establish connection; " + e.getCause().getMessage());
        }
    }

    @Override
    public Driver getDriverForURL(String url) throws SQLException {
        return DriverManager.getDriver(url);
    }

    /**
     * A driver adapter to forward requests to another {@link Driver} implementation.
     * 
     * @author Christian Weiss
     */
    private class DriverAdapter implements Driver {

        private final Driver driver;

        DriverAdapter(final Driver driver) {
            this.driver = driver;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        /**
         * This method is necessary for Java 7 compatibility. It MUST NOT have an @Override annotation, otherwise it will break on Java 6.
         * -- misc_ro
         * 
         * @see java.sql.Driver#getParentLogger()
         * 
         * @return nothing; this method will always throw an exception (see parent JavaDoc)
         * @throws SQLFeatureNotSupportedException always thrown to signal that java.util.logging is not used
         */
        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            // see java.sql.Driver#getParentLogger()
            throw new SQLFeatureNotSupportedException("Not using java.util.logging");
        }
    }

}
