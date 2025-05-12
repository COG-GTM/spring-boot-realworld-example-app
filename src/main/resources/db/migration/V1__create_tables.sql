create table users (
  id varchar(255) primary key,
  username varchar(255) UNIQUE,
  password varchar(255),
  email varchar(255) UNIQUE,
  bio longtext,
  image varchar(511)
);

create table articles (
  id varchar(255) primary key,
  user_id varchar(255),
  slug varchar(255) UNIQUE,
  title varchar(255),
  description longtext,
  body longtext,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

create table article_favorites (
  article_id varchar(255) not null,
  user_id varchar(255) not null,
  primary key(article_id, user_id),
  FOREIGN KEY (article_id) REFERENCES articles(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

create table follows (
  user_id varchar(255) not null,
  follow_id varchar(255) not null,
  primary key(user_id, follow_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (follow_id) REFERENCES users(id)
);

create table tags (
  id varchar(255) primary key,
  name varchar(255) not null
);

create table article_tags (
  article_id varchar(255) not null,
  tag_id varchar(255) not null,
  primary key(article_id, tag_id),
  FOREIGN KEY (article_id) REFERENCES articles(id),
  FOREIGN KEY (tag_id) REFERENCES tags(id)
);

create table comments (
  id varchar(255) primary key,
  body longtext,
  article_id varchar(255),
  user_id varchar(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (article_id) REFERENCES articles(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
