-- Minimal seed (2 users). For 10 rows per table see: scripts/sample_data_10_each.sql
-- Login for both: password = "password"

insert into users(id, email, password_hash, created_at) values
  (1, 'alice@example.com', '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now()),
  (2, 'bob@example.com',   '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now());

insert into profiles(user_id, display_name, bio, gender, distance_km, min_age, max_age)
values (1, 'Alice', 'Coffee and coding', 'F', 30, 25, 40),
       (2, 'Bob', 'Hiking on weekends', 'M', 50, 20, 35);
