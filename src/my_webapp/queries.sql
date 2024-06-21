-- USERS --

-- :name create-user! :! :n
-- :doc creates a new user
INSERT INTO users
(name, password)
VALUES (:name, :password);

-- :name get-user :? :1
-- :doc get user
SELECT id, password
FROM users
WHERE name = :name;

-- LISTS --

-- :name create-list! :! :n
-- :doc creates a new list for the user
WITH rows AS (
  INSERT INTO lists
  (name)
  VALUES
  (:name)
  RETURNING id
)
INSERT INTO user_lists
(user_id, list_id)
VALUES
(:user-id, (SELECT id FROM rows));

-- :name get-lists :? :*
-- :doc get all user lists
SELECT lists.id, name
FROM lists
  INNER JOIN user_lists
  ON lists.id = user_lists.list_id
WHERE user_id = :user-id;

-- :name get-list :? :1
-- :doc get user list
SELECT lists.id, name
FROM lists
  INNER JOIN user_lists
  ON lists.id = user_lists.list_id
WHERE user_id = :user-id AND lists.id = :id;

-- :name get-list-items :? :*
-- :doc get list items
SELECT id, name, complete
FROM items
WHERE list_id = :id
ORDER BY sort DESC;

-- ITEMS --

-- :name create-item! :! :n
-- :doc creates a new list item
INSERT INTO items
(name, user_id, list_id, sort)
VALUES (
  :name,
  :user-id,
  :list-id,
  (SELECT MAX(sort) FROM items) + 1
);

-- :name update-item-complete! :! :n
-- :doc updates the item's "complete" field
UPDATE items
SET complete = :complete
WHERE id = :id

-- :name sort-list-items! :! :n
-- :doc sorts the items of the list and updates the values of the "sort" column
WITH sorted_items AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY complete DESC, sort ASC) AS new_sort
    FROM items
    WHERE list_id = :id
  )
  UPDATE items
  SET sort = sorted_items.new_sort
  FROM sorted_items
  WHERE sorted_items.id = items.id;
