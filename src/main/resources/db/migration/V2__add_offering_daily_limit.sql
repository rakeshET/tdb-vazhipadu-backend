ALTER TABLE offering ADD COLUMN daily_limit INTEGER CHECK (daily_limit > 0);
