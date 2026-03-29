insert into users(id, email, password_hash, created_at) values
  (1, 'alice@example.com', '$2a$10$eB6hTgJ2qjW9Q1rGzFQvde1zEw0mA3mBvQGg6h5G6JfOZQm5kFzQy', now()),
  (2, 'bob@example.com',   '$2a$10$eB6hTgJ2qjW9Q1rGzFQvde1zEw0mA3mBvQGg6h5G6JfOZQm5kFzQy', now());

insert into profiles(user_id, display_name, bio, gender, distance_km, min_age, max_age)
values (1, 'Alice', 'Coffee and coding', 'F', 30, 25, 40),
       (2, 'Bob', 'Hiking on weekends', 'M', 50, 20, 35);
