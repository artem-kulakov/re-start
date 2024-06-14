create table user_lists (
  id serial primary key,
  user_id integer references users,
  list_id integer references lists,
  created_at timestamp
);
