-- Baseline schema (Flyway version 1), captured from a database built by
-- V1__create_tables.sql. A pre-existing database must match this before it is
-- baselined. See docs/database-migrations.md.

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
