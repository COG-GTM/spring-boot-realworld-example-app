-- Add explicit index on email column for login query optimization
-- Note: The UNIQUE constraint already creates an implicit index, but this explicit index
-- ensures optimal query performance and provides better documentation
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Add index on username for profile lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
