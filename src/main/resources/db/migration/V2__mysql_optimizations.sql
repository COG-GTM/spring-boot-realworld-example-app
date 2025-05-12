CREATE INDEX idx_articles_slug ON articles(slug);
CREATE INDEX idx_articles_user_id ON articles(user_id);
CREATE INDEX idx_comments_article_id ON comments(article_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_follows_user_id ON follows(user_id);
CREATE INDEX idx_follows_follow_id ON follows(follow_id);

ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SET GLOBAL transaction_isolation = 'READ-COMMITTED';
