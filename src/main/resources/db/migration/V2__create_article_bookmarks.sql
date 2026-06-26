create table article_bookmarks (
  article_id varchar(255) not null,
  user_id varchar(255) not null,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  primary key(article_id, user_id)
);
