create table invites (
  id serial primary key,
  user_id integer references users not null,
  user_2_id integer references users not null,
  record jsonb not null,
  created_at timestamp not null default current_timestamp
);
