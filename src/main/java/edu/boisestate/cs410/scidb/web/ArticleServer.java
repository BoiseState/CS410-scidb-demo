package edu.boisestate.cs410.scidb.web;

import com.google.common.collect.ImmutableMap;
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
        http.get("/pubs/:pid", this::redirectToFolder);
        http.get("/pubs/:pid/", this::pubPage, engine);
        http.get("/pubs/:pid/issues/:iid", this::redirectToFolder);
        http.get("/pubs/:pid/issues/:iid/", this::issuePage, engine);
        http.get("/articles/:aid", this::articlePage, engine);
    }

    public String redirectToFolder(Request request, Response response) {
        String path = request.pathInfo();
        response.redirect(path + "/", 301);
        return "Redirecting to " + path + "/";
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
        fields.put("pubId", pid);

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

    public ModelAndView issuePage(Request request, Response response) throws SQLException {
        long pub_id = Long.parseLong(request.params("pid"));
        long iss_id = Long.parseLong(request.params("iid"));

        Map<String,Object> fields = new HashMap<>();

        try (Connection cxn = pool.getConnection()) {
            // look up the issue info
            try (PreparedStatement ps = cxn.prepareStatement("SELECT iss_title, pub_title, iss_volume, iss_number, iss_date " +
                                                                     "FROM issue JOIN publication USING (pub_id) " +
                                                                     "WHERE issue_id = ? AND pub_id = ?")) {
                ps.setLong(1, iss_id);
                ps.setLong(2, pub_id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        fields.put("title", rs.getString("iss_title"));
                        Map<String, Object> pub = new HashMap<>();
                        pub.put("id", pub_id);
                        pub.put("title", rs.getString("pub_title"));
                        fields.put("pub", pub);
                        fields.put("volume", rs.getInt("iss_volume"));
                        if (rs.wasNull()) {
                            fields.put("volume", null);
                        }
                        fields.put("number", rs.getInt("iss_number"));
                        if (rs.wasNull()) {
                            fields.put("number", null);
                        }
                        fields.put("date", rs.getDate("iss_date"));
                    } else {
                        http.halt(404, "No such issue " + iss_id + " in publication " + pub_id);
                    }
                }
            }

            try (PreparedStatement ps = cxn.prepareStatement("SELECT article_id, title " +
                                                                     "FROM article " +
                                                                     "WHERE issue_id = ?")) {
                ps.setLong(1, iss_id);
                List<Map<String,Object>> articles = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        articles.add(ImmutableMap.<String,Object>builder()
                                                 .put("id", rs.getLong("article_id"))
                                                 .put("title", rs.getString("title"))
                                                 .build());
                    }
                }
                fields.put("articles", articles);
            }
        }

        return new ModelAndView(fields, "issue.html.twig");
    }

    public ModelAndView articlePage(Request request, Response response) throws SQLException {
        long aid = Long.parseLong(request.params("aid"));

        Map<String,Object> fields = new HashMap<>();
        fields.put("id", aid);

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement("SELECT title, iss_date, abstract FROM article JOIN issue USING (issue_id) WHERE article_id = ?")) {
                ps.setLong(1, aid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        fields.put("title", rs.getString("title"));
                        fields.put("abstract", rs.getString("abstract"));
                        fields.put("date", rs.getDate("iss_date"));
                    } else {
                        http.halt(404, "No such article " + aid);
                    }
                }
            }

            try (PreparedStatement ps = cxn.prepareStatement("SELECT author_name FROM author JOIN article_author USING (author_id) WHERE article_id = ? ORDER BY position")) {
                ps.setLong(1, aid);
                List<String> authors = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        authors.add(rs.getString("author_name"));
                    }
                }
                fields.put("authors", authors);
            }
        }

        return new ModelAndView(fields, "article.html.twig");
    }
}
