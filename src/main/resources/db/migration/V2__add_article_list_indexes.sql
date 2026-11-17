create index if not exists idx_article_tags_tag_id on article_tags (tag_id);
create index if not exists idx_article_favorites_user_id on article_favorites (user_id, article_id);
create index if not exists idx_follows_user_id on follows (user_id, follow_id);
