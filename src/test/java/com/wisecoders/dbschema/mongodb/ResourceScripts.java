package com.wisecoders.dbschema.mongodb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * We tested using a MongoDb docker container running local.
 *
 * Licensed under <a href="https://creativecommons.org/licenses/by-nd/4.0/deed.en">CC BY-ND 4.0 DEED</a>, copyright <a href="https://wisecoders.com">Wise Coders GmbH</a>, used by <a href="https://dbschema.com">DbSchema Database Designer</a>.
 * Code modifications allowed only as pull requests to the <a href="https://github.com/wise-coders/mongodb-jdbc-driver">public GIT repository</a>.
 */
public class ResourceScripts extends AbstractTestCase {

    Connection con;

    @BeforeEach
    public void setUp() throws ClassNotFoundException, SQLException {
        Class.forName("com.wisecoders.dbschema.mongodb.JdbcDriver");
        con = DriverManager.getConnection("jdbc:mongodb://localhost", "root", "root");
    }

    @Test
    public void script() throws IOException, SQLException {
        executeFile("script.txt");
    }

    @Test
    public void aggregate() throws IOException, SQLException {
        executeFile("testAggregate.txt");
    }

    @Test
    public void aggregate2() throws IOException, SQLException {
        executeFile("testAggregate2.txt");
    }

    @Test
    public void inventory() throws IOException, SQLException {
        executeFile("inventory.txt");
    }

    @Test
    public void mapReduce() throws IOException, SQLException {
        executeFile("mapReduce.txt");
    }

    @Test
    public void masterSlave() throws IOException, SQLException {
        executeFile("masterSlave.txt");
    }

    private void executeFile(String fileName) throws IOException, SQLException {
        InputStream is = getClass().getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        final StringBuffer sb = new StringBuffer();
        while ((line = br.readLine()) != null) {
            if (!line.isBlank()) {
                sb.append(line).append("\n");
            }
            if (line.trim().endsWith(";")) {
                executeQuery(sb.toString());
                sb.delete(0, sb.length());
            }
        }
        if (sb.length() > 0) {
            executeQuery(sb.toString());
        }
    }

    private void executeQuery(String query) throws SQLException {
        System.out.println("> " + query);
        final Statement stmt = con.createStatement();
        try {
            printResultSet(stmt.executeQuery(query));
        } catch (Throwable ex) {
            System.out.println(ex.getLocalizedMessage());
            throw ex;
        }
        stmt.close();
    }
}
