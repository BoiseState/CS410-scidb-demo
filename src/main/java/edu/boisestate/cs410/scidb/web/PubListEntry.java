package edu.boisestate.cs410.scidb.web;

import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by michaelekstrand on 10/18/2016.
 */
class PubListEntry {
    public static final Logger logger = LoggerFactory.getLogger(PubListEntry.class);
    final long pubId;
    final String pubTitle;
    final int issueCount;
    final int articleCount;

    public PubListEntry(long id, String title, int issues, int articles) {
        pubId = id;
        pubTitle = title;
        issueCount = issues;
        articleCount = articles;
    }

    public static List<PubListEntry> retrieve(Connection cxn) throws SQLException {
        List<PubListEntry> publications = new ArrayList<>();
        try (Statement stmt = cxn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT pub_id, pub_title, \n" +
                                                      "  COUNT(DISTINCT issue_id) AS issue_count,\n" +
                                                      "  COUNT(article_id) AS article_count\n" +
                                                      "FROM publication\n" +
                                                      "JOIN issue USING (pub_id)\n" +
                                                      "JOIN article USING (issue_id)\n" +
                                                      "GROUP BY pub_id, pub_title \n" +
                                                      "ORDER BY pub_title;")) {
            while (rs.next()) {
                // put result in a list
                publications.add(new PubListEntry(
                        rs.getLong("pub_id"),
                        rs.getString("pub_title"),
                        rs.getInt("issue_count"),
                        rs.getInt("article_count")));
            }
        }
        logger.info("retrieved {} titles", publications.size());
        return publications;
    }

    public long getId() {
        return pubId;
    }

    public String getTitle() {
        return pubTitle;
    }

    public int getIssueCount() {
        return issueCount;
    }

    public int getArticleCount() {
        return articleCount;
    }
}
