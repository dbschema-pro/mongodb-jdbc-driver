package com.wisecoders.dbschema.mongodb;

import com.google.gson.Gson;
import com.wisecoders.dbschema.mongodb.structure.MetaObject;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Licensed under <a href="https://creativecommons.org/licenses/by-nd/4.0/deed.en">CC BY-ND 4.0 DEED</a>, copyright <a href="https://wisecoders.com">Wise Coders GmbH</a>, used by <a href="https://dbschema.com">DbSchema Database Designer</a>.
 * Code modifications allowed only as pull requests to the <a href="https://github.com/wise-coders/mongodb-jdbc-driver">public GIT repository</a>.
 */

class AbstractTestCase {

    void printResultSet(ResultSet rs ) throws SQLException  {
        while ( rs != null && rs.next()){
            if ( rs.getMetaData().getColumnCount() == 1 && ( rs.getMetaData().getColumnType(1) == MetaObject.TYPE_ARRAY || rs.getMetaData().getColumnType(1) == MetaObject.TYPE_OBJECT ) ) {
                Object obj = rs.getObject(1);
                if (obj != null) {
                    try {
                        System.out.println(new Gson().toJson(obj));
                    } catch (Throwable ex) {
                        System.out.println(obj);
                    }
                }
            } else {
                for ( int c = 1; c <= rs.getMetaData().getColumnCount(); c++ ) {
                    System.out.print( rs.getMetaData().getColumnName(c) + " = "  + rs.getObject(c) + " ");
                }
                System.out.println("");
            }
        }
    }
}
