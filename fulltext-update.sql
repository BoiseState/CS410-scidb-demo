-- MARK UPDATED STUFF AS DIRTY
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
-- Create records for new articles
INSERT INTO article_search (article_id, article_vector, up2date)
  SELECT article_id, to_tsvector(''), FALSE
  FROM article
    LEFT OUTER JOIN article_search USING (article_id)
  WHERE article_vector IS NULL;

-- Update all out-of-date articles
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

ANALYZE article_search;