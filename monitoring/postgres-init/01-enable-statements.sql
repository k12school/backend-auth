-- Enable pg_stat_statements extension for query performance monitoring
-- This runs automatically when the database is initialized

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Grant access to k12_user
GRANT pg_read_all_stats TO k12_user;

-- Configure pg_stat_statements (track all queries)
ALTER SYSTEM SET pg_stat_statements.track = all;
ALTER SYSTEM SET pg_stat_statements.max = 10000;

-- Log slow queries (optional)
ALTER SYSTEM SET log_min_duration_statement = 1000; -- Log queries taking > 1s
