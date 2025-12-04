-- Performance indexes for /articles/feed endpoint optimization
-- These indexes optimize the query: SELECT a.* FROM articles a JOIN follows f ON a.user_id = f.follow_id WHERE f.user_id = ? ORDER BY a.created_at DESC LIMIT 20

-- Index on follows.user_id for filtering by the current user
CREATE INDEX idx_follows_user_id ON follows(user_id);

-- Index on follows.follow_id for joining with articles.user_id
CREATE INDEX idx_follows_follow_id ON follows(follow_id);

-- Composite index on articles for user_id lookup and created_at ordering
CREATE INDEX idx_articles_user_id_created_at ON articles(user_id, created_at DESC);
