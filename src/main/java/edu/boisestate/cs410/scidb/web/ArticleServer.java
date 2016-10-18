package edu.boisestate.cs410.scidb.web;

import com.mitchellbosecke.pebble.loader.ClasspathLoader;
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

        return new ModelAndView(fields, "home.html");
    }

    public ModelAndView pubPage(Request request, Response response) {
        return null;
    }

}
