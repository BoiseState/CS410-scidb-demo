package edu.boisestate.cs410.scidb.cli;

import java.sql.*;

/**
 * Created by michaelekstrand on 9/28/2016.
 */
public class AuthorSearch {
    private static final String DB_URI = "jdbc:postgresql://localhost/scidb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "dbAcc3ss!";

    public static void main(String[] args) throws SQLException {
        String name;
        if (args.length > 0) {
            name = args[0];
        } else {
            throw new IllegalArgumentException("no search name provided");
        }
        try (Connection cxn = DriverManager.getConnection(DB_URI, DB_USER, DB_PASSWORD);
             PreparedStatement stmt =
                     cxn.prepareStatement("SELECT author_id, author_name FROM author WHERE author_name LIKE ?")) {
            stmt.setString(1, "%" + name + "%");
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    System.out.format("%d: %s\n", results.getInt(1), results.getString(2));
                }
            }
        }
    }
}
