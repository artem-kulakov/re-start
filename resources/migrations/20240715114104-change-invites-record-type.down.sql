CREATE EXTENSION hstore;
--;;
ALTER TABLE invites ALTER COLUMN record TYPE jsonb;
