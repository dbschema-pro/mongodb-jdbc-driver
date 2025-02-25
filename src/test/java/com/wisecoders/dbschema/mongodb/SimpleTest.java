package com.wisecoders.dbschema.mongodb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Licensed under <a href="https://creativecommons.org/licenses/by-nd/4.0/deed.en">CC BY-ND 4.0 DEED</a>, copyright <a href="https://wisecoders.com">Wise Coders GmbH</a>, used by <a href="https://dbschema.com">DbSchema Database Designer</a>.
 * Code modifications allowed only as pull requests to the <a href="https://github.com/wise-coders/mongodb-jdbc-driver">public GIT repository</a>.
 */

public class SimpleTest extends AbstractTestCase {

    private Connection con;

    private static final String urlWithAuth = "jdbc:mongodb://localhost:27017/local?scan=fast&authSource=local&connectTimeoutMS=1000";
    private static final String urlWithoutAuth = "jdbc:mongodb://localhost";


    @BeforeEach
    public void setUp() throws ClassNotFoundException, SQLException {
        Class.forName("com.wisecoders.dbschema.mongodb.JdbcDriver");
        con = DriverManager.getConnection(urlWithAuth, null, null);
        Statement stmt = con.createStatement();
        stmt.execute("local.books.drop();");
        stmt.execute("local.booksView.drop();");
        stmt.execute("local.books.insertOne({name: 'Java', qty:2});");
        stmt.execute("local.books.insertOne({name: 'Python', qty:5});");
        stmt.execute("local.books.insertOne({name: 'C++', qty:15});");
        stmt.execute("local.createView('booksView','books', [{ $project: { 'bookName': '$name', qty: 1 }}] )");
        stmt.close();
    }

    @Test
    public void testListDatabases() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.listCollectionNames()"));
        stmt.close();
    }

    @Test
    public void testGetViewSource() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.getViewSource('booksView')"));
        stmt.close();
    }

    @Test
    public void testFind() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.words.find()"));
        stmt.close();
    }

    @Test
    public void testFindRegEx() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.words.find(/^J/)"));
        stmt.close();
    }

    @Test
    public void testInsert2() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("" +
                                         "local.cities.drop();" +
                                         "local.cities.insert(\n" +
                                         "{ 'country_id' : 'USA', \n" +
                                         "    'city_name' : 'San Francisco', \n" +
                                         "    'brother_cities' : [\n" +
                                         "        'Urban', 'Paris'\n" +
                                         "    ], \n" +
                                         "    'suburbs' : [\n" +
                                         "         {\n" +
                                         "            'name' : 'Scarsdale'\n" +
                                         "         }, \n" +
                                         "        {\n" +
                                         "            'name' : 'North Hills'\n" +
                                         "        } ]\n" +
                                         "    })"));
        con.commit();
        printResultSet(stmt.executeQuery("local.cities.find()"));
        stmt.close();
    }

    @Test
    public void testFindAnd() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.books.find({ $and: [ {'name':'Java'}, {'qty':2} ] } )"));
        stmt.close();
    }

    @Test
    public void testCount() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.books.count()"));
        stmt.close();
    }

    @Test
    public void testDBRef() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.bicycles.insertOne( { 'name' : 'city bike', 'bike_colour' : DBRef( 'colours', 'Bla') } ); "));
        stmt.close();
    }

    @Test
    public void testFindAndOr() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.books.find({ $or: [{'name': 'Java'}, {'name': 'C++' }]})"));
        stmt.close();
    }

    @Test
    public void testUpdate() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.books.update({'name':'Java'},{$set:{'name':'OpenJDK'}})"));
        stmt.close();
    }


    @Test
    public void testFindId() throws Exception {
        /*
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId("5dd593595f94074908de3db9"));
        printResultSet( new ResultSetIterator(((MongoConnection)con).client.getDatabase("local").getCollection("products").find( query), true));
*/
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.books.find({_id:'5facdca7fea0441ab001f51d'})"));
        stmt.close();
    }

    @Test
    public void testInsert() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.persons.insert({ 'firstname' : 'Anna', 'lastname' : 'Pruteanu' })"));
        stmt.close();
    }

    @Test
    public void testInsertMany() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.testMany.insertMany( [{ 'hello' : '', 'qty454' : 0 }, { 'hello' : '', 'qty454' : 0 }])"));
        stmt.close();
    }

    @Test
    public void testISODate() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.testISODate.insert({'shopId':'baaacd90d36e11e9adb40a8baad32c5a','date':ISODate('2019-12-25T07:23:18.408Z')})"));
        stmt.close();
    }

    @Test
    public void testFindGt() throws Exception {
        Statement stmt = con.createStatement();
        printResultSet(stmt.executeQuery("local.books.find( {qty:{$gt: 4}})"));
        stmt.close();
    }

    @Test
    public void testOID() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("local.testObjectID.drop();");
        printResultSet(stmt.executeQuery("local.testObjectID.insert({'_id':ObjectId('5e95cfecdfa8c111a4b2a53a'), 'name':'Lulu2'})"));
        stmt.close();
    }

    private static final String[] aggregateScript = new String[]{
            "db.food.drop();",
            "db.food.insert([\n" +
            "   { category: 'cake', type: 'chocolate', qty: 10 },\n" +
            "   { category: 'cake', type: 'ice cream', qty: 25 },\n" +
            "   { category: 'pie', type: 'boston cream', qty: 20 },\n" +
            "   { category: 'pie', type: 'blueberry', qty: 15 }\n" +
            "]);",
            "db.food.createIndex( { qty: 1, type: 1 } );",
            "db.food.createIndex( { qty: 1, category: 1 } );",
            "db.food.aggregate( [ { $sort: { qty: 1 }}, { $match: { category: 'cake', qty: 10  } }, { $sort: { type: -1 } } ] ) "
    };


    @Test
    public void testAggregate() throws Exception {
        Statement stmt = con.createStatement();
        for (String str : aggregateScript) {
            printResultSet(stmt.executeQuery(str));
        }
    }

    @Test
    public void testAggregte2() throws Exception {
        Statement stmt = con.createStatement();
        stmt.execute("use local");
        printResultSet(stmt.executeQuery("" +
                                         "db.orders.aggregate([\n" +
                                         " { $match: { status: \"A\" } },\n" +
                                         " { $group: { _id: \"$cust_id\", total: { $sum: \"$amount\" } } },\n" +
                                         " { $sort: { total: -1 } }\n" +
                                         "]);"));
        stmt.close();
    }

    @Test
    public void testInsert3() throws Exception {
        Statement st = con.createStatement();
        st.execute("local.getCollection('issue2').insert({ 'name' : 'aaa' })");
        st.close();
    }

}
