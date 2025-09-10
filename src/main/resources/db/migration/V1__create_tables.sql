create table users (
  id varchar(255) primary key,
  username varchar(255) UNIQUE NOT NULL,
  password varchar(255) NOT NULL,
  email varchar(255) UNIQUE NOT NULL,
  bio text,
  image varchar(511)
);

create table articles (
  id varchar(255) primary key,
  user_id varchar(255) NOT NULL,
  slug varchar(255) UNIQUE NOT NULL,
  title varchar(255) NOT NULL,
  description text,
  body text NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

create table article_favorites (
  article_id varchar(255) not null,
  user_id varchar(255) not null,
  primary key(article_id, user_id),
  FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

create table follows (
  user_id varchar(255) not null,
  follow_id varchar(255) not null,
  PRIMARY KEY (user_id, follow_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (follow_id) REFERENCES users(id) ON DELETE CASCADE
);

create table tags (
  id varchar(255) primary key,
  name varchar(255) not null UNIQUE
);

create table article_tags (
  article_id varchar(255) not null,
  tag_id varchar(255) not null,
  PRIMARY KEY (article_id, tag_id),
  FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

create table comments (
  id varchar(255) primary key,
  body text NOT NULL,
  article_id varchar(255) NOT NULL,
  user_id varchar(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
