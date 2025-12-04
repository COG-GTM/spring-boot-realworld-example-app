-- Add composite index on articles(user_id, created_at) to optimize the findUserFeed query
-- Query being optimized: SELECT a.* FROM articles a JOIN follows f ON a.user_id = f.follow_id WHERE f.user_id = ? ORDER BY a.created_at DESC LIMIT 20
-- This index helps with both the JOIN condition (user_id) and the ORDER BY clause (created_at DESC)

CREATE INDEX idx_articles_user_id_created_at ON articles (user_id, created_at DESC);
