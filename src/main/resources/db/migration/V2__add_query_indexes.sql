create index idx_articles_created_at on articles (created_at);
create index idx_articles_user_id_created_at on articles (user_id, created_at);

create index idx_article_favorites_user_id on article_favorites (user_id, article_id);

create index idx_follows_user_id_follow_id on follows (user_id, follow_id);

create index idx_tags_name on tags (name);

create index idx_article_tags_article_id on article_tags (article_id, tag_id);
create index idx_article_tags_tag_id on article_tags (tag_id, article_id);

create index idx_comments_article_id_created_at on comments (article_id, created_at);
