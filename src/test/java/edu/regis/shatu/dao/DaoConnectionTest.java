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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.regis.shatu.err.NonRecoverableException;
import edu.regis.shatu.err.ObjNotFoundException;

/**
 * Checks that the DAOs close every database connection they open (issue #255).
 *
 * No database is needed. MySqlDAO.URL is pointed at a fake JDBC driver that
 * hands out recording connections, so each test can count how many
 * connections were opened and whether each one was closed.
 */
@DisplayName("DAO connection handling tests")
public class DaoConnectionTest {

    private static final String FAKE_URL = "jdbc:shatu-test:db";

    private static FakeDriver driver;
    private static String savedUrl;

    private ProblemDAO dao;
    private Recorder conn;
    private Recorder stmt;
    private Recorder rs;

    @BeforeAll
    static void registerFakeDriver() throws SQLException {
        driver = new FakeDriver();
        DriverManager.registerDriver(driver);
    }

    @AfterAll
    static void deregisterFakeDriver() throws SQLException {
        DriverManager.deregisterDriver(driver);
        MySqlDAO.URL = savedUrl;
    }

    @BeforeEach
    void setUp() {
        dao = new ProblemDAO();

        if (savedUrl == null) {
            savedUrl = MySqlDAO.URL;
        }
        MySqlDAO.URL = FAKE_URL;

        rs = new Recorder();
        stmt = new Recorder();
        stmt.returns.put("executeQuery", rs.as(ResultSet.class));
        conn = new Recorder();
        conn.returns.put("prepareStatement", stmt.as(PreparedStatement.class));

        driver.opened = 0;
        driver.next = conn.as(Connection.class);
    }

    @Test
    @DisplayName("retrieve(id, conn) uses the connection it is given")
    void retrieveUsesGivenConnection() {
        Recorder given = new Recorder();
        given.returns.put("prepareStatement", stmt.as(PreparedStatement.class));

        assertThrows(ObjNotFoundException.class, () -> dao.retrieve(999, given.as(Connection.class)));

        assertEquals(0, driver.opened, "should not open a new connection");
        assertEquals(1, given.count("prepareStatement"));
        assertEquals(0, given.count("close"), "the caller owns the connection");
        assertEquals(1, stmt.count("close"));
    }

    @Test
    @DisplayName("retrieve(id) opens one connection and closes it")
    void retrieveOpensAndClosesOneConnection() {
        assertThrows(ObjNotFoundException.class, () -> dao.retrieve(999));

        assertEquals(1, driver.opened);
        assertEquals(1, conn.count("close"));
    }

    @Test
    @DisplayName("delete() closes its connection")
    void deleteClosesConnection() throws Exception {
        dao.delete("Id", 5);

        assertEquals(1, driver.opened);
        assertEquals(1, conn.count("close"));
        assertEquals(1, stmt.count("close"));
    }

    @Test
    @DisplayName("exists() closes its connection")
    void existsClosesConnection() throws Exception {
        rs.returns.put("next", (Supplier<Object>) () -> true);

        assertTrue(dao.exists("Id", 5));

        assertEquals(1, driver.opened);
        assertEquals(1, conn.count("close"));
        assertEquals(1, stmt.count("close"));
    }

    @Test
    @DisplayName("exists() closes its connection when the query fails")
    void existsClosesConnectionOnError() {
        conn.returns.put("prepareStatement", new SQLException("simulated failure"));

        assertThrows(NonRecoverableException.class, () -> dao.exists("Id", 5));

        assertEquals(1, driver.opened);
        assertEquals(1, conn.count("close"));
    }

    /**
     * Stands in for any JDBC interface and records which methods were called.
     * A stored Throwable is thrown, a stored Supplier is called, anything else
     * is returned as is. Unlisted methods return false, zero or null.
     */
    private static class Recorder implements InvocationHandler {
        final List<String> calls = new ArrayList<>();
        final Map<String, Object> returns = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            calls.add(method.getName());

            Object value = returns.get(method.getName());
            if (value instanceof Throwable t) {
                throw t;
            }
            if (value instanceof Supplier<?> s) {
                return s.get();
            }
            if (value != null) {
                return value;
            }

            Class<?> type = method.getReturnType();
            if (type == boolean.class) {
                return false;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            return null;
        }

        int count(String methodName) {
            return (int) calls.stream().filter(methodName::equals).count();
        }

        <T> T as(Class<T> type) {
            return type.cast(Proxy.newProxyInstance(
                    DaoConnectionTest.class.getClassLoader(), new Class<?>[] {type}, this));
        }
    }

    /** A JDBC driver for FAKE_URL that hands out the test's connection. */
    private static class FakeDriver implements Driver {
        Connection next;
        int opened;

        @Override
        public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) {
                return null;
            }
            opened++;
            return next;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:shatu-test:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }
}
