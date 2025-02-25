package com.wisecoders.dbschema.mongodb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Licensed under <a href="https://creativecommons.org/licenses/by-nd/4.0/deed.en">CC BY-ND 4.0 DEED</a>, copyright <a href="https://wisecoders.com">Wise Coders GmbH</a>, used by <a href="https://dbschema.com">DbSchema Database Designer</a>.
 * Code modifications allowed only as pull requests to the <a href="https://github.com/wise-coders/mongodb-jdbc-driver">public GIT repository</a>.
 */

public class ReverseEngineer extends AbstractTestCase {
    private Connection con;

    private static final String urlWithAuth = "jdbc:mongodb://admin:fictivpwd@localhost:27017/local?authSource=local&connectTimeoutMS=1000";
    private static final String urlWithoutAuth = "jdbc:mongodb://localhost";


    @BeforeEach
    public void setUp() throws ClassNotFoundException, SQLException {
        Class.forName("com.wisecoders.dbschema.mongodb.JdbcDriver");
        con = DriverManager.getConnection(urlWithoutAuth, null, null);
        Statement stmt = con.createStatement();
        stmt.execute("local.words.drop();");
        stmt.execute("local.words.insertOne({word: 'sample', qty:2, prop: [{ category:'verb'},{ base:'latin'}]});");
        stmt.execute("local.words.createIndex( { word: 1, 'prop.category':1 }, {name:'sampleIndex'} );");
        stmt.execute("use tournament;");
        stmt.execute("tournament.students.drop();");
        stmt.execute("tournament.createCollection('students', {\n" +
                     "   validator: {\n" +
                     "      $jsonSchema: {\n" +
                     "         bsonType: 'object',\n" +
                     "         required: [ 'name', 'year', 'major', 'address' ],\n" +
                     "         properties: {\n" +
                     "            name: {\n" +
                     "               bsonType: 'string',\n" +
                     "               description: 'must be a string and is required'\n" +
                     "            },\n" +
                     "            year: {\n" +
                     "               bsonType: 'int',\n" +
                     "               minimum: 2017,\n" +
                     "               maximum: 3017,\n" +
                     "               description: 'must be an integer in [ 2017, 3017 ] and is required'\n" +
                     "            },\n" +
                     "            major: {\n" +
                     "               enum: [ 'Math', 'English', 'Computer Science', 'History', null ],\n" +
                     "               description: 'can only be one of the enum values and is required'\n" +
                     "            },\n" +
                     "            gpa: {\n" +
                     "               bsonType: [ 'double' ],\n" +
                     "               description: 'must be a double if the field exists'\n" +
                     "            },\n" +
                     "            address: {\n" +
                     "               bsonType: 'object',\n" +
                     "               required: [ 'city' ],\n" +
                     "               properties: {\n" +
                     "                  street: {\n" +
                     "                     bsonType: 'string',\n" +
                     "                     description: 'must be a string if the field exists'\n" +
                     "                  },\n" +
                     "                  city: {\n" +
                     "                     bsonType: 'string',\n" +
                     "                     description: 'must be a string and is required'\n" +
                     "                  }\n" +
                     "               }\n" +
                     "            }\n" +
                     "         }\n" +
                     "      }\n" +
                     "   }\n" +
                     "})");
        stmt.execute("tournament.contacts.drop();");
        stmt.execute("tournament.createCollection( 'contacts',\n" +
                     "   { validator: { $or:\n" +
                     "      [\n" +
                     "         { phone: { $type: 'string' } },\n" +
                     "         { email: { $regex: \"@mongodb\\.com$\" } },\n" +
                     "         { status: { $in: [ 'Unknown', 'Incomplete' ] } }\n" +
                     "      ]\n" +
                     "   }\n" +
                     "} )");


        stmt.execute("db.master.drop()");
        stmt.execute("db.slave.drop()");

        stmt.execute("db.master.insert( { _id: 1, item: \"box1\", qty: 21 } )");
        stmt.execute("db.master.insert( {  item: \"box3\", qty: 23 } )");


        stmt.execute("var itr = db.master.find().iterator()\n" +
                     "var oid = itr.next().get('_id')\n" +
                     "print(\"ObjectId rec1 \" + oid)\n" +
                     "db.slave.insert( { name: \"slave1\", master_id: oid } )\n" +

                     "var oid = itr.next().get('_id')\n" +
                     "print(\"ObjectId rec2 \" + oid)\n" +
                     "db.slave.insert( { name: \"slave2\", master_id: oid } )\n"
        );

        stmt.close();

    }

    @Test
    public void testReverseEngineer() throws SQLException {
        ResultSet rs = con.getMetaData().getTables("tournament", "tournament", null, null);
        while (rs.next()) {
            String colName = rs.getString(3);
            printResultSet(con.getMetaData().getColumns("local", "local", colName, null));
            printResultSet(con.getMetaData().getColumns("tournament", "local", colName, null));
            printResultSet(con.getMetaData().getIndexInfo("local", "local", colName, false, false));
        }
        ResultSet rsf = con.getMetaData().getExportedKeys("tournament", "tournament", null);
        while (rsf.next()) {
            String colName = rsf.getString(3);
        }
    }


}
