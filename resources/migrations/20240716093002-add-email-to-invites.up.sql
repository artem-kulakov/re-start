ALTER TABLE invites ADD COLUMN user_2_email varchar(30);
--;;
ALTER TABLE invites ALTER COLUMN user_2_id DROP NOT NULL;
