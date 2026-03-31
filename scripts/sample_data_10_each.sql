-- =============================================================================
-- 10 sample rows per table — run AFTER recreate-schema.sql (or empty schema)
-- =============================================================================
-- LOGIN: every seeded user uses the SAME password:
--   Email:    see users rows below (e.g. ava@test.local … jack@test.local)
--   Password: password
--
-- Bcrypt below matches plaintext "password" (Spring / BCrypt $2b$ compatible).
--
--   psql -U DatingAppDB -d DatingAppDB -f scripts/sample_data_10_each.sql
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- users (10)
-- -----------------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, created_at, last_login) VALUES
  (1,  'ava@test.local',       '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '30 days', now() - interval '1 day'),
  (2,  'ben@test.local',       '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '28 days', now() - interval '2 days'),
  (3,  'chloe@test.local',     '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '25 days', null),
  (4,  'dan@test.local',       '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '20 days', now() - interval '3 hours'),
  (5,  'emma@test.local',      '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '18 days', now() - interval '5 days'),
  (6,  'finn@test.local',      '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '15 days', null),
  (7,  'gia@test.local',       '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '12 days', now() - interval '1 hour'),
  (8,  'hugo@test.local',      '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '10 days', null),
  (9,  'ivy@test.local',       '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '7 days',  now() - interval '12 hours'),
  (10, 'jack@test.local',      '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW', now() - interval '5 days',  now() - interval '2 days');

-- -----------------------------------------------------------------------------
-- profiles (10) — user_id PK
-- -----------------------------------------------------------------------------
INSERT INTO profiles (
  user_id, display_name, bio, gender, birthday, interests, latitude, longitude,
  min_age, max_age, distance_km, city, education, occupation, hobbies, photo_url, is_premium
) VALUES
  (1,  'Ava',    'Espresso, climbing gyms, and weekend hikes.', 'F', '1996-04-12', '["climbing","coffee","travel"]'::jsonb, 35.6812, 139.7671, 24, 38, 25, 'Tokyo', 'BA Design', 'Product designer', 'Pottery, film cameras', 'https://picsum.photos/seed/ava/400/600', false),
  (2,  'Ben',    'Runner. Always looking for new ramen spots.', 'M', '1994-11-03', '["running","food"]'::jsonb, 35.6580, 139.7016, 22, 40, 40, 'Tokyo', 'MS CS', 'Backend engineer', 'Marathon, chess', 'https://picsum.photos/seed/ben/400/600', true),
  (3,  'Chloe',  'Art museums > clubs. Let’s swap playlists.', 'F', '1997-02-20', '["art","indie music"]'::jsonb, 34.6937, 135.5023, 23, 36, 30, 'Osaka', 'BFA', 'Illustrator', 'Sketching, vinyl', 'https://picsum.photos/seed/chloe/400/600', false),
  (4,  'Dan',    'Dog dad. Beach volleyball when the weather cooperates.', 'M', '1993-07-08', '["dogs","volleyball"]'::jsonb, 35.4437, 139.6380, 25, 42, 35, 'Yokohama', 'MBA', 'Consultant', 'Cooking, sailing', 'https://picsum.photos/seed/dan/400/600', false),
  (5,  'Emma',   'Yoga instructor. Morning person.', 'F', '1995-09-15', '["yoga","wellness"]'::jsonb, 35.0116, 135.7681, 24, 39, 50, 'Kyoto', 'RYT-500', 'Yoga teacher', 'Meditation, tea', 'https://picsum.photos/seed/emma/400/600', true),
  (6,  'Finn',   'Indie games and synth music. Soft spot for cats.', 'M', '1998-01-30', '["games","music"]'::jsonb, 43.0642, 141.3469, 21, 35, 45, 'Sapporo', 'BS IT', 'QA engineer', 'Piano, snowboarding', 'https://picsum.photos/seed/finn/400/600', false),
  (7,  'Gia',    'Food blogger. Spicy tolerance: high.', 'F', '1996-06-25', '["food","travel"]'::jsonb, 33.5904, 130.4017, 23, 38, 20, 'Fukuoka', 'Communications', 'Content creator', 'Street food, photography', 'https://picsum.photos/seed/gia/400/600', false),
  (8,  'Hugo',   'Photography and late-night coding sessions.', 'M', '1992-12-05', '["photography","code"]'::jsonb, 35.6762, 139.6503, 26, 45, 15, 'Tokyo', 'BEng', 'DevOps', 'Darkroom, cycling', 'https://picsum.photos/seed/hugo/400/600', false),
  (9,  'Ivy',    'Book club regular. Fantasy + historical fiction.', 'F', '1999-03-18', '["reading","writing"]'::jsonb, 35.6581, 139.7454, 20, 32, 28, 'Tokyo', 'English Lit', 'Editor', 'Poetry, hiking', 'https://picsum.photos/seed/ivy/400/600', false),
  (10, 'Jack',   'Weekend DJ. Love warehouse sunsets.', 'M', '1995-10-10', '["dj","electronic"]'::jsonb, 35.6938, 139.7035, 22, 40, 22, 'Tokyo', 'Self-taught', 'Sound engineer', 'Vinyl digging', 'https://picsum.photos/seed/jack/400/600', false);

-- -----------------------------------------------------------------------------
-- photos (10) — one extra photo per user (primary flag mixed)
-- -----------------------------------------------------------------------------
INSERT INTO photos (id, user_id, s3_key, is_primary, created_at) VALUES
  (1,  1, 'uploads/1/sample-primary.jpg',   true,  now() - interval '20 days'),
  (2,  2, 'uploads/2/sample-primary.jpg',   true,  now() - interval '19 days'),
  (3,  3, 'uploads/3/sample-primary.jpg',   false, now() - interval '18 days'),
  (4,  4, 'uploads/4/sample-primary.jpg',   true,  now() - interval '17 days'),
  (5,  5, 'uploads/5/sample-primary.jpg',   true,  now() - interval '16 days'),
  (6,  6, 'uploads/6/sample-primary.jpg',   true,  now() - interval '15 days'),
  (7,  7, 'uploads/7/sample-primary.jpg',   true,  now() - interval '14 days'),
  (8,  8, 'uploads/8/sample-primary.jpg',   false, now() - interval '13 days'),
  (9,  9, 'uploads/9/sample-primary.jpg',   true,  now() - interval '12 days'),
  (10, 10,'uploads/10/sample-primary.jpg',  true,  now() - interval '11 days');

-- -----------------------------------------------------------------------------
-- likes (10) — unique (from_user, to_user); mix of super_like
-- -----------------------------------------------------------------------------
INSERT INTO likes (id, from_user, to_user, super_like, created_at) VALUES
  (1,  1, 2,  false, now() - interval '10 days'),
  (2,  1, 3,  true,  now() - interval '9 days'),
  (3,  2, 1,  false, now() - interval '8 days'),
  (4,  3, 4,  false, now() - interval '7 days'),
  (5,  4, 5,  true,  now() - interval '6 days'),
  (6,  5, 6,  false, now() - interval '5 days'),
  (7,  6, 7,  false, now() - interval '4 days'),
  (8,  7, 8,  false, now() - interval '3 days'),
  (9,  8, 9,  true,  now() - interval '2 days'),
  (10, 9, 10, false, now() - interval '1 day');

-- -----------------------------------------------------------------------------
-- passes (10) — swiped left; pairs disjoint from critical like tests where needed
-- -----------------------------------------------------------------------------
INSERT INTO passes (id, from_user, to_user, created_at) VALUES
  (1,  1, 8,  now() - interval '15 days'),
  (2,  2, 9,  now() - interval '14 days'),
  (3,  3, 10, now() - interval '13 days'),
  (4,  4, 8,  now() - interval '12 days'),
  (5,  5, 9,  now() - interval '11 days'),
  (6,  6, 10, now() - interval '10 days'),
  (7,  7, 1,  now() - interval '9 days'),
  (8,  8, 2,  now() - interval '8 days'),
  (9,  9, 3,  now() - interval '7 days'),
  (10, 10, 4, now() - interval '6 days');

-- -----------------------------------------------------------------------------
-- matches (10) — user_a / user_b unique; Java uses min/max; we follow a < b
-- -----------------------------------------------------------------------------
INSERT INTO matches (id, user_a, user_b, created_at) VALUES
  (1,  1, 2,  now() - interval '8 days'),
  (2,  3, 4,  now() - interval '7 days'),
  (3,  5, 6,  now() - interval '6 days'),
  (4,  7, 8,  now() - interval '5 days'),
  (5,  9, 10, now() - interval '4 days'),
  (6,  1, 4,  now() - interval '3 days'),
  (7,  2, 5,  now() - interval '2 days'),
  (8,  3, 6,  now() - interval '2 days'),
  (9,  4, 7,  now() - interval '1 day'),
  (10, 5, 8,  now() - interval '1 day');

-- -----------------------------------------------------------------------------
-- messages (10) — sender must be participant in match
-- -----------------------------------------------------------------------------
INSERT INTO messages (id, match_id, sender_id, body, created_at) VALUES
  (1,  1, 1,  'Hey! Loved your climbing pics — favorite gym in Tokyo?', now() - interval '7 days'),
  (2,  1, 2,  'Thanks! I go to B-Pump mostly. You climb too?', now() - interval '7 days' + interval '5 minutes'),
  (3,  2, 3,  'Your art style is amazing. Do you sell prints?', now() - interval '6 days'),
  (4,  2, 4,  'Thank you! Yes, link in bio 🎨', now() - interval '6 days' + interval '10 minutes'),
  (5,  3, 5,  'Morning flow class was tough today 😅', now() - interval '5 days'),
  (6,  3, 6,  'Haha respect. I can barely touch my toes.', now() - interval '5 days' + interval '2 hours'),
  (7,  4, 7,  'Fukuoka food tour recommendations?', now() - interval '4 days'),
  (8,  4, 8,  'Yatai in Nakasu — start there.', now() - interval '4 days' + interval '30 minutes'),
  (9,  5, 9,  'What are you reading this week?', now() - interval '3 days'),
  (10, 5, 10, 'Just finished a Murakami. You?', now() - interval '3 days' + interval '15 minutes');

-- -----------------------------------------------------------------------------
-- Sequences (so next INSERT without id continues after 10)
-- -----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval(pg_get_serial_sequence('photos', 'id'), (SELECT COALESCE(MAX(id), 1) FROM photos));
SELECT setval(pg_get_serial_sequence('likes', 'id'), (SELECT COALESCE(MAX(id), 1) FROM likes));
SELECT setval(pg_get_serial_sequence('passes', 'id'), (SELECT COALESCE(MAX(id), 1) FROM passes));
SELECT setval(pg_get_serial_sequence('matches', 'id'), (SELECT COALESCE(MAX(id), 1) FROM matches));
SELECT setval(pg_get_serial_sequence('messages', 'id'), (SELECT COALESCE(MAX(id), 1) FROM messages));

COMMIT;
