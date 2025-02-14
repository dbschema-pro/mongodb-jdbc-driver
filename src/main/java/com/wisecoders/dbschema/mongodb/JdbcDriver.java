
package com.wisecoders.dbschema.mongodb;

import com.wisecoders.dbschema.mongodb.wrappers.WrappedMongoClient;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 * Minimal implementation of the JDBC standards for MongoDb database.
 * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
 * Example :
 * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
 *
 * Licensed under <a href="https://creativecommons.org/licenses/by-nd/4.0/deed.en">CC BY-ND 4.0 DEED</a>, copyright <a href="https://wisecoders.com">Wise Coders GmbH</a>, used by <a href="https://dbschema.com">DbSchema Database Designer</a>.
 * Code modifications allowed only as pull requests to the <a href="https://github.com/wise-coders/mongodb-jdbc-driver">public GIT repository</a>.
 */
public class JdbcDriver implements Driver
{
    private final DriverPropertyInfoHelper propertyInfoHelper = new DriverPropertyInfoHelper();

    public static final Logger LOGGER = Logger.getLogger( JdbcDriver.class.getName() );

    static {
        try {
            final File logsFile = new File("~/.DbSchema/logs/");
            if ( !logsFile.exists()) {
                logsFile.mkdirs();
            }

            DriverManager.registerDriver( new JdbcDriver());
            LOGGER.setLevel(Level.ALL);

            final ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(consoleHandler);

            final FileHandler fileHandler = new FileHandler(System.getProperty("user.home") + "/.DbSchema/logs/MongoDbJdbcDriver.log");
            fileHandler.setFormatter( new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            LOGGER.addHandler(fileHandler);

        } catch ( Exception ex ){
            ex.printStackTrace();
        }
    }


    /**
     * Connect to the database using a URL like:
     * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
     * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (url != null && acceptsURL(url)) {
            if (url.startsWith("jdbc:")) {
                url = url.substring("jdbc:".length());
            }
            LOGGER.info("Connect URL: " + url);

            // Extract username and password from Properties
            String user = info.getProperty("user");
            String password = info.getProperty("password");

            // If username and password are provided, inject them into the URL
            if (user != null && password != null) {
                int atIndex = url.indexOf("@");
                if (atIndex > 0) {
                    // Replace existing credentials in the URL
                    int protocolEndIndex = url.indexOf("//") + 2;
                    int credentialsEndIndex = url.indexOf("@");
                    url = url.substring(0, protocolEndIndex) + user + ":" + password + "@" + url.substring(credentialsEndIndex + 1);
                } else {
                    // Add credentials if not present
                    int protocolEndIndex = url.indexOf("//") + 2;
                    url = url.substring(0, protocolEndIndex) + user + ":" + password + "@" + url.substring(protocolEndIndex);
                }
            }

            LOGGER.info("Updated URL with credentials: " + url);

            // Extract existing query parameters from the URL
            int queryIndex = url.indexOf("?");
            String baseUrl = queryIndex > 0 ? url.substring(0, queryIndex) : url;
            String existingParams = queryIndex > 0 ? url.substring(queryIndex + 1) : "";

            // Append additional parameters from Properties
            StringBuilder newParams = new StringBuilder(existingParams);
            for (String key : info.stringPropertyNames()) {
                if (!key.equals("user") && !key.equals("password")) { // Skip user and password
                    if (newParams.length() > 0) {
                        newParams.append("&");
                    }
                    newParams.append(key).append("=").append(info.getProperty(key));
                }
            }

            // Reconstruct the final URL
            String finalUrl = baseUrl;
            if (newParams.length() > 0) {
                finalUrl += "?" + newParams;
            }

            LOGGER.info("Final URL with additional parameters: " + finalUrl);

            int idx;
            ScanStrategy scan = ScanStrategy.fast;
            boolean expand = false, sortFields = false;
            String trustStore = null, trustStorePassword = null;
            String newUrl = finalUrl, urlWithoutParams = finalUrl;

            if ((idx = finalUrl.indexOf("?")) > 0) {
                String paramsURL = finalUrl.substring(idx + 1);
                urlWithoutParams = finalUrl.substring(0, idx);
                StringBuilder sbParams = new StringBuilder();
                for (String pair : paramsURL.split("&")) {
                    String[] pairArr = pair.split("=");
                    String key = pairArr.length == 2 ? pairArr[0].toLowerCase() : "";
                    String value = pairArr.length == 2 ? pairArr[1] : "";
                    switch (key) {
                        case "scan":
                            try {
                                scan = ScanStrategy.valueOf(value);
                            } catch (IllegalArgumentException ignore) {
                            }
                            LOGGER.info("ScanStrategy=" + scan);
                            break;
                        case "expand":
                            expand = Boolean.parseBoolean(value);
                            break;
                        case "sort":
                            sortFields = Boolean.parseBoolean(value);
                            break;
                        case "truststore":
                            trustStore = value;
                            break;
                        case "truststorepassword":
                            trustStorePassword = value;
                            break;
                        default:
                            if (sbParams.length() > 0) sbParams.append("&");
                            sbParams.append(pair);
                            break;
                    }
                }
                newUrl = finalUrl.substring(0, idx) + "?" + sbParams;
            }

            if (trustStore != null) {
                System.setProperty("javax.net.ssl.trustStore", trustStore);
            }
            if (trustStorePassword != null) {
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            }

            String databaseName = "admin";
            if (urlWithoutParams.endsWith("/")) {
                urlWithoutParams = urlWithoutParams.substring(0, urlWithoutParams.length() - 1);
            }
            if ((idx = urlWithoutParams.lastIndexOf("/")) > 1 && urlWithoutParams.charAt(idx - 1) != '/') {
                databaseName = urlWithoutParams.substring(idx + 1);
            }

            LOGGER.info("MongoClient URL: " + finalUrl + " rewritten as " + newUrl);
            final WrappedMongoClient client = new WrappedMongoClient(newUrl, info, databaseName, scan, expand, sortFields);
            return new MongoConnection(client);
        }
        return null;
    }


    /**
     * URLs accepted are of the form: jdbc:mongodb[+srv]://&lt;server&gt;[:27017]/&lt;db-name&gt;
     *
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("mongodb") || url.startsWith("jdbc:mongodb");
    }

    /**
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException
    {
        return propertyInfoHelper.getPropertyInfo();
    }

    /**
     * @see java.sql.Driver#getMajorVersion()
     */
    @Override
    public int getMajorVersion()
    {
        return 1;
    }

    /**
     * @see java.sql.Driver#getMinorVersion()
     */
    @Override
    public int getMinorVersion()
    {
        return 0;
    }

    /**
     * @see java.sql.Driver#jdbcCompliant()
     */
    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

}
