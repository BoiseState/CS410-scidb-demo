package edu.boisestate.cs410.scidb.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.TemplateEngine;
import spark.template.pebble.PebbleTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;

/**
 * Main class for the SciDB web application.
 */
public class WebMain {
    private static final Logger logger = LoggerFactory.getLogger(WebMain.class);

    private static final TemplateEngine TEMPLATE_ENGINE = new PebbleTemplateEngine();

    public static void main(String[] args) {
        get("/", (req, res) -> {
            Map<String,Object> fields = new HashMap<>();

            return new ModelAndView(fields, "base.html");
        }, TEMPLATE_ENGINE);

        logger.info("web app initialized and running");
    }
}
