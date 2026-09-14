-- Seeding sports_player_category table with baseline 14 categories matching PDF specification
-- Binds to the 'GENERAL' community (id is looked up dynamically)
DO $$
DECLARE
    gen_comm_id INT;
BEGIN
    SELECT id INTO gen_comm_id FROM community WHERE invite_code = 'GENERAL';
    
    IF gen_comm_id IS NOT NULL THEN
        -- 1. Cricket Brackets
        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Cricket Kids (Under 8)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Cricket Kids (Under 8)', 'KIDS', 'ALL', 4, 7, gen_comm_id, 'DEFAULT', 'Cricket (Combined Boys & Girls)');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Cricket Youth (8 - 18)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Cricket Youth (8 - 18)', 'OPEN', 'ALL', 8, 18, gen_comm_id, 'DEFAULT', 'Cricket (Combined Boys & Girls)');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Cricket Men (Above 18)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Cricket Men (Above 18)', 'MENS', 'MALE', 18, 100, gen_comm_id, 'DEFAULT', 'Cricket (Men Only)');
        END IF;

        -- 2. Badminton Brackets (Strictly Separate)
        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Badminton Boys (< 12)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Badminton Boys (< 12)', 'BOYS', 'MALE', 4, 11, gen_comm_id, 'DEFAULT', 'Badminton');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Badminton Girls (< 12)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Badminton Girls (< 12)', 'GIRLS', 'FEMALE', 4, 11, gen_comm_id, 'DEFAULT', 'Badminton');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Badminton Boys (12 - 18)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Badminton Boys (12 - 18)', 'BOYS', 'MALE', 12, 18, gen_comm_id, 'DEFAULT', 'Badminton');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Badminton Girls (12 - 18)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Badminton Girls (12 - 18)', 'GIRLS', 'FEMALE', 12, 18, gen_comm_id, 'DEFAULT', 'Badminton');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Badminton Men (18+)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Badminton Men (18+)', 'MENS', 'MALE', 18, 100, gen_comm_id, 'DEFAULT', 'Badminton');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Badminton Women (18+)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Badminton Women (18+)', 'WOMENS', 'FEMALE', 18, 100, gen_comm_id, 'DEFAULT', 'Badminton');
        END IF;

        -- 3. Chess, Carroms, TT, Basketball, Skating (< 15)
        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Boys Under 15 (< 15)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Boys Under 15 (< 15)', 'BOYS', 'MALE', 4, 14, gen_comm_id, 'DEFAULT', 'Chess, Carroms, TT, Basketball, Skating');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Girls Under 15 (< 15)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Girls Under 15 (< 15)', 'GIRLS', 'FEMALE', 4, 14, gen_comm_id, 'DEFAULT', 'Chess, Carroms, TT, Basketball, Skating');
        END IF;

        -- 4. Chess, Carroms, TT, Basketball (15+)
        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Men Above 15 (15+)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Men Above 15 (15+)', 'MENS', 'MALE', 15, 100, gen_comm_id, 'DEFAULT', 'Chess, Carroms, TT, Basketball');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Women Above 15 (15+)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Women Above 15 (15+)', 'WOMENS', 'FEMALE', 15, 100, gen_comm_id, 'DEFAULT', 'Chess, Carroms, TT, Basketball');
        END IF;

        -- 5. Volleyball (Adult Men Only)
        IF NOT EXISTS (SELECT 1 FROM sports_player_category WHERE name = 'Volleyball Men (18+)') THEN
            INSERT INTO sports_player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Volleyball Men (18+)', 'MENS', 'MALE', 18, 100, gen_comm_id, 'DEFAULT', 'Volleyball (Adult Men Only)');
        END IF;
    END IF;
END $$;
