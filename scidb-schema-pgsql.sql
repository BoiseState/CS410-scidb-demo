-- Version: 2015-10-21

DROP TABLE IF EXISTS institution CASCADE;
CREATE TABLE institution (
  inst_id SERIAL PRIMARY KEY,
  inst_name VARCHAR NOT NULL,
  inst_place VARCHAR);

DROP TABLE IF EXISTS author CASCADE;
CREATE TABLE author (
  author_id SERIAL PRIMARY KEY,
  author_name VARCHAR NOT NULL,
  current_inst_id INTEGER REFERENCES institution);

DROP TABLE IF EXISTS publication CASCADE;
CREATE TABLE publication (
  pub_id SERIAL PRIMARY KEY,
  pub_title VARCHAR NOT NULL,
  pub_hb_key VARCHAR UNIQUE);

DROP TABLE IF EXISTS issue CASCADE;
CREATE TABLE issue (
  issue_id SERIAL PRIMARY KEY,
  pub_id INTEGER NOT NULL REFERENCES publication,
  iss_volume INTEGER,
  iss_number INTEGER,
  iss_date DATE,
  iss_title VARCHAR,
  iss_hb_key VARCHAR UNIQUE);

DROP TABLE IF EXISTS article CASCADE;
CREATE TABLE article (
  article_id SERIAL PRIMARY KEY,
  title VARCHAR NOT NULL,
  abstract TEXT,
  issue_id INTEGER NOT NULL REFERENCES issue,
  article_hb_key VARCHAR UNIQUE);

DROP TABLE IF EXISTS article_author CASCADE;
CREATE TABLE article_author (
  article_id INTEGER NOT NULL REFERENCES article,
  author_id INTEGER NOT NULL REFERENCES author,
  position INTEGER NOT NULL DEFAULT 0,
  inst_id INTEGER REFERENCES institution,
  PRIMARY KEY (article_id, position));
CREATE INDEX article_author_article_idx ON article_author (article_id);
CREATE INDEX article_author_author_idx ON article_author (author_id);
