ALTER TABLE users ADD COLUMN token varchar(40);
--;;
ALTER TABLE users ADD COLUMN token_expiration timestamp;
