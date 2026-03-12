-- Replace single file_url in assignments with array of file paths
ALTER TABLE assignments DROP COLUMN IF EXISTS file_url;
ALTER TABLE assignments ADD COLUMN file_paths TEXT[] NOT NULL DEFAULT '{}';

-- Replace single file_path in submissions with array of file paths
ALTER TABLE submissions RENAME COLUMN file_path TO file_path_old;
ALTER TABLE submissions ADD COLUMN file_paths TEXT[] NOT NULL DEFAULT '{}';
UPDATE submissions SET file_paths = ARRAY[file_path_old] WHERE file_path_old IS NOT NULL;
ALTER TABLE submissions DROP COLUMN file_path_old;
