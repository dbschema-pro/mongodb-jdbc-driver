package com.wisecoders.dbschema.mongodb.wrappers;

import com.mongodb.ConnectionString;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;
import com.wisecoders.dbschema.mongodb.ScanStrategy;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

import static com.wisecoders.dbschema.mongodb.JdbcDriver.LOGGER;

/**
 * Copyright Wise Coders GmbH. The MongoDB JDBC driver is build to be used with  <a href="https://dbschema.com">DbSchema Database Designer</a>
 * Free to use by everyone, code modifications allowed only to the  <a href="https://github.com/wise-coders/mongodb-jdbc-driver">public repository</a>
 */
public class WrappedMongoClient {

    private final MongoClient mongoClient;
    private final String databaseName;
    private final String uri;
    private final ScanStrategy scanStrategy;
    public final boolean expandResultSet, sortFields;

    public WrappedMongoClient(String uri, final Properties prop, final String databaseName, final ScanStrategy scanStrategy, boolean expandResultSet, boolean sortFields ){
        final ConnectionString connectionString = new ConnectionString(uri){
            @Override
            public Integer getMaxConnectionIdleTime() {
                return Integer.MAX_VALUE;
            }

            @Override
            public UuidRepresentation getUuidRepresentation() {
                return UuidRepresentation.STANDARD;
            }
        };
        this.mongoClient = MongoClients.create(connectionString);
        this.databaseName = databaseName;
        this.uri = uri;
        this.expandResultSet = expandResultSet;
        this.scanStrategy = scanStrategy;
        this.sortFields = sortFields;
        getDatabaseNames();
    }

    public boolean pingServer(){
        mongoClient.listDatabaseNames();
        Bson command = new BsonDocument("ping", new BsonInt64(1));
        try {
            mongoClient.getDatabase(databaseName != null && !databaseName.isEmpty() ? databaseName : "admin").runCommand(command);
            LOGGER.info("Connected successfully to server.");
            return true;
        } catch ( Throwable ignore ){}
        mongoClient.getDatabase("admin").runCommand(command);
        LOGGER.info("Connected successfully to server.");
        return true;
    }

    public void close(){
        mongoClient.close();
    }

    public MongoIterable<String> listDatabaseNames() {
        return mongoClient.listDatabaseNames();
    }

    public ListDatabasesIterable<Document> listDatabases() {
        return mongoClient.listDatabases();
    }

    public <T> ListDatabasesIterable<T> listDatabases(Class<T> clazz) {
        return mongoClient.listDatabases(clazz);
    }

    // USE STATIC SO OPENING A NEW CONNECTION WILL REMEMBER THIS
    public static final List<String> createdDatabases = new ArrayList<>();


    public String getCurrentDatabaseName() {
        // SEE THIS TO SEE HOW DATABASE NAME IS USED : http://api.mongodb.org/java/current/com/mongodb/MongoClientURI.html
        return databaseName != null ? databaseName : "admin";
    }

    public List<String> getDatabaseNames() {
        final List<String> names = new ArrayList<>();
        try {
            // THIS OFTEN THROWS EXCEPTION BECAUSE OF MISSING RIGHTS. IN THIS CASE WE ONLY ADD CURRENT KNOWN DB.
            for ( String dbName : listDatabaseNames() ){
                names.add( dbName );
            }
        } catch ( Throwable ex ){
            names.add( getCurrentDatabaseName() );
        }
        for ( String str : createdDatabases ){
            if ( !names.contains( str )){
                names.add( str );
            }
        }
        return names;
    }

    private final Map<String, WrappedMongoDatabase> cachedDatabases = new HashMap<>();

    public WrappedMongoDatabase getDatabase(String dbName) {
        if ( cachedDatabases.containsKey(dbName )){
            return cachedDatabases.get( dbName);
        }
        WrappedMongoDatabase db = new WrappedMongoDatabase(mongoClient.getDatabase(dbName), scanStrategy, sortFields );
        cachedDatabases.put( dbName, db );
        return db;
    }

    public List<WrappedMongoDatabase> getDatabases() {
        final List<WrappedMongoDatabase> list = new ArrayList<>();

        for ( String dbName : getDatabaseNames() ){
            list.add( getDatabase(dbName));
        }
        return list;
    }


    public String getVersion(){
        return "1.1";
    }

    public String getURI() {
        return uri;
    }


    public List<String> getCollectionNames(String databaseName) throws SQLException {
        final List<String> list = new ArrayList<>();
        try {
            WrappedMongoDatabase db = getDatabase(databaseName);
            if ( db != null ){
                for ( String name : db.listCollectionNames() ){
                    list.add( name );
                }
            }
            list.remove("system.indexes");
            list.remove("system.users");
            list.remove("system.views");
            list.remove("system.version");
        } catch ( Throwable ex ){
            LOGGER.log(Level.SEVERE, "Cannot list collection names for " + databaseName + ". ", ex );
            throw new SQLException( ex );
        }
        return list;
    }

    public List<String> getViewNames(String databaseName) throws SQLException {
        List<String> list = new ArrayList<>();
        try {
            WrappedMongoDatabase db = getDatabase(databaseName);
            if ( db != null ){
                for ( Document doc : db.listCollections()){
                    if ( doc.containsKey("type") && "view".equals(doc.get("type"))){
                        list.add( String.valueOf( doc.get("name")) );
                    }
                }
            }
        } catch ( Throwable ex ){
            LOGGER.log(Level.SEVERE, "Cannot list collection names for " + databaseName + ". ", ex );
            throw new SQLException( ex );
        }
        return list;
    }

}
