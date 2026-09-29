-- Expected schema after all migrations have been applied. Regenerate this file
-- whenever a migration is added. See docs/database-migrations.md.

CREATE TABLE article_favorites (
  article_id varchar(255) not null,
  user_id varchar(255) not null,
  primary key(article_id, user_id)
);

CREATE TABLE article_tags (
  article_id varchar(255) not null,
  tag_id varchar(255) not null
);

CREATE TABLE articles (
  id varchar(255) primary key,
  user_id varchar(255),
  slug varchar(255) UNIQUE,
  title varchar(255),
  description text,
  body text,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comments (
  id varchar(255) primary key,
  body text,
  article_id varchar(255),
  user_id varchar(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE follows (
  user_id varchar(255) not null,
  follow_id varchar(255) not null
);

CREATE TABLE tags (
  id varchar(255) primary key,
  name varchar(255) not null
);

CREATE TABLE users (
  id varchar(255) primary key,
  username varchar(255) UNIQUE,
  password varchar(255),
  email varchar(255) UNIQUE,
  bio text,
  image varchar(511)
);

CREATE INDEX idx_article_favorites_user_id on article_favorites (user_id);

CREATE INDEX idx_article_tags_article_id on article_tags (article_id);

CREATE INDEX idx_article_tags_tag_id on article_tags (tag_id);

CREATE INDEX idx_articles_created_at on articles (created_at);

CREATE INDEX idx_articles_user_id on articles (user_id);

CREATE INDEX idx_comments_article_id_created_at on comments (article_id, created_at);

CREATE INDEX idx_follows_follow_id on follows (follow_id);

CREATE INDEX idx_follows_user_id_follow_id on follows (user_id, follow_id);

CREATE INDEX idx_tags_name on tags (name);
