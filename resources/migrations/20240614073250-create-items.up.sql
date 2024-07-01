CREATE TABLE items (
  id serial primary key,
  name varchar(30) not null,
  complete boolean default false,
  sort real,
  created_at timestamp
);
