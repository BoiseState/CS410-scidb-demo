DROP TABLE IF EXISTS article_search;
CREATE TABLE article_search (
  -- article_search rows are always about specific article rows
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
