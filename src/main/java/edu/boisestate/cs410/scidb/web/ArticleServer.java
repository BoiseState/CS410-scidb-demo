package edu.boisestate.cs410.scidb.web;

import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.template.pebble.PebbleTemplateEngine;

import java.sql.*;
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
        engine = new PebbleTemplateEngine(new ClasspathLoader());

        http.get("/", this::rootPage, engine);
        http.get("/pubs/:pid/", this::pubPage, engine);
    }

    /**
     * View the root page with basic database info.
     */
    ModelAndView rootPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        try (Connection cxn = pool.getConnection()) {
            try(Statement stmt = cxn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS article_count FROM article")) {
                if (!rs.next()) {
                    throw new IllegalStateException("umm, no data?");
                }
                fields.put("articleCount", rs.getInt("article_count"));
            }
            fields.put("publications", PubListEntry.retrieve(cxn));
        }

        return new ModelAndView(fields, "home.html.twig");
    }

    public ModelAndView pubPage(Request request, Response response) throws SQLException {
        long pid = Long.parseLong(request.params("pid"));

        Map<String,Object> fields = new HashMap<>();

        try (Connection cxn = pool.getConnection()) {
            // look up the pub info
            try (PreparedStatement ps = cxn.prepareStatement("SELECT pub_title FROM publication WHERE pub_id = ?")) {
                ps.setLong(1, pid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        fields.put("title", rs.getString("pub_title"));
                    } else {
                        http.halt(404, "No such publication " + pid);
                    }
                }
            }

            // grab the issues
            try (PreparedStatement ps = cxn.prepareStatement("SELECT issue_id, iss_title, iss_volume, iss_number, iss_date FROM issue WHERE pub_id = ? ORDER BY iss_date")) {
                ps.setLong(1, pid);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Map<String,Object>> issues = new ArrayList<>();
                    while (rs.next()) {
                        Map<String,Object> issue = new HashMap<>();
                        issue.put("id", rs.getLong("issue_id"));
                        issue.put("title", rs.getString("iss_title"));
                        if (rs.wasNull()) {
                            issue.put("title", null);
                        }
                        issue.put("volume", rs.getInt("iss_volume"));
                        if (rs.wasNull()) {
                            issue.put("volume", null);
                        }
                        issue.put("number", rs.getInt("iss_number"));
                        if (rs.wasNull()) {
                            issue.put("number", null);
                        }
                        issue.put("date", rs.getDate("iss_date"));
                        issues.add(issue);
                    }
                    fields.put("issues", issues);
                }
            }
        }

        return new ModelAndView(fields, "pub.html.twig");
    }

}
