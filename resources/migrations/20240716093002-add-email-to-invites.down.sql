ALTER TABLE invites DROP COLUMN user_2_email;
--;;
ALTER TABLE invites ALTER COLUMN user_2_id SET NOT NULL;
