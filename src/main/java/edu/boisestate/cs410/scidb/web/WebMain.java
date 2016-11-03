package edu.boisestate.cs410.scidb.web;

import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Main class for the Charity Donors web application.
 */
public class WebMain {
    private static final Logger logger = LoggerFactory.getLogger(WebMain.class);

    public static void main(String[] args) throws URISyntaxException {
        if (args.length == 0) {
            logger.error("no database URI specified");
            logger.info("provide a database URI as a command line argument");
            logger.info("expected URI format: postgresql://user:password@localhost/scidb");
            throw new IllegalArgumentException("no URI specified");
        }

        // PostgreSQL driver doesn't know how to get user & password from URI
        // So we hand-mangle that.
        URI dburi = URI.create(args[0]);
        String auth = dburi.getUserInfo();
        String user = null, password = null;
        if (auth != null) {
            String[] parts = auth.split(":", 2);
            user = parts[0];
            if (parts.length > 1) {
                password = parts[1];
            }
            dburi = new URI(dburi.getScheme(), null, dburi.getHost(), dburi.getPort(), dburi.getPath(), dburi.getQuery(), dburi.getFragment());
        }

        // Set up the database pool.
        logger.info("using database URI {}", dburi);
        ConnectionFactory cxnFac = new DriverManagerConnectionFactory("jdbc:" + dburi.toString(), user, password);
        PoolableConnectionFactory pFac = new PoolableConnectionFactory(cxnFac, null);
        GenericObjectPool<PoolableConnection> objPool = new GenericObjectPool<>(pFac);

        PoolingDataSource<PoolableConnection> source = new PoolingDataSource<>(objPool);

        Service http = Service.ignite();

        ArticleServer server = new ArticleServer(source, http);

        http.exception(Exception.class, (exception, request, response) -> {
            logger.error("request handler failed", exception);
            response.status(500);
            response.type("text/plain");
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.println("Internal server error");
            print.format("Request url: %s\n", request.url());
            exception.printStackTrace(print);
            response.body(writer.toString());
        });

        logger.info("web app initialized and running");
    }
}
