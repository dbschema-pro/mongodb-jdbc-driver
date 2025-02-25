package com.wisecoders.dbschema.mongodb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Licensed under <a href="https://creativecommons.org/licenses/by-nd/4.0/deed.en">CC BY-ND 4.0 DEED</a>, copyright <a href="https://wisecoders.com">Wise Coders GmbH</a>, used by <a href="https://dbschema.com">DbSchema Database Designer</a>.
 * Code modifications allowed only as pull requests to the <a href="https://github.com/wise-coders/mongodb-jdbc-driver">public GIT repository</a>.
 */

public class ExpandedResultSet extends AbstractTestCase{

    private Connection con;

    private static final String urlWithoutAuth = "jdbc:mongodb://localhost?expand=true";


    @BeforeEach
    public void setUp() throws ClassNotFoundException, SQLException {
        Class.forName("com.wisecoders.dbschema.mongodb.JdbcDriver");
        con = DriverManager.getConnection( urlWithoutAuth, null, null);
        Statement stmt=con.createStatement();
        stmt.execute("local.words.drop();");
        stmt.execute("local.words.insertOne({word: 'sample1'});");
        stmt.execute("local.words.insertOne({word: 'sample2', qty:5});");
        stmt.close();
    }

    @Test
    public void testFind() throws Exception {
        Statement stmt=con.createStatement();
        ResultSet rs = stmt.executeQuery("local.words.find()");
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++ ){
            System.out.printf( "%30s", metaData.getColumnName( i)+ "(" + metaData.getColumnType(i) + ")");
        }
        System.out.println();
        while ( rs.next() ){
            for (int i = 1; i <= metaData.getColumnCount(); i++ ) {
                System.out.printf("%30s", rs.getString(i));
            }
            System.out.println();
        }
        stmt.close();
    }


}
