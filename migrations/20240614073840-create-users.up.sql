CREATE TABLE users (
  id serial primary key,
  name varchar(30) not null,
  password varchar(100) not null,
  created_at timestamp
);
