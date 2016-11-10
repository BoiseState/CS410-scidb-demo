DROP TABLE IF EXISTS article_search;
CREATE TABLE article_search (
  article_id INTEGER PRIMARY KEY REFERENCES article ON DELETE CASCADE,
  article_vector TSVECTOR NOT NULL
);

INSERT INTO article_search (article_id, article_vector)
  SELECT article_id,
    setweight(to_tsvector(title), 'A')
    || setweight(to_tsvector(coalesce(author_string, '')), 'A')
    || setweight(to_tsvector(coalesce(abstract, '')), 'B')
    || setweight(to_tsvector(coalesce(iss_title, '')), 'C')
    || setweight(to_tsvector(coalesce(pub_title, '')), 'C')
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

-- Really Live Update
CREATE OR REPLACE FUNCTION update_article_search() RETURNS trigger AS $update_search$
DECLARE
  -- assume that it exists, and should update
  should_insert BOOLEAN := FALSE;
  aids INTEGER[] = array[]::integer[];
  aarec RECORD;
BEGIN
  -- Step 1: figure out out the article and action
  IF TG_TABLE_NAME = 'article' THEN
    IF TG_OP = 'INSERT' THEN
      aids := array[NEW.article_id]::integer[];
      should_insert := TRUE; -- no, insert
    ELSEIF TG_OP = 'UPDATE' THEN
      aids := array[NEW.article_id]::integer[];
    ELSE
      -- never happen b/c we don't set up that trigger
      RAISE 'invalid op on article';
    END IF;
  ELSEIF TG_TABLE_NAME = 'article_author' THEN
    -- we have inserted, added, or removed an article author
    -- affect all IDs
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
      -- we need to dirty the new article
      aids := array[NEW.article_id]::integer[];
    END IF;
    IF TG_OP = 'DELETE' OR TG_OP = 'UPDATE' THEN
      -- we need to dirty the old article
      aids := aids || OLD.article_id;
    END IF;
  ELSEIF TG_TABLE_NAME = 'author' THEN
    -- delete already handled by cascade
    -- insert is irrelevant (no links to papers yet)
    -- so we just about updates, so we have old & new
    FOR aarec IN
    SELECT DISTINCT article_id FROM article_author
    WHERE author_id = NEW.author_id OR author_id = OLD.author_id
    LOOP
      aids := aids || aarec.article_id;
    END LOOP;
  END IF;

  -- We now have: aids (the article IDs), and should_insert (the mode to use)
  -- Step 2: perform the query
  IF should_insert THEN
    INSERT INTO article_search (article_id, article_vector)
      SELECT article_id, setweight(to_tsvector(title), 'A')
                         || setweight(to_tsvector(coalesce(author_string, '')), 'A')
                         || setweight(to_tsvector(coalesce(abstract, '')), 'B')
                         || setweight(to_tsvector(coalesce(iss_title, '')), 'C')
                         || setweight(to_tsvector(coalesce(pub_title, '')), 'C')
      FROM article a
        JOIN issue USING (issue_id)
        JOIN publication USING (pub_id)
        LEFT OUTER JOIN (SELECT article_id,
                           string_agg(author_name, ' ') AS author_string
                         FROM article_author
                           JOIN author USING (author_id)
                         GROUP BY article_id) aasum
        USING (article_id)
      WHERE article_id = ANY(aids);
  ELSE
    UPDATE article_search srch
    SET article_vector = setweight(to_tsvector(title), 'A')
                         || setweight(to_tsvector(coalesce(author_string, '')), 'A')
                         || setweight(to_tsvector(coalesce(abstract, '')), 'B')
                         || setweight(to_tsvector(coalesce(iss_title, '')), 'C')
                         || setweight(to_tsvector(coalesce(pub_title, '')), 'C')
    FROM article a
      JOIN issue USING (issue_id)
      JOIN publication USING (pub_id)
      LEFT OUTER JOIN (SELECT article_id,
                         string_agg(author_name, ' ') AS author_string
                       FROM article_author
                         JOIN author USING (author_id)
                       GROUP BY article_id) aasum
      USING (article_id)
    WHERE srch.article_id = a.article_id AND srch.article_id = ANY(aids);
  END IF;
  RETURN NULL;
END;
$update_search$ LANGUAGE plpgsql;

-- for article, we care about updates and inserts
-- deletes are handled by the foreign key cascade
CREATE TRIGGER update_article
AFTER UPDATE OR INSERT
ON article
FOR EACH ROW EXECUTE PROCEDURE update_article_search();

-- for article_author, we care about anything, as that affects author list
CREATE TRIGGER update_article_for_author_link
AFTER UPDATE OR INSERT OR DELETE
ON article_author
FOR EACH ROW EXECUTE PROCEDURE update_article_search();

-- for author, we only care about updates
-- inserts aren't linked to authors, we'll pick up on the article_author insert
-- deletes can't happen without first deleting article_author entries
CREATE TRIGGER update_article_for_author_link
AFTER UPDATE
ON author
FOR EACH ROW EXECUTE PROCEDURE update_article_search();
