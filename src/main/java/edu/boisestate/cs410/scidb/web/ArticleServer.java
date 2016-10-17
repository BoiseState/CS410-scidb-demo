package edu.boisestate.cs410.scidb.web;

import org.apache.commons.dbcp2.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.template.pebble.PebbleTemplateEngine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server for the charity database.
 */
public class ArticleServer {
    private static final Logger logger = LoggerFactory.getLogger(ArticleServer.class);

    private final PoolingDataSource<? extends Connection> pool;
    private final Service http;
    private final TemplateEngine engine;

    public ArticleServer(PoolingDataSource<? extends Connection> pds, Service svc) {
        pool = pds;
        http = svc;
        engine = new PebbleTemplateEngine();

        http.get("/", this::rootPage, engine);
    }

    ModelAndView rootPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        try (Connection cxn = pool.getConnection();
             Statement stmt = cxn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS article_count FROM article")) {
                if (!rs.next()) {
                    throw new IllegalStateException("umm, no data?");
                }
                int count = rs.getInt("article_count");
                fields.put("articleCount", count);
            }

            List<TitleListEntry> titles = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SELECT pub_id, pub_title, \n" +
                    "  COUNT(DISTINCT issue_id) AS issue_count,\n" +
                    "  COUNT(article_id) AS article_count\n" +
                    "FROM publication\n" +
                    "JOIN issue USING (pub_id)\n" +
                    "JOIN article USING (issue_id)\n" +
                    "GROUP BY pub_id, pub_title \n" +
                    "ORDER BY pub_title;")) {
                while (rs.next()) {
                    // put result in a list
                    titles.add(new TitleListEntry(
                            rs.getLong("pub_id"),
                            rs.getString("pub_title"),
                            rs.getInt("issue_count"),
                            rs.getInt("article_count")));
                }
            }
            logger.info("retrieved {} titles", titles.size());
            fields.put("publications", titles);
        }

        return new ModelAndView(fields, "base.html");
    }

    static class TitleListEntry {
        final long pubId;
        final String pubTitle;
        final int issueCount;
        final int articleCount;

        public TitleListEntry(long id, String title, int issues, int articles) {
            pubId = id;
            pubTitle = title;
            issueCount = issues;
            articleCount = articles;
        }

        public long getPubId() {
            return pubId;
        }

        public String getPubTitle() {
            return pubTitle;
        }

        public int getIssueCount() {
            return issueCount;
        }

        public int getArticleCount() {
            return articleCount;
        }
    }
}
