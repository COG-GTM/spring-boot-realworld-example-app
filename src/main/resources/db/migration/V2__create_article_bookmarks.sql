create table article_bookmarks (
  article_id varchar(255) not null,
  user_id varchar(255) not null,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  primary key(article_id, user_id)
);

create index idx_article_bookmarks_user_created on article_bookmarks (user_id, created_at);
