create index idx_articles_created_at on articles (created_at);
create index idx_article_tags_article_id on article_tags (article_id);
create index idx_article_tags_tag_id on article_tags (tag_id);
