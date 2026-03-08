ALTER TABLE users
    ADD COLUMN username VARCHAR(30);

UPDATE users
SET username = 'user_' || SUBSTRING(id, 1, 8)
WHERE username IS NULL;

-- H2 does not support partial indexes; uniqueness enforced at application layer in tests
