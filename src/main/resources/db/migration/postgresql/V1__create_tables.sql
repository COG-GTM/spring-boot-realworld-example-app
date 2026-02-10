CREATE TABLE users (
  id VARCHAR(255) PRIMARY KEY,
  username VARCHAR(255) UNIQUE,
  password VARCHAR(255),
  email VARCHAR(255) UNIQUE,
  bio TEXT,
  image VARCHAR(511)
);

CREATE TABLE articles (
  id VARCHAR(255) PRIMARY KEY,
  user_id VARCHAR(255),
  slug VARCHAR(255) UNIQUE,
  title VARCHAR(255),
  description TEXT,
  body TEXT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE article_favorites (
  article_id VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  PRIMARY KEY(article_id, user_id)
);

CREATE TABLE follows (
  user_id VARCHAR(255) NOT NULL,
  follow_id VARCHAR(255) NOT NULL
);

CREATE TABLE tags (
  id VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE article_tags (
  article_id VARCHAR(255) NOT NULL,
  tag_id VARCHAR(255) NOT NULL
);

CREATE TABLE comments (
  id VARCHAR(255) PRIMARY KEY,
  body TEXT,
  article_id VARCHAR(255),
  user_id VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
