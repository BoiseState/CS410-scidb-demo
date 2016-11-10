DROP TABLE IF EXISTS article_search;
CREATE TABLE article_search (
  article_id INTEGER PRIMARY KEY REFERENCES article ON DELETE CASCADE,
  article_vector TSVECTOR NOT NULL,
  up2date BOOLEAN NOT NULL -- is the data current?
);

INSERT INTO article_search (article_id, article_vector, up2date)
  SELECT article_id,
    setweight(to_tsvector(title), 'A')
    || setweight(to_tsvector(coalesce(author_string, '')), 'A')
    || setweight(to_tsvector(coalesce(abstract, '')), 'B')
    || setweight(to_tsvector(coalesce(iss_title, '')), 'C')
    || setweight(to_tsvector(coalesce(pub_title, '')), 'C'),
    TRUE
  FROM article
    JOIN issue USING (issue_id)
    JOIN publication USING (pub_id)
    LEFT OUTER JOIN (SELECT article_id,
                       string_agg(author_name, ' ') AS author_string
                     FROM article_author
                       JOIN author USING (author_id)
                     GROUP BY article_id) aasum
    USING (article_id)
    LEFT OUTER JOIN article_search USING (article_id)
  WHERE article_vector IS NULL;

CREATE INDEX article_search_idx
ON article_search USING gin(article_vector);
ANALYZE article_search;

-- LIVE UPDATE CODE
CREATE OR REPLACE FUNCTION dirty_article() RETURNS trigger AS $dirty_article$
BEGIN
  IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
    -- we need to dirty the new article
    UPDATE article_search SET up2date = FALSE WHERE article_id = NEW.article_id;
  END IF;
  IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
    -- we need to dirty the old article
    UPDATE article_search SET up2date = FALSE WHERE article_id = OLD.article_id;
  END IF;
  RETURN NULL;
END;
$dirty_article$ LANGUAGE plpgsql;

CREATE TRIGGER dirty_article
AFTER UPDATE
ON article
FOR EACH ROW EXECUTE PROCEDURE dirty_article();

CREATE TRIGGER dirty_article_for_author
AFTER UPDATE OR INSERT OR DELETE
ON article_author
FOR EACH ROW EXECUTE PROCEDURE dirty_article();

-- PERIODIC UPDATE
INSERT INTO article_search (article_id, article_vector, up2date)
  SELECT article_id, to_tsvector(''), FALSE
  FROM article
    LEFT OUTER JOIN article_search USING (article_id)
  WHERE article_vector IS NULL;

UPDATE article_search srch
SET article_vector = setweight(to_tsvector(title), 'A')
    || setweight(to_tsvector(coalesce(author_string, '')), 'A')
    || setweight(to_tsvector(coalesce(abstract, '')), 'B')
    || setweight(to_tsvector(coalesce(iss_title, '')), 'C')
    || setweight(to_tsvector(coalesce(pub_title, '')), 'C'),
  up2date = TRUE
FROM article a
  JOIN issue USING (issue_id)
  JOIN publication USING (pub_id)
  LEFT OUTER JOIN (SELECT article_id,
                     string_agg(author_name, ' ') AS author_string
                   FROM article_author
                     JOIN author USING (author_id)
                   GROUP BY article_id) aasum
  USING (article_id)
WHERE srch.article_id = a.article_id AND NOT up2date;
