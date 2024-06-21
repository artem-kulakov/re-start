ALTER TABLE items ADD COLUMN user_id integer references users;
--;;
ALTER TABLE items ADD COLUMN list_id integer references lists;
