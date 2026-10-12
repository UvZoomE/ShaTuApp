/*
 * SHATU: SHA-256 Tutor
 * 
 *  (C) Johanna & Richard Blumenthal, All rights reserved
 * 
 *  Unauthorized use, duplication or distribution without the authors'
 *  permission is strictly prohibited.
 * 
 *  Unless required by applicable law or agreed to in writing, this
 *  software is distributed on an "AS IS" basis without warranties
 *  or conditions of any kind, either expressed or implied.
 */
package edu.regis.shatu.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.regis.shatu.err.MissingPropertyException;
import edu.regis.shatu.err.NonRecoverableException;
import edu.regis.shatu.err.ObjNotFoundException;
import edu.regis.shatu.util.ResourceMgr;

/**
 * A root Data Access Object that provides some utility operations and ensures
 * the MySQL JDBC driver is loaded.
 * 
 * @author Rickb
 */
public abstract class MySqlDAO {
    /**
     * The host where MySQL resides (see /resources/ShaTu.Properties).
     */
    public static final String DB_HOST_PROP = "edu.regis.shatu.DB_HOST";

    /**
     * The host where MySQL resides (see /resources/ShaTu.Properties).
     */
    public static final String DB_PORT_PROP = "edu.regis.shatu.DB_PORT";

    /**
     * The name of the ShaTu database (see /resources/ShaTu.Properties).
     */
    public static final String DB_NAME_PROP = "edu.regis.shatu.DB_NAME";

    /**
     * The DB user used by ShaTu to login (see /resources/ShaTu.Properties).
     */
    public static final String DB_USER_PROP = "edu.regis.shatu.DB_USER";

    /**
     * The password used by ShaTu to login into the DB.
     */
    public static final String DB_PASS_PROP = "edu.regis.shatu.DB_PASS";

    /**
     * Fully qualified name of the MySql JDBC driver class.
     */
    public static String DRIVER = "com.mysql.cj.jdbc.Driver";

    public String table;

    public String primaryKey;

    /*
     * The URL used to obtain a JDBC connection.
     */
    public static String URL;

    private static final Logger LOGGER = Logger.getLogger(MySqlDAO.class.getName());

    /**
     * Utility indicating whether the DriverClass was explictly loaded (in
     * order to overcome errors in certain JVMs).
     */
    public static boolean IS_LOADED = false;

    /**
     * If it hasn't already been loaded, explicitly load the MySql driver.
     */
    public MySqlDAO(String table, String primaryKey) {
        this.table = table;
        this.primaryKey = primaryKey;
        if (!IS_LOADED) {
            try {
                ResourceMgr rscr = ResourceMgr.instance();

                String dbHost = rscr.getProp(DB_HOST_PROP);
                String dbPort = rscr.getProp(DB_PORT_PROP);
                String dbName = rscr.getProp(DB_NAME_PROP);
                String dbUser = rscr.getProp(DB_USER_PROP);
                String dbPass = rscr.getProp(DB_PASS_PROP);

                URL = String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", dbHost, dbPort, dbName, dbUser,
                        dbPass);

                // ToDo: Do we want to convert port to an int?
                // URL = String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s", dbHost,
                // dbPort, dbName, dbUser, dbPass);

                Class.forName(DRIVER); // Old School

                IS_LOADED = true;

            } catch (MissingPropertyException e) {
                LOGGER.log(Level.INFO, "Missing DB property: {0}", e.toString());
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "MySqlDao-ERR-1: Illegal driver class name {0}", e.toString());
            }
        }
    }
    
    public String getPrimaryKey() {
        return primaryKey;
    }

    /**
     * If the given connection or statement is open, close it, but log any
     * errors that might be thrown during the closing operations.
     *
     * @param conn An JDBC Connection that will to be closed.
     * @param stmt An JDBC Statement that will be closed.
     */
    protected void close(Connection conn, Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "MySqlDao-ERR-4: stmt.close() {0}", e.toString());
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "MySqlDao-ERR-5: close() {0}", e.toString());
            }
        }
    }

    /**
     * If the given connection is open, close it, but log any errors that occur
     * in attempting to close the connection.
     * 
     * @param conn
     */
    protected void close(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true); // Convenience
                conn.close();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "MySqlDao-ERR-6: close() {0}", e.toString());
            }
        }
    }

    /**
     * If the given statement is open, close it, but log any errors that occur
     * in attempting to close the connection.
     * 
     * @param stmt
     */
    protected void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "MySqlDao-ERR-7: close() {0}", e.toString());
            }
        }
    }

    /**
     * Rollback any statements made in the current transaction associated
     * with the given connection.
     * 
     * @param conn
     */
    protected void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "MySqlDao-ERR-8: rollback {0}", e.toString());
        }
    }

    /**
     * Deletes records from the DB on column value match
     * 
     * @param column the column to search on
     * @param key    the value to match
     * @throws NonRecoverableException
     */
    public void delete(String column, Object key) throws NonRecoverableException {
        String stmt = String.format("DELETE FROM %s WHERE %s = ?", this.table, column);

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement prepstmt = conn.prepareStatement(stmt)) {

            sqlTypeCoerce(key, prepstmt);

            prepstmt.executeUpdate();
        } catch (SQLException e) {
            throw new NonRecoverableException(this.table + "-DAO-ERR-3" + e.toString(), e);
        }
    }

    /**
     * Checks if an entry in the Database exists.
     * 
     * @param keyColumn the column to search on
     * @param keyValue  the value to match
     * @return boolean
     * @throws ObjNotFoundException
     * @throws NonRecoverableException
     */
    public boolean exists(String keyColumn, Object keyValue) throws NonRecoverableException {

        String stmt = String.format("SELECT %s FROM %s WHERE %s = ?", this.primaryKey, this.table, keyColumn);

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement prepstmt = conn.prepareStatement(stmt)) {

            sqlTypeCoerce(keyValue, prepstmt);

            ResultSet result = prepstmt.executeQuery();

            return result.next();

        } catch (SQLException e) {
            throw new NonRecoverableException(this.table + "-DAO-ERR-10" + e.toString(), e);
        }
    }

    /**
     * A not so great way to coerce the various types of columns.
     * only supports one param for now.
     * 
     * @param obj  the object to coerce
     * @param stmt the statement to coerce it into
     * @return PreparedStatment
     * @throws SQLException
     */
    static PreparedStatement sqlTypeCoerce(Object obj, PreparedStatement stmt) throws SQLException {
        System.out.println("Obj: " + obj);
        if (obj instanceof String) {
            System.out.println("HERE");
            stmt.setString(1, obj.toString());
        } else if (obj instanceof Integer) {
            stmt.setInt(1, (Integer) obj);
        } else if (obj instanceof Boolean) {
            stmt.setBoolean(1, (Boolean) obj);
        } else if (obj instanceof Timestamp) {
            stmt.setTimestamp(1, (Timestamp) obj);
        }

        return stmt;
    }
}
