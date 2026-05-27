-- =============================================================================
-- 400 distinct test users for production/staging load testing
-- =============================================================================
-- SAFE TO RE-RUN: deletes prior rows matching seed%@youme.test first.
--
-- Login (all seeded users):
--   Email:    seed001@youme.test … seed400@youme.test
--   Password: password
--
-- Bcrypt below matches plaintext "password" (same as sample_data_10_each.sql).
--
-- Run on EC2 (postgres container):
--   docker exec -i <postgres_container> psql -U DatingAppDB -d DatingAppDB -f /tmp/seed_400_test_users.sql
--
-- Discover feed needs: profile row, gender, birthday, lat/lon, photo_url OR photos row.
-- =============================================================================

BEGIN;

DELETE FROM users WHERE email LIKE 'seed%@youme.test';

-- -----------------------------------------------------------------------------
-- users (400)
-- -----------------------------------------------------------------------------
INSERT INTO users (
  email,
  password_hash,
  created_at,
  registration_complete,
  tos_accepted_at,
  privacy_accepted_at,
  locale
)
SELECT
  'seed' || lpad(i::text, 3, '0') || '@youme.test',
  '$2b$10$GKENAxxda6nhA1E3a5BEh.rwj/PmdFtl5BIWFW1Zj5cOqI3/3VibW',
  now() - ((400 - i) || ' hours')::interval,
  true,
  now(),
  now(),
  CASE WHEN i % 3 = 0 THEN 'ja' ELSE 'en' END
FROM generate_series(1, 400) AS s(i);

-- -----------------------------------------------------------------------------
-- profiles (400)
-- -----------------------------------------------------------------------------
INSERT INTO profiles (
  user_id,
  display_name,
  bio,
  gender,
  birthday,
  interests,
  latitude,
  longitude,
  min_age,
  max_age,
  distance_km,
  city,
  education,
  occupation,
  hobbies,
  photo_url,
  is_premium,
  subscription_plan,
  lifestyle
)
SELECT
  u.id,
  format('%s %s', v.first_names[(i % 20) + 1], v.last_names[(i % 15) + 1]),
  v.bios[(i % 10) + 1],
  CASE i % 5
    WHEN 0 THEN 'M'
    WHEN 1 THEN 'F'
    WHEN 2 THEN 'M'
    WHEN 3 THEN 'F'
    ELSE 'NB'
  END,
  (date '1990-01-01' + ((i % 23) || ' years')::interval + ((i % 365) || ' days')::interval)::date,
  to_jsonb(ARRAY[
    v.interests_pool[(i % 18) + 1],
    v.interests_pool[((i + 7) % 18) + 1],
    v.interests_pool[((i + 13) % 18) + 1]
  ]),
  35.65 + (sin(i::double precision / 17) * 0.08),
  139.70 + (cos(i::double precision / 23) * 0.12),
  22 + (i % 5),
  38 + (i % 8),
  15 + (i % 45),
  v.cities[(i % 8) + 1],
  v.educations[(i % 8) + 1],
  v.occupations[(i % 10) + 1],
  v.hobbies_list[(i % 10) + 1],
  format('https://picsum.photos/seed/youme-%s/400/600', i),
  (i % 17 = 0),
  CASE WHEN i % 17 = 0 THEN 'PLUS' ELSE 'FREE' END,
  jsonb_build_object(
    'appearsInModes', jsonb_build_array('for_you', 'serious', 'casual'),
    'lookingFor', CASE i % 4 WHEN 0 THEN 'long_term' WHEN 1 THEN 'short_term' WHEN 2 THEN 'friendship' ELSE 'not_sure' END,
    'drinking', CASE i % 3 WHEN 0 THEN 'never' WHEN 1 THEN 'socially' ELSE 'regularly' END,
    'smoking', CASE i % 3 WHEN 0 THEN 'never' WHEN 1 THEN 'sometimes' ELSE 'no' END,
    'languages', jsonb_build_array(CASE WHEN i % 2 = 0 THEN 'en' ELSE 'ja' END, 'en')
  )
FROM users u
JOIN LATERAL (SELECT (regexp_match(u.email, 'seed(\d+)'))[1]::int AS i) AS n ON true
CROSS JOIN (
  SELECT
    ARRAY['Ava','Ben','Chloe','Dan','Emma','Finn','Gia','Hugo','Ivy','Jack','Kai','Luna','Milo','Nora','Owen','Priya','Quinn','Rina','Sora','Taro'] AS first_names,
    ARRAY['Tanaka','Suzuki','Kim','Park','Lee','Nguyen','Smith','Brown','Garcia','Miller','Wilson','Ito','Sato','Chen','Wong'] AS last_names,
    ARRAY['Coffee and long walks.','Weekend hikes and good food.','Art, music, and quiet nights.','Dog lover, beach volleyball.','Yoga mornings, calm evenings.','Indie games and synth playlists.','Food tours and photography.','Books, poetry, and tea.','Running and ramen hunts.','Museums over clubs.'] AS bios,
    ARRAY['climbing','coffee','travel','running','food','art','music','dogs','yoga','games','photography','reading','dj','hiking','film','cooking','wine','cycling'] AS interests_pool,
    ARRAY['Tokyo','Osaka','Yokohama','Kyoto','Fukuoka','Sapporo','Nagoya','Kobe'] AS cities,
    ARRAY['BA','BS','MS','MBA','BFA','PhD','Self-taught','Associate'] AS educations,
    ARRAY['Designer','Engineer','Teacher','Consultant','Nurse','Chef','Photographer','Writer','Analyst','Founder'] AS occupations,
    ARRAY['Pottery, film','Marathon, chess','Sketching, vinyl','Cooking, sailing','Meditation, tea','Piano, snow','Street food','Poetry, hikes','Cycling, code','Vinyl digging'] AS hobbies_list
) AS v
WHERE u.email LIKE 'seed%@youme.test';

INSERT INTO photos (user_id, s3_key, is_primary, created_at)
SELECT u.id, format('seed/%s/primary.jpg', u.id), true, now()
FROM users u
WHERE u.email LIKE 'seed%@youme.test';

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval(pg_get_serial_sequence('photos', 'id'), (SELECT COALESCE(MAX(id), 1) FROM photos));

COMMIT;

SELECT count(*) AS seed_users FROM users WHERE email LIKE 'seed%@youme.test';
SELECT count(*) AS seed_profiles FROM profiles p
  JOIN users u ON u.id = p.user_id WHERE u.email LIKE 'seed%@youme.test';
