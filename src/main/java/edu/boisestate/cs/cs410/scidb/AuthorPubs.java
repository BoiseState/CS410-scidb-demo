package edu.boisestate.cs.cs410.scidb;

import java.sql.*;

/**
 * Created by michaelekstrand on 9/28/2016.
 */
public class AuthorPubs {
    private static final String DB_URI = "jdbc:postgresql://localhost/scidb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "dbAcc3ss!";

    public static void main(String[] args) throws SQLException {
        int authorID;
        if (args.length > 0) {
            authorID = Integer.parseInt(args[0]);
        } else {
            throw new IllegalArgumentException("no search name provided");
        }
        try (Connection cxn = DriverManager.getConnection(DB_URI, DB_USER, DB_PASSWORD);
             PreparedStatement stmt =
                     cxn.prepareStatement("SELECT article_id, title " +
                             "FROM article JOIN article_author USING (article_id) " +
                             "WHERE author_id = ?")) {
            stmt.setInt(1, authorID);
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    System.out.format("%d: %s\n", results.getInt(1), results.getString(2));
                }
            }
        }
    }
}
