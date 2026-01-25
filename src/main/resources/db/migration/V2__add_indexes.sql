-- Add indexes for frequently queried columns to improve query performance

-- Index on articles.user_id for faster author lookups
CREATE INDEX IF NOT EXISTS idx_articles_user_id ON articles(user_id);

-- Index on articles.created_at for faster sorting and cursor-based pagination
CREATE INDEX IF NOT EXISTS idx_articles_created_at ON articles(created_at);

-- Index on articles.slug for faster slug lookups (already UNIQUE, but explicit index helps)
CREATE INDEX IF NOT EXISTS idx_articles_slug ON articles(slug);

-- Index on article_tags.article_id for faster tag lookups by article
CREATE INDEX IF NOT EXISTS idx_article_tags_article_id ON article_tags(article_id);

-- Index on article_favorites.article_id for faster favorite count queries
CREATE INDEX IF NOT EXISTS idx_article_favorites_article_id ON article_favorites(article_id);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_articles_user_created ON articles(user_id, created_at);

-- Index on article_favorites.user_id for faster user favorite lookups
CREATE INDEX IF NOT EXISTS idx_article_favorites_user_id ON article_favorites(user_id);

-- Index on follows for faster follower/following queries
CREATE INDEX IF NOT EXISTS idx_follows_user_id ON follows(user_id);
CREATE INDEX IF NOT EXISTS idx_follows_follow_id ON follows(follow_id);

-- Index on comments for faster article comment lookups
CREATE INDEX IF NOT EXISTS idx_comments_article_id ON comments(article_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
