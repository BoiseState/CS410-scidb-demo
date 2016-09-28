package edu.boisestate.cs.cs410.scidb;

import java.sql.*;

/**
 * Created by michaelekstrand on 9/28/2016.
 */
public class SciDBStats {
    private static final String DB_URI = "jdbc:postgresql://localhost/scidb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "dbAcc3ss!";

    public static void main(String[] args) throws SQLException {
        try (Connection cxn = DriverManager.getConnection(DB_URI, DB_USER, DB_PASSWORD);
             Statement stmt = cxn.createStatement()) {
            stmt.execute("SELECT COUNT(article_id) FROM article");
            try (ResultSet results = stmt.getResultSet()) {
                if (!results.next()) {
                    throw new RuntimeException("uhh, no results?");
                }
                System.out.format("we have %d articles\n", results.getInt(1));
            }
        }
    }
}
